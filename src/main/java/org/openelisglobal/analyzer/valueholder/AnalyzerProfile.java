package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;

/**
 * Portable profile artifact (Built-in or Site Library entry). Per data-model.md
 * AnalyzerProfile entity.
 */
@Entity
@Table(name = "analyzer_profile")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalyzerProfile extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "profile_meta_id", nullable = false, length = 120)
    private String profileMetaId;

    @Column(name = "profile_meta_version", nullable = false, length = 40)
    private String profileMetaVersion;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "source", nullable = false, length = 20)
    private String source;

    @Column(name = "compat_min_version", length = 40)
    private String compatMinVersion;

    @Column(name = "compat_max_version", length = 40)
    private String compatMaxVersion;

    @Column(name = "is_latest", nullable = false)
    private Boolean isLatest = false;

    @Column(name = "is_mutable", nullable = false)
    private Boolean isMutable = true;

    @Column(name = "profile_json", nullable = false, columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private String profileJson;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @PrePersist
    public void generateIdIfNeeded() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (getLastupdated() == null) {
            setLastupdatedFields();
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

    public String getProfileMetaId() {
        return profileMetaId;
    }

    public void setProfileMetaId(String profileMetaId) {
        this.profileMetaId = profileMetaId;
    }

    public String getProfileMetaVersion() {
        return profileMetaVersion;
    }

    public void setProfileMetaVersion(String profileMetaVersion) {
        this.profileMetaVersion = profileMetaVersion;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCompatMinVersion() {
        return compatMinVersion;
    }

    public void setCompatMinVersion(String compatMinVersion) {
        this.compatMinVersion = compatMinVersion;
    }

    public String getCompatMaxVersion() {
        return compatMaxVersion;
    }

    public void setCompatMaxVersion(String compatMaxVersion) {
        this.compatMaxVersion = compatMaxVersion;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
    }

    public Boolean getIsMutable() {
        return isMutable;
    }

    public void setIsMutable(Boolean isMutable) {
        this.isMutable = isMutable;
    }

    public String getProfileJson() {
        return profileJson;
    }

    public void setProfileJson(String profileJson) {
        this.profileJson = profileJson;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
