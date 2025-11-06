package org.openelisglobal.notification.valueholder;

import java.util.List;

public class EmailNotification implements RemoteNotification {

    private String recipientEmailAddress;

    private List<String> bccs;

    private NotificationPayload payload;

    public String getRecipientEmailAddress() {
        return recipientEmailAddress;
    }

    public void setRecipientEmailAddress(String recipientEmailAddress) {
        this.recipientEmailAddress = recipientEmailAddress;
    }

    public NotificationPayload getPayload() {
        return payload;
    }

    public void setPayload(NotificationPayload payload) {
        this.payload = payload;
    }

    @Override
    public String getMessage() {
        return payload.getMessage();
    }

    @Override
    public String getSubject() {
        return payload.getSubject();
    }

    public List<String> getBccs() {
        return bccs;
    }

    public void setBccs(List<String> bccs) {
        this.bccs = bccs;
    }
}
