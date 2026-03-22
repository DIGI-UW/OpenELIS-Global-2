package org.openelisglobal.odoo.daoimpl;

import jakarta.persistence.TypedQuery;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.odoo.dao.OdooSyncQueueDAO;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue;
import org.openelisglobal.odoo.valueholder.OdooSyncQueue.SyncStatus;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class OdooSyncQueueDAOImpl extends BaseDAOImpl<OdooSyncQueue, Long> implements OdooSyncQueueDAO {

    public OdooSyncQueueDAOImpl() {
        super(OdooSyncQueue.class);
    }

    @Override
    public List<OdooSyncQueue> getItemsReadyForRetry() {
        String hql = "FROM OdooSyncQueue q WHERE q.status = :pendingStatus "
                + "AND (q.nextRetryTime IS NULL OR q.nextRetryTime <= :now) " + "AND q.retryCount < q.maxRetries "
                + "ORDER BY q.createdAt ASC";
        TypedQuery<OdooSyncQueue> query = entityManager.createQuery(hql, OdooSyncQueue.class);
        query.setParameter("pendingStatus", SyncStatus.PENDING);
        query.setParameter("now", Timestamp.from(Instant.now()));
        return query.getResultList();
    }

    @Override
    public List<OdooSyncQueue> getByStatus(SyncStatus status) {
        String hql = "FROM OdooSyncQueue q WHERE q.status = :status ORDER BY q.createdAt ASC";
        TypedQuery<OdooSyncQueue> query = entityManager.createQuery(hql, OdooSyncQueue.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    @Override
    public OdooSyncQueue getByAccessionNumber(String accessionNumber) {
        String hql = "FROM OdooSyncQueue q WHERE q.accessionNumber = :accessionNumber ORDER BY q.createdAt DESC";
        TypedQuery<OdooSyncQueue> query = entityManager.createQuery(hql, OdooSyncQueue.class);
        query.setParameter("accessionNumber", accessionNumber);
        List<OdooSyncQueue> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public OdooSyncQueue getActiveItemByAccessionNumber(String accessionNumber) {
        String hql = "FROM OdooSyncQueue q WHERE q.accessionNumber = :accessionNumber "
                + "AND q.status IN (:pendingStatus, :inProgressStatus) " + "ORDER BY q.createdAt DESC";
        TypedQuery<OdooSyncQueue> query = entityManager.createQuery(hql, OdooSyncQueue.class);
        query.setParameter("accessionNumber", accessionNumber);
        query.setParameter("pendingStatus", SyncStatus.PENDING);
        query.setParameter("inProgressStatus", SyncStatus.IN_PROGRESS);
        query.setMaxResults(1);
        List<OdooSyncQueue> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
