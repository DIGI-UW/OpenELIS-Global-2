package org.openelisglobal.notification.valueholder;

import java.util.Map;

public class WhatsAppNotification implements RemoteNotification {

    private NotificationPayload payload;

    private String receiverPhoneNumber;

    private String templateContentSid;

    private Map<String, String> templateVariables;

    @Override
    public String getMessage() {
        return payload == null ? null : payload.getMessage();
    }

    @Override
    public String getSubject() {
        return payload == null ? null : payload.getSubject();
    }

    public NotificationPayload getPayload() {
        return payload;
    }

    public void setPayload(NotificationPayload payload) {
        this.payload = payload;
    }

    public String getReceiverPhoneNumber() {
        return receiverPhoneNumber;
    }

    public void setReceiverPhoneNumber(String receiverPhoneNumber) {
        this.receiverPhoneNumber = receiverPhoneNumber;
    }

    public String getTemplateContentSid() {
        return templateContentSid;
    }

    public void setTemplateContentSid(String templateContentSid) {
        this.templateContentSid = templateContentSid;
    }

    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
    }
}
