package org.openelisglobal.testsamplehandling.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-949 / OGC-766 — an audit snapshot of a {@code test_sample_handling}
 * change (the v1 table is lit up for writes in v2). Each row captures the
 * before/after JSON of the handling config, who changed it and when. The JSONB
 * columns map as String with a {@code ?::jsonb} write transform — the
 * established OpenELIS idiom (see {@code TestActivationAcknowledgment}).
 */
@Entity
@Table(name = "test_sample_handling_history", schema = "clinlims")
public class TestSampleHandlingHistory extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "test_sample_handling_id", nullable = false, length = 36)
    private String testSampleHandlingId;

    @Column(name = "changed_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String changedBy;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private Timestamp changedAt;

    @Column(name = "change_type", length = 10)
    private String changeType;

    @Column(name = "previous_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String previousValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String newValues;

    public TestSampleHandlingHistory() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTestSampleHandlingId() {
        return testSampleHandlingId;
    }

    public void setTestSampleHandlingId(String testSampleHandlingId) {
        this.testSampleHandlingId = testSampleHandlingId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp changedAt) {
        this.changedAt = changedAt;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(String previousValues) {
        this.previousValues = previousValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }
}
