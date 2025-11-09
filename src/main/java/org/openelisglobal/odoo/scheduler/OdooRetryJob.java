package org.openelisglobal.odoo.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.service.OdooIntegrationService;
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

    @Autowired
    private OdooIntegrationService odooIntegrationService;

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
        runRetry(false);
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

            Integer partnerId = extractPartnerId(invoiceData);
            if (!odooIntegrationService.partnerExists(partnerId)) {
                log.warn(
                        "Partner {} referenced by queue entry {} not found in Odoo. Attempting to refresh partner reference.",
                        partnerId, queueEntry.getId());
                Integer refreshedPartnerId = odooIntegrationService.ensurePartnerForQueueEntry(queueEntry);
                if (refreshedPartnerId == null) {
                    throw new RuntimeException("Unable to refresh partner for queue entry " + queueEntry.getId()
                            + ". Partner data missing.");
                }
                invoiceData.put("partner_id", refreshedPartnerId);
                odooSyncQueueService.updateInvoiceData(queueEntry, invoiceData);
                partnerId = refreshedPartnerId;
            }

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

    public String triggerManualRetry() {
        log.info("Manual Odoo retry job triggered");
        RetryRunStatus status = runRetry(true);

        if (!status.executed) {
            if (status.message != null) {
                return status.message;
            }
            return String.format("Odoo retry job skipped. Pending: %d, Failed: %d", status.pending, status.failed);
        }

        return String.format("Odoo retry job completed. Pending: %d, Failed: %d", status.pending, status.failed);
    }

    private RetryRunStatus runRetry(boolean forceRun) {
        RetryRunStatus status = new RetryRunStatus();

        if (!forceRun && !retryEnabled) {
            log.debug("Odoo retry job is disabled");
            status.message = "Odoo retry job is disabled by configuration";
            status.pending = odooSyncQueueService.getPendingCount();
            status.failed = odooSyncQueueService.getFailedCount();
            return status;
        }

        if (!odooConnection.isAvailable()) {
            log.warn("Odoo connection is not available. Skipping retry job execution.");
            status.message = "Odoo connection is not available. Skipping retry job execution.";
            status.pending = odooSyncQueueService.getPendingCount();
            status.failed = odooSyncQueueService.getFailedCount();
            return status;
        }

        try {
            List<OdooSyncQueue> pendingEntries = odooSyncQueueService.getPendingSyncRequests();

            if (pendingEntries.isEmpty()) {
                log.debug("No pending Odoo sync requests to process");
                status.message = "No pending Odoo sync requests to process";
                status.pending = 0;
                status.failed = odooSyncQueueService.getFailedCount();
                return status;
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

            status.executed = true;
            status.pending = remainingPending;
            status.failed = permanentlyFailed;

            if (remainingPending > 0 || permanentlyFailed > 0) {
                log.warn("Odoo sync queue status - Pending: {}, Permanently Failed: {}", remainingPending,
                        permanentlyFailed);
            }

        } catch (Exception e) {
            log.error("Error in Odoo retry job: {}", e.getMessage(), e);
            status.message = "Error occurred during Odoo retry job: " + e.getMessage();
            status.pending = odooSyncQueueService.getPendingCount();
            status.failed = odooSyncQueueService.getFailedCount();
        }

        return status;
    }

    private static class RetryRunStatus {
        private boolean executed;
        private int pending;
        private int failed;
        private String message;
    }

    private Integer extractPartnerId(Map<String, Object> invoiceData) {
        if (invoiceData == null) {
            return null;
        }
        Object partnerValue = invoiceData.get("partner_id");
        if (partnerValue instanceof Integer) {
            return (Integer) partnerValue;
        }
        if (partnerValue instanceof Number) {
            return ((Number) partnerValue).intValue();
        }
        if (partnerValue instanceof String) {
            try {
                return Integer.parseInt(((String) partnerValue).trim());
            } catch (NumberFormatException e) {
                log.warn("Unable to parse partner_id '{}' from invoice data as integer", partnerValue);
            }
        }
        if (partnerValue instanceof List) {
            List<?> values = (List<?>) partnerValue;
            if (!values.isEmpty() && values.get(0) instanceof Number) {
                return ((Number) values.get(0)).intValue();
            }
        }
        return null;
    }
}
