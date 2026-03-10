package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerError entity - Stores failed/unmapped analyzer messages for error
 * dashboard and reprocessing workflow.
 */
@Entity
@Table(name = "analyzer_error")
public class AnalyzerError extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false, referencedColumnName = "id")
    private Analyzer analyzer;

    @Column(name = "error_type", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    @Column(name = "severity", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    @NotNull
    private String errorMessage;

    @Column(name = "raw_message", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Enumerated(EnumType.STRING)
    private ErrorStatus status = ErrorStatus.UNACKNOWLEDGED;

    @Column(name = "acknowledged_by", length = 36)
    private String acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Timestamp acknowledgedAt;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
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

    public ErrorStatus getStatus() {
        return status;
    }

    public void setStatus(ErrorStatus status) {
        this.status = status;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Timestamp getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(Timestamp acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public enum ErrorType {
        MAPPING, VALIDATION, TIMEOUT, PROTOCOL, CONNECTION, QC_MAPPING_INCOMPLETE, QC_SERVICE_UNAVAILABLE
    }

    public enum Severity {
        CRITICAL, ERROR, WARNING
    }

    public enum ErrorStatus {
        UNACKNOWLEDGED, ACKNOWLEDGED, RESOLVED
    }
}
