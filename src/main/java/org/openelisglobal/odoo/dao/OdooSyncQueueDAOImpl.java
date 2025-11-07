package org.openelisglobal.odoo.dao;

import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.entity.OdooSyncQueue;
import org.openelisglobal.odoo.entity.OdooSyncQueue.SyncStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OdooSyncQueueDAOImpl extends BaseDAOImpl<OdooSyncQueue, Long> implements OdooSyncQueueDAO {

    public OdooSyncQueueDAOImpl() {
        super(OdooSyncQueue.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getPendingSyncRequests() {
        try {
            String hql = "FROM OdooSyncQueue q WHERE q.status = :status ORDER BY q.createdDate ASC";
            Query<OdooSyncQueue> query = entityManager.unwrap(Session.class).createQuery(hql, OdooSyncQueue.class);
            query.setParameter("status", SyncStatus.PENDING.name());
            List<OdooSyncQueue> results = query.list();
            return results.stream().filter(this::withinRetryLimit).collect(Collectors.toList());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OdooSyncQueueDAO getPendingSyncRequests()", e);
        }
    }

    private boolean withinRetryLimit(OdooSyncQueue queue) {
        int retryCount = queue.getRetryCount() == null ? 0 : queue.getRetryCount();
        int maxRetries = queue.getMaxRetries() == null ? 10 : queue.getMaxRetries();
        return retryCount < maxRetries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByStatus(SyncStatus status) {
        try {
            String hql = "FROM OdooSyncQueue q WHERE q.status = :status ORDER BY q.createdDate DESC";
            Query<OdooSyncQueue> query = entityManager.unwrap(Session.class).createQuery(hql, OdooSyncQueue.class);
            query.setParameter("status", status);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OdooSyncQueueDAO getByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getByAccessionNumber(String accessionNumber) {
        try {
            String hql = "FROM OdooSyncQueue q WHERE q.accessionNumber = :accessionNumber "
                    + "ORDER BY q.createdDate DESC";
            Query<OdooSyncQueue> query = entityManager.unwrap(Session.class).createQuery(hql, OdooSyncQueue.class);
            query.setParameter("accessionNumber", accessionNumber);
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OdooSyncQueueDAO getByAccessionNumber()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OdooSyncQueue> getFailedSyncRequests() {
        try {
            String hql = "FROM OdooSyncQueue q WHERE q.status = :status ORDER BY q.createdDate DESC";
            Query<OdooSyncQueue> query = entityManager.unwrap(Session.class).createQuery(hql, OdooSyncQueue.class);
            query.setParameter("status", SyncStatus.FAILED.name());
            return query.list();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in OdooSyncQueueDAO getFailedSyncRequests()", e);
        }
    }
}
