package org.openelisglobal.notification.dao;

import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;

public interface NotificationTriggerConfigDAO extends BaseDAO<NotificationTriggerConfig, Integer> {

    Optional<NotificationTriggerConfig> findByEventCode(String eventCode);
}
