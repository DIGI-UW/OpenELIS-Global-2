package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.sql.Timestamp;
import java.time.Instant;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_site_binding_revision", schema = "clinlims")
@DynamicUpdate
public class AnalyzerSiteBindingRevision extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_site_binding_revision_seq_gen")
    @GenericGenerator(name = "analyzer_site_binding_revision_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_site_binding_revision_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_binding_id", nullable = false, updatable = false)
    private AnalyzerSiteBinding siteBinding;

    @Min(1)
    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "binding_fingerprint", length = 71, nullable = false, updatable = false)
    private String bindingFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_revision_id", updatable = false)
    private AnalyzerSiteBindingRevision supersedesRevision;

    @Column(name = "created_by", length = 36, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @PrePersist
    protected void prepareForInsert() {
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

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public String getBindingFingerprint() {
        return bindingFingerprint;
    }

    public void setBindingFingerprint(String bindingFingerprint) {
        this.bindingFingerprint = bindingFingerprint;
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
