package org.openelisglobal.odoo.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;

public interface OdooSyncQueueDAO extends BaseDAO<OdooSyncQueue, Long> {

    /**
     * Get all pending sync requests that need to be retried
     * 
     * @param processingTimeoutThreshold Entries stuck in PROCESSING with a last
     *                                   retry before this threshold are included as
     *                                   stale and should be retried.
     * @return List of pending sync queue entries
     */
    List<OdooSyncQueue> getPendingSyncRequests(java.sql.Timestamp processingTimeoutThreshold);

    /**
     * Get sync requests by status
     * 
     * @param status The sync status to filter by
     * @return List of sync queue entries with the specified status
     */
    List<OdooSyncQueue> getByStatus(SyncStatus status);

    /**
     * Get sync request by accession number
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
}
