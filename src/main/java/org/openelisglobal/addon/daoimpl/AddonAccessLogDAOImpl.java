package org.openelisglobal.addon.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.dao.AddonAccessLogDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Native SQL like the outbox; an insert runs in its own transaction so that the
 * record of a read survives whatever happens to the request's transaction.
 */
@Component
@Transactional
public class AddonAccessLogDAOImpl implements AddonAccessLogDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Entry e) {
        try {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("addon_id", e.addonId());
            values.put("credential", e.credential());
            values.put("user_id", e.userId());
            values.put("actor", e.actor());
            values.put("purpose", e.purpose());
            values.put("method", e.method());
            values.put("endpoint", e.endpoint());
            values.put("subject_type", e.subjectType());
            values.put("subject", e.subject());
            values.put("status", e.status());
            values.put("row_count", e.rows());
            // a null parameter is sent as bytea, so absent values are written as NULL
            List<String> columns = new ArrayList<>();
            List<String> params = new ArrayList<>();
            values.forEach((k, v) -> {
                columns.add(k);
                params.add(v == null ? "NULL" : ":" + k);
            });
            Query q = entityManager.createNativeQuery("INSERT INTO clinlims.addon_access_log (at, "
                    + String.join(", ", columns) + ") VALUES (:at, " + String.join(", ", params) + ")");
            q.setParameter("at", Timestamp.from(e.at()));
            values.forEach((k, v) -> {
                if (v != null) {
                    q.setParameter(k, v);
                }
            });
            q.executeUpdate();
        } catch (RuntimeException ex) {
            LogEvent.logError(ex);
            throw new LIMSRuntimeException("Error in AddonAccessLog insert()", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Entry> find(String addonId, Instant from, Instant to, String actor, String subject, int limit) {
        StringBuilder sql = new StringBuilder("SELECT at, addon_id, credential, user_id, actor, purpose, method,"
                + " endpoint, subject_type, subject, status, row_count FROM clinlims.addon_access_log WHERE 1 = 1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (addonId != null) {
            sql.append(" AND addon_id = :addonId");
            params.put("addonId", addonId);
        }
        if (from != null) {
            sql.append(" AND at >= :from");
            params.put("from", Timestamp.from(from));
        }
        if (to != null) {
            sql.append(" AND at < :to");
            params.put("to", Timestamp.from(to));
        }
        if (actor != null) {
            sql.append(" AND actor = :actor");
            params.put("actor", actor);
        }
        if (subject != null) {
            sql.append(" AND subject = :subject");
            params.put("subject", subject);
        }
        sql.append(" ORDER BY at DESC, id DESC LIMIT :limit");
        params.put("limit", limit);
        try {
            Query q = entityManager.createNativeQuery(sql.toString());
            params.forEach(q::setParameter);
            List<Entry> out = new ArrayList<>();
            for (Object[] r : (List<Object[]>) q.getResultList()) {
                out.add(new Entry(instant(r[0]), (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                        (String) r[5], (String) r[6], (String) r[7], (String) r[8], (String) r[9],
                        ((Number) r[10]).intValue(), r[11] == null ? null : ((Number) r[11]).intValue()));
            }
            return out;
        } catch (RuntimeException ex) {
            LogEvent.logError(ex);
            throw new LIMSRuntimeException("Error in AddonAccessLog find()", ex);
        }
    }

    @Override
    public int purgeOlderThan(int days) {
        try {
            return entityManager
                    .createNativeQuery("DELETE FROM clinlims.addon_access_log"
                            + " WHERE at < now() - make_interval(days => :days)")
                    .setParameter("days", days).executeUpdate();
        } catch (RuntimeException ex) {
            LogEvent.logError(ex);
            throw new LIMSRuntimeException("Error in AddonAccessLog purgeOlderThan()", ex);
        }
    }

    private static Instant instant(Object o) {
        if (o instanceof Timestamp t) {
            return t.toInstant();
        }
        if (o instanceof OffsetDateTime t) {
            return t.toInstant();
        }
        if (o instanceof Instant i) {
            return i;
        }
        return null;
    }
}
