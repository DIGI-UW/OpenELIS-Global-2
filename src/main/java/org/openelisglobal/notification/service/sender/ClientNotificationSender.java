package org.openelisglobal.notification.service.sender;

import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.notification.valueholder.RemoteNotification;

@CrossDomainService(callers = "notification dispatch infrastructure — internal")
public interface ClientNotificationSender<T extends RemoteNotification> {

    public Class<T> forClass();

    public void send(T notification);
}
