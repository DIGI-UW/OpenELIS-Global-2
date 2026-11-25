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
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1017: a result released at entry without a validator. It records who
 * saved the result and the rule that released it, as it stood at that moment:
 * the validation policy in force (lab-wide default or the lab unit's override,
 * its trigger, number of reviews and version), or the "validate all results"
 * setting while the validation policy is off.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "analysis_auto_release", schema = "clinlims")
public class AnalysisAutoRelease extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    /**
     * NO_REVIEW: the policy asks for no review. CLEAR: the result was clear under a
     * policy, or the setting, that reviews only results that need it.
     */
    public enum Rule {
        NO_REVIEW, CLEAR
    }

    /**
     * LAB_WIDE: the lab-wide default. LAB_UNIT: the lab unit's override. SETTING:
     * the validation policy is off and "validate all results" decided.
     */
    public enum Scope {
        LAB_WIDE, LAB_UNIT, SETTING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analysis_auto_release_generator")
    @SequenceGenerator(name = "analysis_auto_release_generator", sequenceName = "analysis_auto_release_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "analysis_id", nullable = false)
    private Integer analysisId;

    @Column(name = "released_at", nullable = false)
    private OffsetDateTime releasedAt;

    @Column(name = "released_by")
    private Integer releasedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_rule", nullable = false, length = 20)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_scope", nullable = false, length = 20)
    private Scope scope;

    @Column(name = "test_section_id")
    private Integer testSectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_trigger", length = 20)
    private ReviewTrigger reviewTrigger;

    @Column(name = "review_levels")
    private Integer reviewLevels;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_version")
    private Timestamp policyVersion;

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

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Integer getReleasedBy() {
        return releasedBy;
    }

    public void setReleasedBy(Integer releasedBy) {
        this.releasedBy = releasedBy;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Integer getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(Integer testSectionId) {
        this.testSectionId = testSectionId;
    }

    public ReviewTrigger getReviewTrigger() {
        return reviewTrigger;
    }

    public void setReviewTrigger(ReviewTrigger reviewTrigger) {
        this.reviewTrigger = reviewTrigger;
    }

    public Integer getReviewLevels() {
        return reviewLevels;
    }

    public void setReviewLevels(Integer reviewLevels) {
        this.reviewLevels = reviewLevels;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Timestamp getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(Timestamp policyVersion) {
        this.policyVersion = policyVersion;
    }
}
