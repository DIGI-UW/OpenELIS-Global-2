package org.openelisglobal.validationpolicy.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1364 v2: one person's signature on one review level of an analysis. A
 * modification, retest or rejection supersedes the signatures of the current
 * pass; they stay as evidence and review restarts at level 1.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "analysis_review_sign_off", schema = "clinlims")
public class AnalysisReviewSignOff extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public enum SupersededReason {
        MODIFIED, RETEST, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysis_review_sign_off_generator")
    @SequenceGenerator(name = "analysis_review_sign_off_generator", sequenceName = "analysis_review_sign_off_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Integer analysisId;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "review_levels", nullable = false)
    private int reviewLevels;

    @Column(name = "signer_id", nullable = false)
    private Integer signerId;

    @Column(name = "role_id", nullable = false)
    private Integer roleId;

    @Column(name = "signed_at", nullable = false)
    private OffsetDateTime signedAt;

    @Column(name = "electronic_signature_id")
    private Long electronicSignatureId;

    @Column(name = "superseded_at")
    private OffsetDateTime supersededAt;

    @Column(name = "superseded_by")
    private Integer supersededBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "superseded_reason", length = 20)
    private SupersededReason supersededReason;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getReviewLevels() {
        return reviewLevels;
    }

    public void setReviewLevels(int reviewLevels) {
        this.reviewLevels = reviewLevels;
    }

    public Integer getSignerId() {
        return signerId;
    }

    public void setSignerId(Integer signerId) {
        this.signerId = signerId;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public OffsetDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(OffsetDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public Long getElectronicSignatureId() {
        return electronicSignatureId;
    }

    public void setElectronicSignatureId(Long electronicSignatureId) {
        this.electronicSignatureId = electronicSignatureId;
    }

    public OffsetDateTime getSupersededAt() {
        return supersededAt;
    }

    public Integer getSupersededBy() {
        return supersededBy;
    }

    public SupersededReason getSupersededReason() {
        return supersededReason;
    }

    public boolean isSuperseded() {
        return supersededAt != null;
    }

    public void supersede(SupersededReason reason, Integer by, OffsetDateTime at) {
        this.supersededReason = reason;
        this.supersededBy = by;
        this.supersededAt = at;
    }
}
