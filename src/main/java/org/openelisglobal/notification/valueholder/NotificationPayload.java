package org.openelisglobal.notification.valueholder;

public interface NotificationPayload {

    public String getMessage();

    public String getSubject();

    static NotificationPayload of(String subject, String message) {
        String s = subject == null ? "" : subject;
        String m = message == null ? "" : message;
        return new NotificationPayload() {
            @Override
            public String getSubject() {
                return s;
            }

            @Override
            public String getMessage() {
                return m;
            }
        };
    }
}
