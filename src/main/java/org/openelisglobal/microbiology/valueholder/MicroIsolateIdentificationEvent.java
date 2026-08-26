package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_isolate_identification_event", schema = "clinlims")
public class MicroIsolateIdentificationEvent extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "isolate_id", nullable = false, length = 36)
    private String isolateId;

    @Column(name = "amendment_id", length = 36)
    private String amendmentId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "previous_organism_id", length = 36)
    private String previousOrganismId;

    @Column(name = "previous_organism_text")
    private String previousOrganismText;

    @Column(name = "previous_significance", length = 40)
    private String previousSignificance;

    @Column(name = "previous_identification_status", length = 40)
    private String previousIdentificationStatus;

    @Column(name = "previous_identification_method", length = 80)
    private String previousIdentificationMethod;

    @Column(name = "previous_identification_confidence", precision = 5, scale = 2)
    private BigDecimal previousIdentificationConfidence;

    @Column(name = "new_organism_id", length = 36)
    private String newOrganismId;

    @Column(name = "new_organism_text")
    private String newOrganismText;

    @Column(name = "new_significance", length = 40)
    private String newSignificance;

    @Column(name = "new_identification_status", length = 40)
    private String newIdentificationStatus;

    @Column(name = "new_identification_method", length = 80)
    private String newIdentificationMethod;

    @Column(name = "new_identification_confidence", precision = 5, scale = 2)
    private BigDecimal newIdentificationConfidence;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "changed_at", nullable = false)
    private Timestamp changedAt;

    @Column(name = "changed_by", nullable = false, length = 20)
    private String changedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIsolateId() {
        return isolateId;
    }

    public void setIsolateId(String isolateId) {
        this.isolateId = isolateId;
    }

    public String getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(String amendmentId) {
        this.amendmentId = amendmentId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPreviousOrganismId() {
        return previousOrganismId;
    }

    public void setPreviousOrganismId(String previousOrganismId) {
        this.previousOrganismId = previousOrganismId;
    }

    public String getPreviousOrganismText() {
        return previousOrganismText;
    }

    public void setPreviousOrganismText(String previousOrganismText) {
        this.previousOrganismText = previousOrganismText;
    }

    public String getPreviousSignificance() {
        return previousSignificance;
    }

    public void setPreviousSignificance(String previousSignificance) {
        this.previousSignificance = previousSignificance;
    }

    public String getPreviousIdentificationStatus() {
        return previousIdentificationStatus;
    }

    public void setPreviousIdentificationStatus(String previousIdentificationStatus) {
        this.previousIdentificationStatus = previousIdentificationStatus;
    }

    public String getPreviousIdentificationMethod() {
        return previousIdentificationMethod;
    }

    public void setPreviousIdentificationMethod(String previousIdentificationMethod) {
        this.previousIdentificationMethod = previousIdentificationMethod;
    }

    public BigDecimal getPreviousIdentificationConfidence() {
        return previousIdentificationConfidence;
    }

    public void setPreviousIdentificationConfidence(BigDecimal previousIdentificationConfidence) {
        this.previousIdentificationConfidence = previousIdentificationConfidence;
    }

    public String getNewOrganismId() {
        return newOrganismId;
    }

    public void setNewOrganismId(String newOrganismId) {
        this.newOrganismId = newOrganismId;
    }

    public String getNewOrganismText() {
        return newOrganismText;
    }

    public void setNewOrganismText(String newOrganismText) {
        this.newOrganismText = newOrganismText;
    }

    public String getNewSignificance() {
        return newSignificance;
    }

    public void setNewSignificance(String newSignificance) {
        this.newSignificance = newSignificance;
    }

    public String getNewIdentificationStatus() {
        return newIdentificationStatus;
    }

    public void setNewIdentificationStatus(String newIdentificationStatus) {
        this.newIdentificationStatus = newIdentificationStatus;
    }

    public String getNewIdentificationMethod() {
        return newIdentificationMethod;
    }

    public void setNewIdentificationMethod(String newIdentificationMethod) {
        this.newIdentificationMethod = newIdentificationMethod;
    }

    public BigDecimal getNewIdentificationConfidence() {
        return newIdentificationConfidence;
    }

    public void setNewIdentificationConfidence(BigDecimal newIdentificationConfidence) {
        this.newIdentificationConfidence = newIdentificationConfidence;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
