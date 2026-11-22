package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.NotificationLog;

public interface NotificationLogService extends BaseObjectService<NotificationLog, Long> {

    /**
     * Paginated read of the log. {@code page} is 1-indexed to match the Carbon
     * Pagination component. Returns the slice matching the filters, ordered by
     * fired_at DESC.
     */
    List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size);

    /** Total matching count, used by Carbon Pagination as {@code totalItems}. */
    long countMatching(Optional<String> eventCode, Optional<String> status);

    /**
     * Queues the same rendered content again to the same recipient as a NEW row
     * whose {@code resentFromId} points to the source row; the delivery worker
     * sends it.
     *
     * @throws IllegalArgumentException if the source row does not exist or is still
     *                                  queued
     */
    NotificationLog resend(Long sourceLogId, String sysUserId);
}
