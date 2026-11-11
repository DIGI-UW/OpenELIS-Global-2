package org.openelisglobal.delivery.service;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.notification.valueholder.NotificationChannel;
import org.openelisglobal.notification.valueholder.NotificationLog.Product;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;

/**
 * Everything the outbox needs to queue one message to one recipient: the
 * product and event, who the recipient is on each channel, the rendered text,
 * and the references the Sent Messages row shows.
 */
public class DeliveryRequest {

    private Product product = Product.TRIGGER;
    private String eventCode;
    private NotificationRecipientType recipientType = NotificationRecipientType.TYPED_ADDRESS;
    private String recipientDisplayName;
    private String recipientEmail;
    private String recipientPhone;
    private Set<NotificationChannel> channels = new LinkedHashSet<>();
    private String subject;
    private String message;
    private String referenceAccession;
    private String referenceWorkflow;
    private String ruleName;
    private Long reportVersionId;
    private String reportFormat;
    private String templateExternalId;
    private Map<String, String> templateVariables;
    /** The user who caused the send; null means automatic. */
    private String sentByUserId;
    private Long resentFromId;

    public Product getProduct() {
        return product;
    }

    public DeliveryRequest setProduct(Product product) {
        this.product = product;
        return this;
    }

    public String getEventCode() {
        return eventCode;
    }

    public DeliveryRequest setEventCode(String eventCode) {
        this.eventCode = eventCode;
        return this;
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public DeliveryRequest setRecipientType(NotificationRecipientType recipientType) {
        this.recipientType = recipientType;
        return this;
    }

    public String getRecipientDisplayName() {
        return recipientDisplayName;
    }

    public DeliveryRequest setRecipientDisplayName(String recipientDisplayName) {
        this.recipientDisplayName = recipientDisplayName;
        return this;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public DeliveryRequest setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
        return this;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public DeliveryRequest setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
        return this;
    }

    public Set<NotificationChannel> getChannels() {
        return channels;
    }

    public DeliveryRequest setChannels(Set<NotificationChannel> channels) {
        this.channels = channels == null ? new LinkedHashSet<>() : new LinkedHashSet<>(channels);
        return this;
    }

    public DeliveryRequest addChannel(NotificationChannel channel) {
        this.channels.add(channel);
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public DeliveryRequest setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public DeliveryRequest setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getReferenceAccession() {
        return referenceAccession;
    }

    public DeliveryRequest setReferenceAccession(String referenceAccession) {
        this.referenceAccession = referenceAccession;
        return this;
    }

    public String getReferenceWorkflow() {
        return referenceWorkflow;
    }

    public DeliveryRequest setReferenceWorkflow(String referenceWorkflow) {
        this.referenceWorkflow = referenceWorkflow;
        return this;
    }

    public String getRuleName() {
        return ruleName;
    }

    public DeliveryRequest setRuleName(String ruleName) {
        this.ruleName = ruleName;
        return this;
    }

    public Long getReportVersionId() {
        return reportVersionId;
    }

    public DeliveryRequest setReportVersionId(Long reportVersionId) {
        this.reportVersionId = reportVersionId;
        return this;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public DeliveryRequest setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
        return this;
    }

    public String getTemplateExternalId() {
        return templateExternalId;
    }

    public DeliveryRequest setTemplateExternalId(String templateExternalId) {
        this.templateExternalId = templateExternalId;
        return this;
    }

    public Map<String, String> getTemplateVariables() {
        return templateVariables;
    }

    public DeliveryRequest setTemplateVariables(Map<String, String> templateVariables) {
        this.templateVariables = templateVariables;
        return this;
    }

    public String getSentByUserId() {
        return sentByUserId;
    }

    public DeliveryRequest setSentByUserId(String sentByUserId) {
        this.sentByUserId = sentByUserId;
        return this;
    }

    public Long getResentFromId() {
        return resentFromId;
    }

    public DeliveryRequest setResentFromId(Long resentFromId) {
        this.resentFromId = resentFromId;
        return this;
    }
}
