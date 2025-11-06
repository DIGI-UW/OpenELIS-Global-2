package org.openelisglobal.notification.valueholder;

public class SMSNotification implements RemoteNotification {

    private NotificationPayload payload;

    private String receiverPhoneNumber;

    @Override
    public String getMessage() {
        return payload.getMessage();
    }

    public void setPayload(NotificationPayload payload) {
        this.payload = payload;
    }

    @Override
    public String getSubject() {
        return payload.getSubject();
    }

    public String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    public void setReceiverPhoneNumber(String receiverPhoneNumber) {
        this.receiverPhoneNumber = receiverPhoneNumber;
    }
}
