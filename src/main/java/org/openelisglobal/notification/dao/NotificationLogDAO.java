package org.openelisglobal.notification.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.NotificationLog;

public interface NotificationLogDAO extends BaseDAO<NotificationLog, Long> {

    /**
     * Paginated read used by the admin Sent Messages tab. Page is 1-indexed.
     * Returns the slice of {@link NotificationLog} rows ordered by
     * {@code fired_at DESC} that match the optional filters.
     */
    List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size);

    /**
     * Total row count matching the same filters used by {@link #findPage}. The REST
     * DTO returns this as {@code totalItems} for Carbon Pagination.
     */
    long countMatching(Optional<String> eventCode, Optional<String> status);
}
