package org.openelisglobal.notification.service.sender;

import org.openelisglobal.notification.valueholder.RemoteNotification;

public interface ClientNotificationSender<T extends RemoteNotification> {

    public Class<T> forClass();

    public void send(T notification);
}
