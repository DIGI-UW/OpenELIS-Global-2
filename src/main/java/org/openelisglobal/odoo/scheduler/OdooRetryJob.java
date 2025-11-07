package org.openelisglobal.odoo.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.service.OdooSyncQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to retry failed Odoo synchronization attempts. Runs
 * periodically to process pending queue entries and attempt to sync them with
 * Odoo.
 */
@Component
public class OdooRetryJob {

    private static final Logger log = LogManager.getLogger(OdooRetryJob.class);

    @Autowired
    private OdooSyncQueueService odooSyncQueueService;

    @Autowired
    private OdooConnection odooConnection;

    @Value("${org.openelisglobal.odoo.retry.enabled:false}")
    private boolean retryEnabled;

    @Value("${org.openelisglobal.odoo.retry.batchSize:10}")
    private int batchSize;

    /**
     * Scheduled method to process pending Odoo sync queue entries. Runs every 5
     * minutes by default (300000 milliseconds). Can be configured via property:
     * org.openelisglobal.odoo.retry.interval
     */
    @Scheduled(initialDelay = 60000, // Start 1 minute after application startup
            fixedDelayString = "${org.openelisglobal.odoo.retry.interval:300000}" // Default: 5 minutes
    )
    public void retryFailedSyncs() {
        if (!retryEnabled) {
            log.debug("Odoo retry job is disabled");
            return;
        }

        if (!odooConnection.isAvailable()) {
            log.warn("Odoo connection is not available. Skipping retry job execution.");
            return;
        }

        try {
            List<OdooSyncQueue> pendingEntries = odooSyncQueueService.getPendingSyncRequests();

            if (pendingEntries.isEmpty()) {
                log.debug("No pending Odoo sync requests to process");
                return;
            }

            log.info("Found {} pending Odoo sync requests. Processing up to {} entries...", pendingEntries.size(),
                    batchSize);

            int processed = 0;
            int succeeded = 0;
            int failed = 0;

            for (OdooSyncQueue queueEntry : pendingEntries) {
                if (processed >= batchSize) {
                    log.info("Reached batch size limit of {}. Remaining entries will be processed in next run.",
                            batchSize);
                    break;
                }

                processed++;
                boolean success = processQueueEntry(queueEntry);
                if (success) {
                    succeeded++;
                } else {
                    failed++;
                }
            }

            log.info("Odoo retry job completed. Processed: {}, Succeeded: {}, Failed: {}", processed, succeeded,
                    failed);

            int remainingPending = odooSyncQueueService.getPendingCount();
            int permanentlyFailed = odooSyncQueueService.getFailedCount();

            if (remainingPending > 0 || permanentlyFailed > 0) {
                log.warn("Odoo sync queue status - Pending: {}, Permanently Failed: {}", remainingPending,
                        permanentlyFailed);
            }

        } catch (Exception e) {
            log.error("Error in Odoo retry job: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a single queue entry
     * 
     * @param queueEntry The queue entry to process
     * @return true if successful, false otherwise
     */
    private boolean processQueueEntry(OdooSyncQueue queueEntry) {
        log.info("Processing queue entry {} for accession number: {} (attempt {}/{})", queueEntry.getId(),
                queueEntry.getAccessionNumber(), queueEntry.getRetryCount() + 1, queueEntry.getMaxRetries());

        try {
            odooSyncQueueService.markAsProcessing(queueEntry);

            Map<String, Object> invoiceData = odooSyncQueueService.getInvoiceDataAsMap(queueEntry);

            Integer invoiceId = odooConnection.create("account.move", List.of(invoiceData));

            if (invoiceId == null) {
                throw new RuntimeException("Odoo returned null invoice ID");
            }

            odooSyncQueueService.markAsCompleted(queueEntry, invoiceId);
            log.info("Successfully synced queue entry {} with Odoo invoice ID: {}", queueEntry.getId(), invoiceId);
            return true;

        } catch (JsonProcessingException e) {
            log.error("Error deserializing invoice data for queue entry {}: {}", queueEntry.getId(), e.getMessage(), e);
            odooSyncQueueService.markRetryFailed(queueEntry, "Failed to deserialize invoice data: " + e.getMessage());
            return false;

        } catch (Exception e) {
            log.error("Error syncing queue entry {} to Odoo: {}", queueEntry.getId(), e.getMessage(), e);
            odooSyncQueueService.markRetryFailed(queueEntry, "Odoo sync failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Manual trigger for retry job (can be called from admin interface)
     * 
     * @return Summary message
     */
    public String triggerManualRetry() {
        log.info("Manual Odoo retry job triggered");
        retryFailedSyncs();

        int pending = odooSyncQueueService.getPendingCount();
        int failed = odooSyncQueueService.getFailedCount();

        return String.format("Odoo retry job completed. Pending: %d, Failed: %d", pending, failed);
    }
}
