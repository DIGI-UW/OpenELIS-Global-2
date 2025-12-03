package org.openelisglobal.qc.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;

/**
 * Form DTO for QC Rule Violation data (T110).
 *
 * Used for REST API responses and request validation.
 */
public class QCViolationForm {

    private String id;

    @NotBlank(message = "Triggering result ID is required")
    private String triggeringResultId;

    @NotBlank(message = "Rule code is required")
    private String ruleCode;

    private Timestamp violationDateTime;

    @NotBlank(message = "Severity is required")
    private String severity;

    @NotNull(message = "Instrument ID is required")
    private Integer instrumentId;

    @NotNull(message = "Test ID is required")
    private Integer testId;

    private String resolutionStatus;

    private Timestamp resolvedDateTime;

    private Integer resolvedByUserId;

    private String resolutionNotes;

    // For display purposes
    private String instrumentName;
    private String testName;
    private String resolvedByUserName;
    private String ruleDescription;

    public QCViolationForm() {
    }

    public String getId() {
        return id;
    }

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

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getResolvedByUserName() {
        return resolvedByUserName;
    }

    public void setResolvedByUserName(String resolvedByUserName) {
        this.resolvedByUserName = resolvedByUserName;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }

    public void setRuleDescription(String ruleDescription) {
        this.ruleDescription = ruleDescription;
    }

    /**
     * DTO for violation resolution request.
     */
    public static class ResolveRequest {
        @NotBlank(message = "Resolution notes are required")
        private String notes;

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * DTO for violation acknowledgement request.
     */
    public static class AcknowledgeRequest {
        // No additional fields needed - user ID comes from authentication
    }
}
