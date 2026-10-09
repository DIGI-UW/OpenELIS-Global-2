package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_case_split", schema = "clinlims")
public class MicroCaseSplit extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "source_case_id")
    private String sourceCaseId;

    @Column(name = "result_case_id")
    private String resultCaseId;

    @Column(name = "sample_item_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleItemId;

    @Column(name = "occurred_at")
    private Timestamp occurredAt;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "reason")
    private String reason;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSourceCaseId() {
        return sourceCaseId;
    }

    public void setSourceCaseId(String sourceCaseId) {
        this.sourceCaseId = sourceCaseId;
    }

    public String getResultCaseId() {
        return resultCaseId;
    }

    public void setResultCaseId(String resultCaseId) {
        this.resultCaseId = resultCaseId;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
