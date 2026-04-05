package org.openelisglobal.audittrail.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
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
            query.setParameter("refId", Integer.parseInt(refId));
            query.setParameter("tableId", Integer.parseInt(tableId));
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
            List<String> referenceTableIds, String activity, String search, int page, int pageSize)
            throws LIMSRuntimeException {
        try {
            StringBuilder hql = new StringBuilder("from History h where 1=1");
            appendFilters(hql, startDate, endDate, sysUserId, referenceTableIds, activity, search);
            hql.append(" order by h.timestamp desc");

            Query<History> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), History.class);
            setFilterParameters(query, startDate, endDate, sysUserId, referenceTableIds, activity, search);
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
            List<String> referenceTableIds, String activity, String search) throws LIMSRuntimeException {
        try {
            StringBuilder hql = new StringBuilder("select count(*) from History h where 1=1");
            appendFilters(hql, startDate, endDate, sysUserId, referenceTableIds, activity, search);

            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), Long.class);
            setFilterParameters(query, startDate, endDate, sysUserId, referenceTableIds, activity, search);
            return query.uniqueResult();
        } catch (HibernateException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in HistoryDAOImpl getSystemEventHistoryCount()", e);
        }
    }

    private void appendFilters(StringBuilder hql, Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search) {
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
    }

    private void setFilterParameters(Query<?> query, Timestamp startDate, Timestamp endDate, String sysUserId,
            List<String> referenceTableIds, String activity, String search) {
        if (startDate != null) {
            query.setParameter("startDate", startDate);
        }
        if (endDate != null) {
            query.setParameter("endDate", endDate);
        }
        if (sysUserId != null && !sysUserId.isEmpty()) {
            try {
                query.setParameter("sysUserId", Integer.parseInt(sysUserId));
            } catch (NumberFormatException e) {
                LogEvent.logWarn("HistoryDAOImpl", "bindParameters", "Invalid sysUserId (non-numeric): " + sysUserId);
                query.setParameter("sysUserId", -1);
            }
        }
        if (referenceTableIds != null && !referenceTableIds.isEmpty()) {
            try {
                query.setParameterList("referenceTableIds",
                        referenceTableIds.stream().map(Integer::parseInt).collect(Collectors.toList()));
            } catch (NumberFormatException e) {
                LogEvent.logWarn("HistoryDAOImpl", "bindParameters", "Invalid referenceTableId (non-numeric) in list");
                query.setParameterList("referenceTableIds", List.of(-1));
            }
        }
        if (activity != null && !activity.isEmpty()) {
            query.setParameter("activity", activity);
        }
        if (search != null && !search.isEmpty()) {
            query.setParameter("search", "%" + search + "%");
        }
    }
}
