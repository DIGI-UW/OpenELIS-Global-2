package org.openelisglobal.odoo.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;

public interface OdooSyncQueueDAO extends BaseDAO<OdooSyncQueue, Long> {
    List<OdooSyncQueue> getItemsReadyForRetry();

    List<OdooSyncQueue> getByStatus(SyncStatus status);

    OdooSyncQueue getByAccessionNumber(String accessionNumber);

    /**
     * Returns the most recent PENDING or IN_PROGRESS item for the given accession
     * number, or null if no active item exists. Used by enqueue() to enforce
     * idempotency and prevent duplicate invoice creation.
     */
    OdooSyncQueue getActiveItemByAccessionNumber(String accessionNumber);
}
