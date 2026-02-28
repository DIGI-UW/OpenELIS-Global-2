package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Snapshot provenance when profile is applied to analyzer. Per data-model.md
 * AnalyzerProfileApplication entity.
 */
@Entity
@Table(name = "analyzer_profile_application")
public class AnalyzerProfileApplication extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analyzer_id", nullable = false)
    private Integer analyzerId;

    @Column(name = "source_profile_id", nullable = false, length = 36)
    private String sourceProfileId;

    @Column(name = "source_profile_meta_id", nullable = false, length = 120)
    private String sourceProfileMetaId;

    @Column(name = "source_profile_version", nullable = false, length = 40)
    private String sourceProfileVersion;

    @Column(name = "applied_at", nullable = false)
    private Timestamp appliedAt;

    @Column(name = "applied_by", length = 36)
    private String appliedBy;

    @PrePersist
    public void generateIdIfNeeded() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (appliedAt == null) {
            appliedAt = new Timestamp(System.currentTimeMillis());
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

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getSourceProfileId() {
        return sourceProfileId;
    }

    public void setSourceProfileId(String sourceProfileId) {
        this.sourceProfileId = sourceProfileId;
    }

    public String getSourceProfileMetaId() {
        return sourceProfileMetaId;
    }

    public void setSourceProfileMetaId(String sourceProfileMetaId) {
        this.sourceProfileMetaId = sourceProfileMetaId;
    }

    public String getSourceProfileVersion() {
        return sourceProfileVersion;
    }

    public void setSourceProfileVersion(String sourceProfileVersion) {
        this.sourceProfileVersion = sourceProfileVersion;
    }

    public Timestamp getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Timestamp appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(String appliedBy) {
        this.appliedBy = appliedBy;
    }
}
