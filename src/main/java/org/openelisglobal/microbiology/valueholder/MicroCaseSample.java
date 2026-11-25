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
@Table(name = "micro_case_sample", schema = "clinlims")
public class MicroCaseSample extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id")
    private String caseId;

    @Column(name = "sample_item_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String sampleItemId;

    @Column(name = "joined_at")
    private Timestamp joinedAt;

    @Column(name = "joined_by")
    private String joinedBy;

    @Column(name = "split_out_at")
    private Timestamp splitOutAt;

    @Column(name = "split_out_by")
    private String splitOutBy;

    @Column(name = "split_reason")
    private String splitReason;

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

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Timestamp getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Timestamp joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getJoinedBy() {
        return joinedBy;
    }

    public void setJoinedBy(String joinedBy) {
        this.joinedBy = joinedBy;
    }

    public Timestamp getSplitOutAt() {
        return splitOutAt;
    }

    public void setSplitOutAt(Timestamp splitOutAt) {
        this.splitOutAt = splitOutAt;
    }

    public String getSplitOutBy() {
        return splitOutBy;
    }

    public void setSplitOutBy(String splitOutBy) {
        this.splitOutBy = splitOutBy;
    }

    public String getSplitReason() {
        return splitReason;
    }

    public void setSplitReason(String splitReason) {
        this.splitReason = splitReason;
    }
}
