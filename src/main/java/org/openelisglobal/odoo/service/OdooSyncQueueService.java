package org.openelisglobal.odoo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.odoo.dao.OdooSyncQueueDAO;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing Odoo sync queue operations. Handles queueing,
 * retry logic, and status management for failed Odoo syncs.
 */
@Service
public class OdooSyncQueueService extends BaseObjectServiceImpl<OdooSyncQueue, Long> {

    private static final Logger log = LogManager.getLogger(OdooSyncQueueService.class);

    @Autowired
    private OdooSyncQueueDAO baseObjectDAO;

    @Autowired
    private ObjectMapper objectMapper;

    public OdooSyncQueueService() {
        super(OdooSyncQueue.class);
    }

    @Override
    protected OdooSyncQueueDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    /**
     * Queue a failed invoice sync for later retry
     * 
     * @param updateData   The sample patient update data
     * @param invoiceData  The invoice data that failed to sync
     * @param errorMessage The error message from the failure
     * @return The created queue entry
     */
    @Transactional
    public OdooSyncQueue queueFailedSync(SamplePatientUpdateData updateData, Map<String, Object> invoiceData,
            String errorMessage) {
        try {
            OdooSyncQueue queueEntry = new OdooSyncQueue();
            queueEntry.setAccessionNumber(updateData.getAccessionNumber());

            if (updateData.getSample() != null) {
                queueEntry.setSampleId(updateData.getSample().getId());
            }

            // Serialize invoice data to JSON
            String invoiceDataJson = objectMapper.writeValueAsString(invoiceData);
            queueEntry.setInvoiceData(invoiceDataJson);

            queueEntry.setStatus(SyncStatus.PENDING);
            queueEntry.setErrorMessage(errorMessage);
            queueEntry.setCreatedDate(new Timestamp(System.currentTimeMillis()));

            Long id = insert(queueEntry);
            log.info("Queued failed Odoo sync for accession number: {} with queue ID: {}",
                    updateData.getAccessionNumber(), id);

            return get(id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing invoice data for accession number: {}", updateData.getAccessionNumber(), e);
            throw new RuntimeException("Failed to serialize invoice data", e);
        }
    }

    /**
     * Get all pending sync requests that need to be retried
     * 
     * @return List of pending sync queue entries
     */
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getPendingSyncRequests() {
        return baseObjectDAO.getPendingSyncRequests();
    }

    /**
     * Get sync requests by status
     * 
     * @param status The sync status to filter by
     * @return List of sync queue entries with the specified status
     */
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByStatus(SyncStatus status) {
        return baseObjectDAO.getByStatus(status);
    }

    /**
     * Get sync requests by accession number
     * 
     * @param accessionNumber The accession number to search for
     * @return List of sync queue entries for the accession number
     */
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByAccessionNumber(String accessionNumber) {
        return baseObjectDAO.getByAccessionNumber(accessionNumber);
    }

    /**
     * Get all failed sync requests (after max retries)
     * 
     * @return List of permanently failed sync queue entries
     */
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getFailedSyncRequests() {
        return baseObjectDAO.getFailedSyncRequests();
    }

    /**
     * Mark a sync request as processing
     * 
     * @param queueEntry The queue entry to mark
     */
    @Transactional
    public void markAsProcessing(OdooSyncQueue queueEntry) {
        queueEntry.setStatus(SyncStatus.PROCESSING);
        queueEntry.setLastRetryDate(new Timestamp(System.currentTimeMillis()));
        update(queueEntry);
        log.debug("Marked queue entry {} as processing", queueEntry.getId());
    }

    /**
     * Mark a sync request as completed
     * 
     * @param queueEntry The queue entry to mark
     * @param invoiceId  The Odoo invoice ID
     */
    @Transactional
    public void markAsCompleted(OdooSyncQueue queueEntry, Integer invoiceId) {
        queueEntry.markCompleted(invoiceId);
        update(queueEntry);
        log.info("Successfully synced queue entry {} for accession number: {} with Odoo invoice ID: {}",
                queueEntry.getId(), queueEntry.getAccessionNumber(), invoiceId);
    }

    /**
     * Mark a sync request as failed after a retry attempt
     * 
     * @param queueEntry   The queue entry to mark
     * @param errorMessage The error message from the retry attempt
     */
    @Transactional
    public void markRetryFailed(OdooSyncQueue queueEntry, String errorMessage) {
        queueEntry.incrementRetryCount();
        queueEntry.setErrorMessage(errorMessage);

        if (queueEntry.hasExceededMaxRetries()) {
            queueEntry.markFailed(errorMessage);
            log.error("Queue entry {} for accession number {} has exceeded max retries. Marking as FAILED.",
                    queueEntry.getId(), queueEntry.getAccessionNumber());
        } else {
            queueEntry.setStatus(SyncStatus.PENDING);
            log.warn("Retry attempt {} failed for queue entry {} (accession: {}). Will retry again.",
                    queueEntry.getRetryCount(), queueEntry.getId(), queueEntry.getAccessionNumber());
        }

        update(queueEntry);
    }

    /**
     * Get the invoice data from a queue entry as a Map
     * 
     * @param queueEntry The queue entry
     * @return The invoice data as a Map
     * @throws JsonProcessingException if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getInvoiceDataAsMap(OdooSyncQueue queueEntry) throws JsonProcessingException {
        return objectMapper.readValue(queueEntry.getInvoiceData(), Map.class);
    }

    /**
     * Reset a failed queue entry to pending (for manual retry)
     * 
     * @param queueEntry The queue entry to reset
     */
    @Transactional
    public void resetToPending(OdooSyncQueue queueEntry) {
        queueEntry.setStatus(SyncStatus.PENDING);
        queueEntry.setRetryCount(0);
        queueEntry.setErrorMessage(null);
        update(queueEntry);
        log.info("Reset queue entry {} to PENDING for manual retry", queueEntry.getId());
    }

    /**
     * Get count of pending sync requests
     * 
     * @return The count of pending requests
     */
    @Transactional(readOnly = true)
    public int getPendingCount() {
        return getPendingSyncRequests().size();
    }

    /**
     * Get count of failed sync requests
     * 
     * @return The count of failed requests
     */
    @Transactional(readOnly = true)
    public int getFailedCount() {
        return getFailedSyncRequests().size();
    }
}
