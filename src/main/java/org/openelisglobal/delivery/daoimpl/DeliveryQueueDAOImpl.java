package org.openelisglobal.delivery.daoimpl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.delivery.dao.DeliveryQueueDAO;
import org.openelisglobal.delivery.dao.DeliveryQueueSearch;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DeliveryQueueDAOImpl implements DeliveryQueueDAO {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Claim> claimDue(String worker, int limit, int leaseSeconds) {
        try {
            String sql = """
                    UPDATE clinlims.notification_log_channel c
                    SET locked_by = :worker, locked_until = now() + make_interval(secs => :lease)
                    WHERE (c.notification_log_id, c.channel) IN (
                        SELECT d.notification_log_id, d.channel
                        FROM clinlims.notification_log_channel d
                        WHERE d.status = 'QUEUED'
                          AND (d.next_attempt_at IS NULL OR d.next_attempt_at <= now())
                          AND (d.locked_until IS NULL OR d.locked_until < now())
                        ORDER BY d.next_attempt_at NULLS FIRST, d.notification_log_id
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED)
                    RETURNING c.notification_log_id, c.channel, c.attempts
                    """;
            List<Object[]> rows = entityManager.createNativeQuery(sql).setParameter("worker", worker)
                    .setParameter("lease", leaseSeconds).setParameter("limit", limit).getResultList();
            List<Claim> claims = new ArrayList<>();
            for (Object[] row : rows) {
                claims.add(new Claim(((Number) row[0]).longValue(), NotificationChannel.valueOf((String) row[1]),
                        ((Number) row[2]).intValue()));
            }
            return claims;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue claimDue()", e);
        }
    }

    @Override
    public boolean extendLease(Long logId, NotificationChannel channel, String worker, int leaseSeconds) {
        try {
            int renewed = entityManager
                    .createNativeQuery("UPDATE clinlims.notification_log_channel"
                            + " SET locked_until = now() + make_interval(secs => :lease)"
                            + " WHERE notification_log_id = :logId AND channel = :channel AND status = 'QUEUED'"
                            + " AND locked_by = :worker AND locked_until > now()")
                    .setParameter("lease", leaseSeconds).setParameter("logId", logId)
                    .setParameter("channel", channel.name()).setParameter("worker", worker).executeUpdate();
            return renewed == 1;
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue extendLease()", e);
        }
    }

    @Override
    public void release(Long logId, NotificationChannel channel, String worker) {
        try {
            entityManager
                    .createNativeQuery("UPDATE clinlims.notification_log_channel SET locked_by = NULL,"
                            + " locked_until = NULL WHERE notification_log_id = :logId AND channel = :channel"
                            + " AND locked_by = :worker")
                    .setParameter("logId", logId).setParameter("channel", channel.name()).setParameter("worker", worker)
                    .executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue release()", e);
        }
    }

    @Override
    public void recordAttempt(Long logId, NotificationChannel channel, ChannelStatus status, int attempts, String error,
            Timestamp nextAttemptAt, String gatewayMessageId) {
        try {
            // A null parameter is sent as bytea, which no cast repairs, so each
            // optional value is either bound or written as NULL in the statement.
            String sql = "UPDATE clinlims.notification_log_channel SET status = :status, attempts = attempts + 1,"
                    + " error_message = " + (error == null ? "NULL" : ":error") + ", last_attempted_at = now(),"
                    + " next_attempt_at = " + (nextAttemptAt == null ? "NULL" : ":next") + ", gateway_message_id = "
                    + (gatewayMessageId == null ? "gateway_message_id" : ":gatewayId")
                    + ", locked_by = NULL, locked_until = NULL"
                    + " WHERE notification_log_id = :logId AND channel = :channel";
            Query query = entityManager.createNativeQuery(sql).setParameter("status", status.name())
                    .setParameter("logId", logId).setParameter("channel", channel.name());
            if (error != null) {
                query.setParameter("error", error.substring(0, Math.min(error.length(), 2000)));
            }
            if (nextAttemptAt != null) {
                query.setParameter("next", nextAttemptAt);
            }
            if (gatewayMessageId != null) {
                query.setParameter("gatewayId", gatewayMessageId);
            }
            query.executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue recordAttempt()", e);
        }
    }

    @Override
    public void refreshOverallStatus(Long logId) {
        try {
            entityManager.createNativeQuery("""
                    UPDATE clinlims.notification_log l
                    SET overall_status = s.status, last_updated = now()
                    FROM (SELECT CASE
                                   WHEN bool_or(c.status = 'SUCCESS') THEN 'SENT'
                                   WHEN bool_or(c.status = 'QUEUED') THEN 'QUEUED'
                                   WHEN bool_or(c.status = 'FAILED') THEN 'FAILED'
                                   ELSE 'SKIPPED' END AS status
                          FROM clinlims.notification_log_channel c
                          WHERE c.notification_log_id = :logId) s
                    WHERE l.id = :logId
                    """).setParameter("logId", logId).executeUpdate();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue refreshOverallStatus()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> search(DeliveryQueueSearch search, int page, int pageSize) {
        try {
            Map<String, Object> params = new HashMap<>();
            String where = where(search, params);
            TypedQuery<NotificationLog> query = entityManager.createQuery(
                    "SELECT nl FROM NotificationLog nl" + where + " ORDER BY nl.firedAt DESC, nl.id DESC",
                    NotificationLog.class);
            params.forEach(query::setParameter);
            query.setFirstResult(Math.max(0, (page - 1) * pageSize));
            query.setMaxResults(pageSize);
            return query.getResultList();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue search()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long count(DeliveryQueueSearch search) {
        try {
            Map<String, Object> params = new HashMap<>();
            String where = where(search, params);
            Query query = entityManager.createQuery("SELECT COUNT(nl.id) FROM NotificationLog nl" + where);
            params.forEach(query::setParameter);
            Object result = query.getSingleResult();
            return result == null ? 0L : ((Number) result).longValue();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in DeliveryQueue count()", e);
        }
    }

    private String where(DeliveryQueueSearch search, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();
        if (search.getProduct() != null) {
            clauses.add("nl.product = :product");
            params.put("product", search.getProduct());
        }
        if (search.getStatus() != null) {
            clauses.add("nl.overallStatus = :status");
            params.put("status", search.getStatus());
        }
        if (search.getChannel() != null) {
            clauses.add("EXISTS (SELECT 1 FROM nl.channels ch WHERE ch.channel = :channel)");
            params.put("channel", search.getChannel());
        }
        if (search.getFrom() != null) {
            clauses.add("nl.firedAt >= :from");
            params.put("from", Timestamp.from(search.getFrom().toInstant()));
        }
        if (search.getTo() != null) {
            clauses.add("nl.firedAt < :to");
            params.put("to", Timestamp.from(search.getTo().toInstant()));
        }
        if (!GenericValidator.isBlankOrNull(search.getEventCode())) {
            clauses.add("nl.eventCode = :eventCode");
            params.put("eventCode", search.getEventCode().trim());
        }
        if (!GenericValidator.isBlankOrNull(search.getLabNumber())) {
            clauses.add("nl.referenceAccession = :labNumber");
            params.put("labNumber", search.getLabNumber().trim());
        }
        if (!GenericValidator.isBlankOrNull(search.getRecipient())) {
            clauses.add("(LOWER(nl.recipientDisplayName) LIKE :recipient OR LOWER(nl.recipientEmail) LIKE :recipient"
                    + " OR nl.recipientPhone LIKE :recipient)");
            params.put("recipient", "%" + search.getRecipient().trim().toLowerCase() + "%");
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }
}
