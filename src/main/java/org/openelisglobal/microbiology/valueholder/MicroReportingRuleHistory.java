package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_reporting_rule_history", schema = "clinlims")
public class MicroReportingRuleHistory extends BaseObject<String> {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String value) {
        id = value;
    }

    @Column(name = "rule_id", nullable = false, length = 36)
    private String ruleId;

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String value) {
        ruleId = value;
    }

    @Column(name = "revision", nullable = false)
    private int revision;

    public int getRevision() {
        return revision;
    }

    public void setRevision(int value) {
        revision = value;
    }

    @Column(name = "changed_at", nullable = false)
    private Timestamp changedAt;

    public Timestamp getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Timestamp value) {
        changedAt = value;
    }

    @Column(name = "changed_by", nullable = false, length = 36)
    private String changedBy;

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String value) {
        changedBy = value;
    }

    @Column(name = "before_value", columnDefinition = "text")
    private String beforeValue;

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String value) {
        beforeValue = value;
    }

    @Column(name = "after_value", nullable = false, columnDefinition = "text")
    private String afterValue;

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String value) {
        afterValue = value;
    }
}
