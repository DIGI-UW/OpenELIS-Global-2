package org.openelisglobal.delivery.dao;

import java.time.OffsetDateTime;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog.OverallStatus;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;

/** The Sent Messages filters; every field is optional. */
public class DeliveryQueueSearch {

    private Product product;
    private OverallStatus status;
    private NotificationChannel channel;
    private OffsetDateTime from;
    private OffsetDateTime to;
    private String labNumber;
    private String recipient;
    private String eventCode;

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public OverallStatus getStatus() {
        return status;
    }

    public void setStatus(OverallStatus status) {
        this.status = status;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public OffsetDateTime getFrom() {
        return from;
    }

    public void setFrom(OffsetDateTime from) {
        this.from = from;
    }

    public OffsetDateTime getTo() {
        return to;
    }

    public void setTo(OffsetDateTime to) {
        this.to = to;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
}
