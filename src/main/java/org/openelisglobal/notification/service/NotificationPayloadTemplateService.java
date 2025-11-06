package org.openelisglobal.notification.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

public interface NotificationPayloadTemplateService extends BaseObjectService<NotificationPayloadTemplate, Integer> {

    public NotificationPayloadTemplate getSystemDefaultPayloadTemplateForType(NotificationPayloadType type);

    void updatePayloadTemplateMessagesAndSubject(NotificationPayloadTemplate newPayloadTemplate, String sysUserId);
}
