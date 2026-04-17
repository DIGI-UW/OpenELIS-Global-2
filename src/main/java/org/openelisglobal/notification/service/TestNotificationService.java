package org.openelisglobal.notification.service;

import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestNotificationService {

    @PreAuthorize("hasAuthority('PRIV_NOTIFICATION_MANAGE')")
    void createAndSendNotificationsToConfiguredSources(NotificationNature nature, Result result);
}
