package org.openelisglobal.notification.service;

import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.result.valueholder.Result;

public interface TestNotificationService {

    void createAndSendNotificationsToConfiguredSources(NotificationNature nature, Result result);
}
