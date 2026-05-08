package org.openelisglobal.audittrail.daoimpl;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.audittrail.dao.HistoryDAO;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class HistoryDAOImpl extends BaseDAOImpl<History, String> implements HistoryDAO {
    HistoryDAOImpl() {
        super(History.class);
    }

    @Override
    public List<History> getHistoryByRefIdAndRefTableId(String refId, String tableId) throws LIMSRuntimeException {
        History history = new History();
        history.setReferenceId(refId);
        history.setReferenceTable(tableId);
        return getHistoryByRefIdAndRefTableId(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<History> getHistoryByRefIdAndRefTableId(History history) throws LIMSRuntimeException {
        String refId = history.getReferenceId();
        String tableId = history.getReferenceTable();
        List<History> list;

        try {
            String sql = "from History h where h.referenceId = :refId and h.referenceTable = :tableId order by"
                    + " h.timestamp desc, h.activity desc";
            Query<History> query = entityManager.unwrap(Session.class).createQuery(sql, History.class);
            query.setParameter("refId", refId);
            query.setParameter("tableId", tableId);
            list = query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AuditTrail getHistoryByRefIdAndRefTableId()", e);
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<History> getSystemEventHistory(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId, int page, int pageSize)
            throws LIMSRuntimeException {
        // sysUserId binds through LIMSStringNumberUserType, which calls
        // Integer.parseInt during binding; short-circuit to an empty result for
        // non-numeric input so bad client filters don't surface as 500s.
        if (sysUserId != null && !sysUserId.isEmpty() && !GenericValidator.isInt(sysUserId)) {
            return Collections.emptyList();
        }
        try {
            StringBuilder hql = new StringBuilder("from History h where 1=1");
            appendFilters(hql, startDate, endDate, sysUserId, referenceTableIds, activity, search, referenceId);
            hql.append(" order by h.timestamp desc");

            Query<History> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), History.class);
            setFilterParameters(query, startDate, endDate, sysUserId, referenceTableIds, activity, search, referenceId);
            query.setFirstResult((page - 1) * pageSize);
            query.setMaxResults(pageSize);
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in HistoryDAOImpl getSystemEventHistory()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getSystemEventHistoryCount(Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId)
            throws LIMSRuntimeException {
        if (sysUserId != null && !sysUserId.isEmpty() && !GenericValidator.isInt(sysUserId)) {
            return 0L;
        }
        try {
            StringBuilder hql = new StringBuilder("select count(*) from History h where 1=1");
            appendFilters(hql, startDate, endDate, sysUserId, referenceTableIds, activity, search, referenceId);

            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), Long.class);
            setFilterParameters(query, startDate, endDate, sysUserId, referenceTableIds, activity, search, referenceId);
            return query.uniqueResult();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in HistoryDAOImpl getSystemEventHistoryCount()", e);
        }
    }

    private void appendFilters(StringBuilder hql, Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId) {
        if (startDate != null) {
            hql.append(" and h.timestamp >= :startDate");
        }
        if (endDate != null) {
            hql.append(" and h.timestamp <= :endDate");
        }
        if (sysUserId != null && !sysUserId.isEmpty()) {
            hql.append(" and h.sysUserId = :sysUserId");
        }
        if (referenceTableIds != null && !referenceTableIds.isEmpty()) {
            hql.append(" and h.referenceTable in (:referenceTableIds)");
        }
        if (activity != null && !activity.isEmpty()) {
            hql.append(" and h.activity = :activity");
        }
        if (search != null && !search.isEmpty()) {
            hql.append(" and cast(h.referenceId as string) like :search");
        }
        if (referenceId != null && !referenceId.isEmpty()) {
            hql.append(" and h.referenceId = :referenceId");
        }
    }

    private void setFilterParameters(Query<?> query, Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search, String referenceId) {
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        if (sysUserId != null && !sysUserId.isEmpty()) {
            query.setParameter("sysUserId", sysUserId);
        }
        if (referenceTableIds != null && !referenceTableIds.isEmpty()) {
            query.setParameterList("referenceTableIds", referenceTableIds);
        }
        if (activity != null && !activity.isEmpty()) {
            query.setParameter("activity", activity);
        }
        if (search != null && !search.isEmpty()) {
            query.setParameter("search", "%" + search + "%");
        }
        if (referenceId != null && !referenceId.isEmpty()) {
            query.setParameter("referenceId", referenceId);
        }
    }
}
