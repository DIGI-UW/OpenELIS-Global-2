package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_case_activity", schema = "clinlims")
public class MicroCaseActivity extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "activity_type", nullable = false, length = 40)
    private String activityType;

    @Column(name = "occurred_at", nullable = false)
    private Timestamp occurredAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "performed_by", length = 20)
    private String performedBy;

    @Column(name = "note")
    private String note;

    @Column(name = "structured_data")
    private String structuredData;

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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Timestamp getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Timestamp occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStructuredData() {
        return structuredData;
    }

    public void setStructuredData(String structuredData) {
        this.structuredData = structuredData;
    }
}
