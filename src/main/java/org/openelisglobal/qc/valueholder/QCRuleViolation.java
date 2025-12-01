package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCRuleViolation represents a detected violation of a Westgard rule.
 */
@Entity
@Table(name = "qc_rule_violation")
public class QCRuleViolation extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "triggering_result_id", nullable = false, length = 36)
    private String triggeringResultId;

    @NotNull
    @Column(name = "rule_code", nullable = false, length = 20)
    private String ruleCode;

    @NotNull
    @Column(name = "violation_date_time", nullable = false)
    private Timestamp violationDateTime;

    @NotNull
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @NotNull
    @Column(name = "instrument_id", nullable = false)
    private Integer instrumentId;

    @NotNull
    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @NotNull
    @Column(name = "resolution_status", nullable = false, length = 50)
    private String resolutionStatus = "UNRESOLVED";

    @Column(name = "resolved_date_time")
    private Timestamp resolvedDateTime;

    @Column(name = "resolved_by_user_id")
    private Integer resolvedByUserId;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public QCRuleViolation() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getTriggeringResultId() {
        return triggeringResultId;
    }

    public void setTriggeringResultId(String triggeringResultId) {
        this.triggeringResultId = triggeringResultId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Timestamp getViolationDateTime() {
        return violationDateTime;
    }

    public void setViolationDateTime(Timestamp violationDateTime) {
        this.violationDateTime = violationDateTime;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Integer instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public String getResolutionStatus() {
        return resolutionStatus;
    }

    public void setResolutionStatus(String resolutionStatus) {
        this.resolutionStatus = resolutionStatus;
    }

    public Timestamp getResolvedDateTime() {
        return resolvedDateTime;
    }

    public void setResolvedDateTime(Timestamp resolvedDateTime) {
        this.resolvedDateTime = resolvedDateTime;
    }

    public Integer getResolvedByUserId() {
        return resolvedByUserId;
    }

    public void setResolvedByUserId(Integer resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
