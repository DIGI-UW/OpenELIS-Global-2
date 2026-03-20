package org.openelisglobal.odoo.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;

public interface OdooSyncQueueDAO extends BaseDAO<OdooSyncQueue, Long> {

    List<OdooSyncQueue> getItemsReadyForRetry();

    List<OdooSyncQueue> getByStatus(SyncStatus status);

    OdooSyncQueue getByAccessionNumber(String accessionNumber);
}
