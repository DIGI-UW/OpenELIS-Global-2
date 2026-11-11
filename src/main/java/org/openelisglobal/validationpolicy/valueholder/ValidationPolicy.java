package org.openelisglobal.validationpolicy.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1364: which results of a lab unit wait for review and how many sequential
 * reviews they need, with the role that signs each level. A policy without a
 * lab unit is the lab-wide default every unit without an override follows.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "validation_policy", schema = "clinlims")
public class ValidationPolicy extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "validation_policy_generator")
    @SequenceGenerator(name = "validation_policy_generator", sequenceName = "validation_policy_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "test_section_id")
    private Integer testSectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_trigger", nullable = false, length = 20)
    private ReviewTrigger reviewTrigger;

    @Column(name = "review_levels", nullable = false)
    private int reviewLevels;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sys_user_id")
    private Integer changedBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "validation_policy_level", schema = "clinlims", joinColumns = @JoinColumn(name = "validation_policy_id"))
    @MapKeyColumn(name = "level")
    @Column(name = "role_id")
    private Map<Integer, Integer> levelRoleIds = new HashMap<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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

    public int getReviewLevels() {
        return reviewLevels;
    }

    public void setReviewLevels(int reviewLevels) {
        this.reviewLevels = reviewLevels;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }

    /** The role id that signs each level, keyed by level from 1. */
    public Map<Integer, Integer> getLevelRoleIds() {
        return levelRoleIds;
    }

    public void setLevelRoleIds(Map<Integer, Integer> levelRoleIds) {
        this.levelRoleIds = levelRoleIds;
    }
}
