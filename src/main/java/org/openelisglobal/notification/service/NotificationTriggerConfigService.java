package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.NotificationTriggerConfig;
import org.springframework.security.access.prepost.PreAuthorize;

public interface NotificationTriggerConfigService extends BaseObjectService<NotificationTriggerConfig, Integer> {

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_VIEW')")
    List<NotificationTriggerConfig> getAllConfigs();

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_VIEW')")
    Optional<NotificationTriggerConfig> getByEventCode(String eventCode);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    NotificationTriggerConfig saveConfig(NotificationTriggerConfig incoming, String sysUserId);

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void saveAll(List<NotificationTriggerConfig> incoming, String sysUserId);
}
