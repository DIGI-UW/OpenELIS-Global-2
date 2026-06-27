package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_critical_communication", schema = "clinlims")
public class MicroCriticalCommunication extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "communicated_at", nullable = false)
    private Timestamp communicatedAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "communicated_by", length = 20)
    private String communicatedBy;

    @Column(name = "acknowledgement_status", nullable = false, length = 40)
    private String acknowledgementStatus = MicroCriticalCommunicationStatus.OPEN.name();

    @Column(name = "acknowledged_at")
    private Timestamp acknowledgedAt;

    @Column(name = "acknowledged_by", length = 20)
    private String acknowledgedBy;

    @Column(name = "follow_up_needed", nullable = false)
    private Boolean followUpNeeded = Boolean.FALSE;

    @Column(name = "correction_of_id", length = 36)
    private String correctionOfId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getCommunicatedAt() {
        return communicatedAt;
    }

    public void setCommunicatedAt(Timestamp communicatedAt) {
        this.communicatedAt = communicatedAt;
    }

    public String getCommunicatedBy() {
        return communicatedBy;
    }

    public void setCommunicatedBy(String communicatedBy) {
        this.communicatedBy = communicatedBy;
    }

    public String getAcknowledgementStatus() {
        return acknowledgementStatus;
    }

    public void setAcknowledgementStatus(String acknowledgementStatus) {
        this.acknowledgementStatus = acknowledgementStatus;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Boolean getFollowUpNeeded() {
        return followUpNeeded;
    }

    public void setFollowUpNeeded(Boolean followUpNeeded) {
        this.followUpNeeded = followUpNeeded;
    }

    public String getCorrectionOfId() {
        return correctionOfId;
    }

    public void setCorrectionOfId(String correctionOfId) {
        this.correctionOfId = correctionOfId;
    }
}
