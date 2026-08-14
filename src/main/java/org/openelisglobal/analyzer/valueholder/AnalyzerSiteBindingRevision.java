package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_site_binding_revision", schema = "clinlims")
public class AnalyzerSiteBindingRevision extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_id", nullable = false, updatable = false)
    private AnalyzerSiteBinding siteBinding;

    @Column(name = "revision_number", nullable = false, updatable = false)
    private Integer revisionNumber;

    @Column(name = "bridge_profile_id", length = 128, nullable = false, updatable = false)
    private String bridgeProfileId;

    @Column(name = "bridge_profile_revision", nullable = false, updatable = false)
    private Integer bridgeProfileRevision;

    @Column(name = "fingerprint", length = 71, nullable = false, updatable = false)
    private String fingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_revision_id", updatable = false)
    private AnalyzerSiteBindingRevision supersedesRevision;

    @Column(name = "created_by", length = 50, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    public AnalyzerSiteBindingRevision() {
        id = UUID.randomUUID().toString();
    }

    @PrePersist
    protected void prepareForInsert() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Timestamp.from(Instant.now());
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

    public AnalyzerSiteBinding getSiteBinding() {
        return siteBinding;
    }

    public void setSiteBinding(AnalyzerSiteBinding siteBinding) {
        this.siteBinding = siteBinding;
    }

    public Integer getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(Integer revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public String getBridgeProfileId() {
        return bridgeProfileId;
    }

    public void setBridgeProfileId(String bridgeProfileId) {
        this.bridgeProfileId = bridgeProfileId;
    }

    public Integer getBridgeProfileRevision() {
        return bridgeProfileRevision;
    }

    public void setBridgeProfileRevision(Integer bridgeProfileRevision) {
        this.bridgeProfileRevision = bridgeProfileRevision;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public AnalyzerSiteBindingRevision getSupersedesRevision() {
        return supersedesRevision;
    }

    public void setSupersedesRevision(AnalyzerSiteBindingRevision supersedesRevision) {
        this.supersedesRevision = supersedesRevision;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
