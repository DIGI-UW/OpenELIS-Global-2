package org.openelisglobal.delivery.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.openelisglobal.notification.valueholder.NotificationLogChannel.ChannelStatus;

/**
 * The outbox side of the notification log: claiming due channel rows for one
 * worker, recording an attempt's outcome and searching the log for Sent
 * Messages. Writes go through SQL on the channel row, never through the
 * parent's element collection, so two workers (the WAR runs twice in one JVM)
 * never overwrite each other's claims.
 */
public interface DeliveryQueueDAO {

    /** One claimed channel row of a message. */
    /**
     * Renews this worker's lease on one row right before the send; false when the
     * lease has lapsed and another worker may hold the row, so no send happens.
     */
    boolean extendLease(Long logId, NotificationChannel channel, String worker, int leaseSeconds);

    record Claim(Long logId, NotificationChannel channel, int attempts) {
    }

    /** Claims up to {@code limit} due QUEUED rows for {@code worker}. */
    List<Claim> claimDue(String worker, int limit, int leaseSeconds);

    /** Releases a claim without an outcome, for example when the worker stops. */
    void release(Long logId, NotificationChannel channel, String worker);

    /**
     * Records an attempt's outcome on the channel row: the new status, the attempt
     * count, the error, when the next attempt is due (null when final) and the
     * gateway's message id when it gave one.
     */
    void recordAttempt(Long logId, NotificationChannel channel, ChannelStatus status, int attempts, String error,
            Timestamp nextAttemptAt, String gatewayMessageId);

    /** Recomputes the parent's overall status from its channel rows. */
    void refreshOverallStatus(Long logId);

    /** Rows matching the Sent Messages filters, newest first, one page. */
    List<NotificationLog> search(DeliveryQueueSearch search, int page, int pageSize);

    long count(DeliveryQueueSearch search);
}
