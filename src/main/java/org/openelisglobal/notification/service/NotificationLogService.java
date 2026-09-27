package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.NotificationLog;

public interface NotificationLogService extends BaseObjectService<NotificationLog, Long> {

    /**
     * Persist a {@link NotificationLog} row from a fire event. Invoked by
     * {@code NotificationLogEventListener} (a separate bean), which is required so
     * the call goes through the service proxy and Spring's {@code @Transactional}
     * advice opens a transaction. Direct self-invocation from within
     * {@code NotificationLogServiceImpl} would bypass the proxy and Hibernate would
     * fail the write with "no transaction is in progress" on the async dispatcher
     * thread.
     */
    void recordFire(NotificationFiredEvent event);

    /**
     * Paginated read used by the admin Sent Messages tab. {@code page} is 1-indexed
     * to match the Carbon Pagination component. Returns the slice matching the
     * filters, ordered by fired_at DESC.
     */
    List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size);

    /**
     * Total matching count, used by Carbon Pagination as {@code totalItems}.
     */
    long countMatching(Optional<String> eventCode, Optional<String> status);

    /**
     * Resend the FAILED channels of the given log row. Creates a NEW
     * {@link NotificationLog} row with {@code resentFromId} pointing to the source
     * row (linked-list, not flat — see plan decision #5). Recipient contact info,
     * rendered subject + message are copied verbatim from the source
     * (replay-original-bytes per decision #1).
     *
     * @throws IllegalArgumentException if the source row has no FAILED channels
     *                                  (resend has nothing to retry)
     */
    NotificationLog resend(Long sourceLogId, String sysUserId);
}
