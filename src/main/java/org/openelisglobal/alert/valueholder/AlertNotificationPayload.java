package org.openelisglobal.alert.valueholder;

import org.openelisglobal.notification.valueholder.NotificationPayload;

public class AlertNotificationPayload implements NotificationPayload {

    private final String subject;
    private final String message;

    public AlertNotificationPayload(String subject, String message) {
        this.subject = subject;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getSubject() {
        return subject;
    }
}
