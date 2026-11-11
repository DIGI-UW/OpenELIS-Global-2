package org.openelisglobal.testalertrule.service;

import org.openelisglobal.notification.valueholder.NotificationPayload;

/**
 * OGC-949 / OGC-763 — a plain message/subject payload for a fired test alert,
 * dispatched through the same SMS/Email senders the testNotificationConfig page
 * uses
 * ({@link org.openelisglobal.notification.service.sender.ClientNotificationSender}).
 */
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
