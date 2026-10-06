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

/**
 * One analyzer's mapping of a pinned profile revision to the local catalog, as
 * an append-only revision. The analyzer's current revision is the one in force;
 * a newer revision is a draft until it is confirmed and applied.
 */
@Entity
@Table(name = "analyzer_mapping", schema = "clinlims")
@DynamicUpdate
public class AnalyzerMapping extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_mapping_seq_gen")
    @GenericGenerator(name = "analyzer_mapping_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_mapping_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analyzer_id", nullable = false, updatable = false)
    private Analyzer analyzer;

    @Min(1)
    @Column(name = "revision_number", nullable = false, updatable = false)
    private int revisionNumber;

    @Column(name = "profile_id", length = 128, nullable = false, updatable = false)
    private String profileId;

    @Min(1)
    @Column(name = "profile_revision", nullable = false, updatable = false)
    private int profileRevision;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "profile_fingerprint", length = 71, nullable = false, updatable = false)
    private String profileFingerprint;

    @Pattern(regexp = "^sha256:[0-9a-f]{64}$")
    @Column(name = "mapping_fingerprint", length = 71, nullable = false, updatable = false)
    private String mappingFingerprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_mapping_id", updatable = false)
    private AnalyzerMapping supersedes;

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

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public int getProfileRevision() {
        return profileRevision;
    }

    public void setProfileRevision(int profileRevision) {
        this.profileRevision = profileRevision;
    }

    public String getProfileFingerprint() {
        return profileFingerprint;
    }

    public void setProfileFingerprint(String profileFingerprint) {
        this.profileFingerprint = profileFingerprint;
    }

    public AnalyzerProfilePin getProfilePin() {
        return new AnalyzerProfilePin(profileId, profileRevision, profileFingerprint);
    }

    public String getMappingFingerprint() {
        return mappingFingerprint;
    }

    public void setMappingFingerprint(String mappingFingerprint) {
        this.mappingFingerprint = mappingFingerprint;
    }

    public AnalyzerMapping getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(AnalyzerMapping supersedes) {
        this.supersedes = supersedes;
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
