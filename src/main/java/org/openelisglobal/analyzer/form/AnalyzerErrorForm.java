package org.openelisglobal.analyzer.form;

import java.util.Date;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;

/**
 * Form DTO for AnalyzerError operations
 * 
 * 
 * Used for REST API request/response mapping and validation.
 */
public class AnalyzerErrorForm {

    private String id;
    private String analyzerId;
    private String analyzerName;
    private String errorType;
    private String severity;
    private String errorMessage;
    private String rawMessage;
    private String status;
    private String acknowledgedBy;
    private Date acknowledgedAt;
    private Date resolvedAt;
    private Date timestamp;
    private List<String> errorIds; // For batch operations

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerName() {
        return analyzerName;
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Date getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Date acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Date getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Date resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getErrorIds() {
        return errorIds;
    }

    public void setErrorIds(List<String> errorIds) {
        this.errorIds = errorIds;
    }

    /**
     * Convert AnalyzerError entity to Form DTO
     */
    public static AnalyzerErrorForm fromEntity(AnalyzerError error) {
        AnalyzerErrorForm form = new AnalyzerErrorForm();
        form.setId(error.getId());
        if (error.getAnalyzer() != null) {
            form.setAnalyzerId(error.getAnalyzer().getId());
            form.setAnalyzerName(error.getAnalyzer().getName());
        }
        form.setErrorType(error.getErrorType() != null ? error.getErrorType().name() : null);
        form.setSeverity(error.getSeverity() != null ? error.getSeverity().name() : null);
        form.setErrorMessage(error.getErrorMessage());
        form.setRawMessage(error.getRawMessage());
        form.setStatus(error.getStatus() != null ? error.getStatus().name() : null);
        form.setAcknowledgedBy(error.getAcknowledgedBy());
        if (error.getAcknowledgedAt() != null) {
            form.setAcknowledgedAt(new Date(error.getAcknowledgedAt().getTime()));
        }
        if (error.getResolvedAt() != null) {
            form.setResolvedAt(new Date(error.getResolvedAt().getTime()));
        }
        if (error.getLastupdated() != null) {
            form.setTimestamp(new Date(error.getLastupdated().getTime()));
        }
        return form;
    }
}
