package org.openelisglobal.notification.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationLogDAOImpl extends BaseDAOImpl<NotificationLog, Long> implements NotificationLogDAO {

    public NotificationLogDAOImpl() {
        super(NotificationLog.class);
    }

    @Override
    public List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size) {
        try {
            StringBuilder hql = new StringBuilder("from NotificationLog as nl");
            List<String> where = buildWhereClauses(eventCode, status);
            if (!where.isEmpty()) {
                hql.append(" where ").append(String.join(" and ", where));
            }
            hql.append(" order by nl.firedAt desc, nl.id desc");

            Query<NotificationLog> query = entityManager.unwrap(Session.class).createQuery(hql.toString(),
                    NotificationLog.class);
            bindFilters(query, eventCode, status);
            query.setFirstResult(Math.max(0, (page - 1) * size));
            query.setMaxResults(size);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NotificationLogDAOImpl.findPage", e);
        }
    }

    @Override
    public long countMatching(Optional<String> eventCode, Optional<String> status) {
        try {
            StringBuilder hql = new StringBuilder("select count(nl.id) from NotificationLog as nl");
            List<String> where = buildWhereClauses(eventCode, status);
            if (!where.isEmpty()) {
                hql.append(" where ").append(String.join(" and ", where));
            }
            Query<Long> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), Long.class);
            bindFilters(query, eventCode, status);
            Long result = query.getSingleResult();
            return result == null ? 0L : result;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in NotificationLogDAOImpl.countMatching", e);
        }
    }

    private List<String> buildWhereClauses(Optional<String> eventCode, Optional<String> status) {
        List<String> where = new ArrayList<>();
        if (eventCode.isPresent() && !eventCode.get().isBlank()) {
            where.add("nl.eventCode = :eventCode");
        }
        if (status.isPresent() && !status.get().isBlank()) {
            where.add("nl.overallStatus = :overallStatus");
        }
        return where;
    }

    private void bindFilters(Query<?> query, Optional<String> eventCode, Optional<String> status) {
        if (eventCode.isPresent() && !eventCode.get().isBlank()) {
            query.setParameter("eventCode", eventCode.get());
        }
        if (status.isPresent() && !status.get().isBlank()) {
            query.setParameter("overallStatus", OverallStatus.valueOf(status.get()));
        }
    }
}
