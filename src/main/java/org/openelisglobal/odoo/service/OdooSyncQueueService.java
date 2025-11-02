package org.openelisglobal.odoo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;

/**
 * Service interface for managing Odoo sync queue operations. Handles queueing,
 * retry logic, and status management for failed Odoo syncs.
 */
public interface OdooSyncQueueService extends BaseObjectService<OdooSyncQueue, Long> {

    /**
     * Queue a failed invoice sync for later retry
     * 
     * @param updateData   The sample patient update data
     * @param invoiceData  The invoice data that failed to sync
     * @param errorMessage The error message from the failure
     * @return The created queue entry
     */
    OdooSyncQueue queueFailedSync(SamplePatientUpdateData updateData, Map<String, Object> invoiceData,
            String errorMessage);

    /**
     * Get all pending sync requests that need to be retried
     * 
     * @return List of pending sync queue entries
     */
    List<OdooSyncQueue> getPendingSyncRequests();

    /**
     * Get sync requests by status
     * 
     * @param status The sync status to filter by
     * @return List of sync queue entries with the specified status
     */
    List<OdooSyncQueue> getByStatus(SyncStatus status);

    /**
     * Get sync requests by accession number
     * 
     * @param accessionNumber The accession number to search for
     * @return List of sync queue entries for the accession number
     */
    List<OdooSyncQueue> getByAccessionNumber(String accessionNumber);

    /**
     * Get all failed sync requests (after max retries)
     * 
     * @return List of permanently failed sync queue entries
     */
    List<OdooSyncQueue> getFailedSyncRequests();

    /**
     * Mark a sync request as processing
     * 
     * @param queueEntry The queue entry to mark
     */
    void markAsProcessing(OdooSyncQueue queueEntry);

    /**
     * Mark a sync request as completed
     * 
     * @param queueEntry The queue entry to mark
     * @param invoiceId  The Odoo invoice ID
     */
    void markAsCompleted(OdooSyncQueue queueEntry, Integer invoiceId);

    /**
     * Mark a sync request as failed after a retry attempt
     * 
     * @param queueEntry   The queue entry to mark
     * @param errorMessage The error message from the retry attempt
     */
    void markRetryFailed(OdooSyncQueue queueEntry, String errorMessage);

    /**
     * Get the invoice data from a queue entry as a Map
     * 
     * @param queueEntry The queue entry
     * @return The invoice data as a Map
     * @throws JsonProcessingException if deserialization fails
     */
    Map<String, Object> getInvoiceDataAsMap(OdooSyncQueue queueEntry) throws JsonProcessingException;

    /**
     * Reset a failed queue entry to pending (for manual retry)
     * 
     * @param queueEntry The queue entry to reset
     */
    void resetToPending(OdooSyncQueue queueEntry);

    /**
     * Get count of pending sync requests
     * 
     * @return The count of pending requests
     */
    int getPendingCount();

    /**
     * Get count of failed sync requests
     * 
     * @return The count of failed requests
     */
    int getFailedCount();
}
