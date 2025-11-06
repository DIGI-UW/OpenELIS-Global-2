package org.openelisglobal.notification.dao;

import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

public interface NotificationPayloadTemplateDAO extends BaseDAO<NotificationPayloadTemplate, Integer> {

    NotificationPayloadTemplate getSystemDefaultPayloadTemplateForType(NotificationPayloadType type);
}
