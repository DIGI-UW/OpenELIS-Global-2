package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCAlert represents a notification sent to a user about a QC violation.
 */
@Entity
@Table(name = "qc_alert")
public class QCAlert extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "violation_id", nullable = false, length = 36)
    private String violationId;

    @NotNull
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    @NotNull
    @Column(name = "recipient_user_id", nullable = false)
    private Integer recipientUserId;

    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;

    @NotNull
    @Column(name = "sent_date_time", nullable = false)
    private Timestamp sentDateTime;

    @NotNull
    @Column(name = "read_status", nullable = false)
    private Boolean readStatus = false;

    @Column(name = "read_date_time")
    private Timestamp readDateTime;

    @Column(name = "message_subject", length = 500)
    private String messageSubject;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    public QCAlert() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getViolationId() {
        return violationId;
    }

    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public Integer getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Integer recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public Timestamp getSentDateTime() {
        return sentDateTime;
    }

    public void setSentDateTime(Timestamp sentDateTime) {
        this.sentDateTime = sentDateTime;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
    }

    public Timestamp getReadDateTime() {
        return readDateTime;
    }

    public void setReadDateTime(Timestamp readDateTime) {
        this.readDateTime = readDateTime;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public void setMessageSubject(String messageSubject) {
        this.messageSubject = messageSubject;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
