package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;

public interface NotificationTriggerConfigService extends BaseObjectService<NotificationTriggerConfig, Integer> {

    List<NotificationTriggerConfig> getAllConfigs();

    Optional<NotificationTriggerConfig> getByEventCode(String eventCode);

    NotificationTriggerConfig saveConfig(NotificationTriggerConfig incoming, String sysUserId);

    void saveAll(List<NotificationTriggerConfig> incoming, String sysUserId);
}
