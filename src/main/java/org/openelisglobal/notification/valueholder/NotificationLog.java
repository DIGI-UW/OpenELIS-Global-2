package org.openelisglobal.notification.valueholder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One row per notification fire-instance per recipient — backs the Sent
 * Messages admin tab. The dispatcher publishes a {@code
 * NotificationFiredEvent} after each fire; {@code NotificationLogService}
 * listens and writes a row here.
 *
 * <p>
 * Per-channel outcomes are stored in {@code notification_log_channel} as an
 * {@code @ElementCollection} of {@link NotificationLogChannel}. Composite PK
 * (parent_id, channel); no Hibernate identity / optimistic-lock on the child.
 *
 * <p>
 * {@code rendered_subject} + {@code rendered_message} hold the literal bytes
 * the sender received. They back the Resend action ("replay original" per the
 * locked decision) and the View Log modal display.
 *
 * <p>
 * Privacy note: this table persists recipient email/phone + full rendered
 * message bodies (which include patient identifiers via merge fields).
 */
@Entity
@Table(name = "notification_log")
public class NotificationLog extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum OverallStatus {
        SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_log_generator")
    @SequenceGenerator(name = "notification_log_generator", sequenceName = "notification_log_seq", allocationSize = 1)
    private Long id;

    @Column(name = "event_code", nullable = false, length = 64)
    private String eventCode;

    @Column(name = "fired_at", nullable = false)
    private Timestamp firedAt;

    @Column(name = "triggered_by_user_id", length = 64)
    private String triggeredByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 32)
    private NotificationRecipientType recipientType;

    @Column(name = "recipient_display_name", length = 255)
    private String recipientDisplayName;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @Column(name = "recipient_phone", length = 64)
    private String recipientPhone;

    @Column(name = "reference_accession", length = 64)
    private String referenceAccession;

    @Column(name = "reference_workflow", length = 32)
    private String referenceWorkflow;

    @Column(name = "rendered_subject", length = 1000)
    private String renderedSubject;

    @Column(name = "rendered_message", length = 10000)
    private String renderedMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 16)
    private OverallStatus overallStatus;

    // Soft FK — points to the immediate predecessor in a resend chain. Null
    // for original fires. Linked-list, not flat (decision #5).
    @Column(name = "resent_from_id")
    private Long resentFromId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_log_channel", joinColumns = @JoinColumn(name = "notification_log_id"))
    private Set<NotificationLogChannel> channels = new HashSet<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public Timestamp getFiredAt() {
        return firedAt;
    }

    public void setFiredAt(Timestamp firedAt) {
        this.firedAt = firedAt;
    }

    public String getTriggeredByUserId() {
        return triggeredByUserId;
    }

    public void setTriggeredByUserId(String triggeredByUserId) {
        this.triggeredByUserId = triggeredByUserId;
    }

    public NotificationRecipientType getRecipientType() {
        return recipientType;
    }

    public void setRecipientType(NotificationRecipientType recipientType) {
        this.recipientType = recipientType;
    }

    public String getRecipientDisplayName() {
        return recipientDisplayName;
    }

    public void setRecipientDisplayName(String recipientDisplayName) {
        this.recipientDisplayName = recipientDisplayName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getReferenceAccession() {
        return referenceAccession;
    }

    public void setReferenceAccession(String referenceAccession) {
        this.referenceAccession = referenceAccession;
    }

    public String getReferenceWorkflow() {
        return referenceWorkflow;
    }

    public void setReferenceWorkflow(String referenceWorkflow) {
        this.referenceWorkflow = referenceWorkflow;
    }

    public String getRenderedSubject() {
        return renderedSubject;
    }

    public void setRenderedSubject(String renderedSubject) {
        this.renderedSubject = renderedSubject;
    }

    public String getRenderedMessage() {
        return renderedMessage;
    }

    public void setRenderedMessage(String renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public Long getResentFromId() {
        return resentFromId;
    }

    public void setResentFromId(Long resentFromId) {
        this.resentFromId = resentFromId;
    }

    public Set<NotificationLogChannel> getChannels() {
        if (channels == null) {
            channels = new HashSet<>();
        }
        return channels;
    }

    public void setChannels(Set<NotificationLogChannel> channels) {
        this.channels = channels == null ? new HashSet<>() : channels;
    }
}
