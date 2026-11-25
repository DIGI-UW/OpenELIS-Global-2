package org.openelisglobal.delivery.service.sender;

import java.util.Map;
import org.openelisglobal.notification.valueholder.NotificationChannel;

/**
 * What one channel send carries: the address on that channel, the rendered
 * subject and body, and for WhatsApp the optional provider template id with its
 * variables.
 */
public class DeliveryMessage {

    private final NotificationChannel channel;
    private final String address;
    private final String subject;
    private final String body;
    private final String templateExternalId;
    private final Map<String, String> templateVariables;

    public DeliveryMessage(NotificationChannel channel, String address, String subject, String body) {
        this(channel, address, subject, body, null, null);
    }

    public DeliveryMessage(NotificationChannel channel, String address, String subject, String body,
            String templateExternalId, Map<String, String> templateVariables) {
        this.channel = channel;
        this.address = address;
        this.subject = subject;
        this.body = body;
        this.templateExternalId = templateExternalId;
        this.templateVariables = templateVariables;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getAddress() {
        return address;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getTemplateExternalId() {
        return templateExternalId;
    }

    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }
}
