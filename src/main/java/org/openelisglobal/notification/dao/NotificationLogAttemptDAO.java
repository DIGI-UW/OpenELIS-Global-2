package org.openelisglobal.notification.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.NotificationLogAttempt;

public interface NotificationLogAttemptDAO extends BaseDAO<NotificationLogAttempt, Long> {

    List<NotificationLogAttempt> findByLog(Long notificationLogId);

    NotificationLogAttempt save(NotificationLogAttempt attempt);
}
