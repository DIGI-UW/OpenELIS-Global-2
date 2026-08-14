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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_profile_migration_anomaly", schema = "clinlims")
public class AnalyzerProfileMigrationAnomaly extends BaseObject<String> {

    public enum Code {
        PROFILE_REF_MISSING, PROFILE_SOURCE_ROW_MISSING, DISTINCT_SOURCE_ROWS_SHARE_NORMALIZED_IDENTITY,
        LOCAL_TEST_NOT_UNIQUE, LOCAL_TEST_INACTIVE_OR_MISSING
    }

    public enum Status {
        OPEN, RESOLVED
    }

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "analyzer_id", nullable = false, updatable = false)
    private Analyzer analyzer;

    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 80, nullable = false, updatable = false)
    private Code code;

    @Column(name = "evidence_key", length = 512, nullable = false, updatable = false)
    private String evidenceKey;

    @Column(name = "legacy_source_key", length = 255, updatable = false)
    private String legacySourceKey;

    @Column(name = "legacy_test_id", precision = 10, scale = 0, updatable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String legacyTestId;

    @Column(name = "detail", length = 1000, nullable = false, updatable = false)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "detected_by", length = 50, nullable = false, updatable = false)
    private String detectedBy;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Timestamp detectedAt;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    public AnalyzerProfileMigrationAnomaly() {
        id = UUID.randomUUID().toString();
        status = Status.OPEN;
    }

    @PrePersist
    protected void prepareForInsert() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (detectedAt == null) {
            detectedAt = Timestamp.from(Instant.now());
        }
        if (status == null) {
            status = Status.OPEN;
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

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public String getEvidenceKey() {
        return evidenceKey;
    }

    public void setEvidenceKey(String evidenceKey) {
        this.evidenceKey = evidenceKey;
    }

    public String getLegacySourceKey() {
        return legacySourceKey;
    }

    public void setLegacySourceKey(String legacySourceKey) {
        this.legacySourceKey = legacySourceKey;
    }

    public String getLegacyTestId() {
        return legacyTestId;
    }

    public void setLegacyTestId(String legacyTestId) {
        this.legacyTestId = legacyTestId;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDetectedBy() {
        return detectedBy;
    }

    public void setDetectedBy(String detectedBy) {
        this.detectedBy = detectedBy;
    }

    public Timestamp getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Timestamp detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
