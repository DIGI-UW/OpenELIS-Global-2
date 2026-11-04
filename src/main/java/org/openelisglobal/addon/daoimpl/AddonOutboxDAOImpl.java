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
import org.openelisglobal.addon.dao.AddonOutboxDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Native SQL, like the Result Delivery outbox (DeliveryQueueDAOImpl): the
 * outbox rows are written by database triggers, and claiming needs
 * {@code FOR UPDATE SKIP LOCKED}, which HQL cannot express.
 */
@Component
@Transactional
public class AddonOutboxDAOImpl implements AddonOutboxDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int fanOut(int limit) {
        try {
            // One statement, so the events marked fanned are exactly the events
            // fanned out: an event committed meanwhile waits for the next tick.
            String sql = """
                    WITH ev AS (
                        SELECT id, type FROM clinlims.addon_event
                        WHERE NOT fanned ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED),
                    ins AS (
                        INSERT INTO clinlims.addon_delivery (event_id, addon_id)
                        SELECT ev.id, a.id FROM ev JOIN clinlims.addon a
                          ON a.enabled AND (',' || a.events || ',') LIKE ('%,' || ev.type || ',%')
                        ON CONFLICT DO NOTHING
                        RETURNING 1),
                    upd AS (
                        UPDATE clinlims.addon_event SET fanned = TRUE WHERE id IN (SELECT id FROM ev))
                    SELECT count(*) FROM ins
                    """;
            return ((Number) entityManager.createNativeQuery(sql).setParameter("limit", limit).getSingleResult())
                    .intValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox fanOut()", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Claim> claimDue(String worker, int limit, int leaseSeconds) {
        try {
            String sql = """
                    UPDATE clinlims.addon_delivery d
                    SET locked_by = :worker, locked_until = now() + make_interval(secs => :lease)
                    FROM clinlims.addon_event e
                    WHERE e.id = d.event_id AND (d.event_id, d.addon_id) IN (
                        SELECT q.event_id, q.addon_id FROM clinlims.addon_delivery q
                        WHERE q.status = 'PENDING' AND q.next_attempt_at <= now()
                          AND (q.locked_until IS NULL OR q.locked_until < now())
                        ORDER BY q.next_attempt_at, q.event_id
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED)
                    RETURNING d.event_id, d.addon_id, d.attempts, e.type, e.data, e.created_at
                    """;
            List<Object[]> rows = entityManager.createNativeQuery(sql).setParameter("worker", worker)
                    .setParameter("lease", leaseSeconds).setParameter("limit", limit).getResultList();
            List<Claim> claims = new ArrayList<>();
            for (Object[] row : rows) {
                claims.add(new Claim(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).intValue(),
                        (String) row[3], (String) row[4], toInstant(row[5])));
            }
            return claims;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox claimDue()", e);
        }
    }

    @Override
    public void recordAttempt(long eventId, String addonId, String worker, String status, Integer statusCode,
            String error, Instant nextAttemptAt) {
        try {
            // A null parameter is sent as bytea, which no cast repairs, so each
            // optional value is either bound or written as NULL in the statement.
            String sql = "UPDATE clinlims.addon_delivery SET status = :status, attempts = attempts + 1,"
                    + " last_status_code = " + (statusCode == null ? "NULL" : ":code") + ", last_error = "
                    + (error == null ? "NULL" : ":error") + ", next_attempt_at = "
                    + (nextAttemptAt == null ? "next_attempt_at" : ":next")
                    + ", delivered_at = CASE WHEN :status = 'DELIVERED' THEN now() ELSE delivered_at END,"
                    + " locked_by = NULL, locked_until = NULL"
                    + " WHERE event_id = :eventId AND addon_id = :addonId AND locked_by = :worker";
            Query query = entityManager.createNativeQuery(sql).setParameter("status", status)
                    .setParameter("eventId", eventId).setParameter("addonId", addonId).setParameter("worker", worker);
            if (statusCode != null) {
                query.setParameter("code", statusCode);
            }
            if (error != null) {
                query.setParameter("error", error.substring(0, Math.min(error.length(), 500)));
            }
            if (nextAttemptAt != null) {
                query.setParameter("next", Timestamp.from(nextAttemptAt));
            }
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox recordAttempt()", e);
        }
    }

    @Override
    public int dropPending(String addonId) {
        try {
            return entityManager
                    .createNativeQuery("UPDATE clinlims.addon_delivery SET status = 'DROPPED', locked_by = NULL,"
                            + " locked_until = NULL WHERE addon_id = :addonId AND status = 'PENDING'")
                    .setParameter("addonId", addonId).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox dropPending()", e);
        }
    }

    @Override
    public int purgeOlderThan(int days) {
        try {
            return entityManager
                    .createNativeQuery("DELETE FROM clinlims.addon_event"
                            + " WHERE created_at < now() - make_interval(days => :days)")
                    .setParameter("days", days).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox purgeOlderThan()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Map<String, Integer> countsByStatus(String addonId) {
        try {
            List<Object[]> rows = entityManager
                    .createNativeQuery("SELECT status, count(*) FROM clinlims.addon_delivery"
                            + " WHERE addon_id = :addonId GROUP BY status")
                    .setParameter("addonId", addonId).getResultList();
            Map<String, Integer> out = new LinkedHashMap<>();
            for (Object[] row : rows) {
                out.put((String) row[0], ((Number) row[1]).intValue());
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox countsByStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> recent(String addonId, int limit) {
        try {
            List<Object[]> rows = entityManager
                    .createNativeQuery("SELECT d.event_id, e.type, d.status, d.attempts,"
                            + " d.last_status_code, d.last_error, e.created_at, d.delivered_at"
                            + " FROM clinlims.addon_delivery d JOIN clinlims.addon_event e ON e.id = d.event_id"
                            + " WHERE d.addon_id = :addonId ORDER BY d.event_id DESC LIMIT :limit")
                    .setParameter("addonId", addonId).setParameter("limit", limit).getResultList();
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("eventId", ((Number) row[0]).longValue());
                m.put("type", row[1]);
                m.put("status", row[2]);
                m.put("attempts", row[3]);
                m.put("lastStatusCode", row[4]);
                m.put("lastError", row[5]);
                m.put("createdAt", row[6] == null ? null : toInstant(row[6]).toString());
                m.put("deliveredAt", row[7] == null ? null : toInstant(row[7]).toString());
                out.add(m);
            }
            return out;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox recent()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean wasNotified(String addonId, String accessionNumber) {
        try {
            Object found = entityManager
                    .createNativeQuery("SELECT EXISTS (SELECT 1 FROM clinlims.addon_delivery d"
                            + " JOIN clinlims.addon_event e ON e.id = d.event_id WHERE d.addon_id = :addonId"
                            + " AND e.accession_number = :accession AND d.status IN ('PENDING', 'DELIVERED'))")
                    .setParameter("addonId", addonId).setParameter("accession", accessionNumber).getSingleResult();
            return Boolean.TRUE.equals(found);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox wasNotified()", e);
        }
    }

    @Override
    public void emit(String type, String accessionNumber, String dataJson) {
        try {
            Query query = entityManager
                    .createNativeQuery("SELECT CAST(clinlims.addon_emit(:type, "
                            + (accessionNumber == null ? "NULL" : ":accession") + ", :data) AS TEXT)")
                    .setParameter("type", type).setParameter("data", dataJson);
            if (accessionNumber != null) {
                query.setParameter("accession", accessionNumber);
            }
            query.getSingleResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in AddonOutbox emit()", e);
        }
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Timestamp t) {
            return t.toInstant();
        }
        if (value instanceof OffsetDateTime o) {
            return o.toInstant();
        }
        if (value instanceof Instant i) {
            return i;
        }
        throw new IllegalStateException("Unexpected timestamp type " + value);
    }
}
