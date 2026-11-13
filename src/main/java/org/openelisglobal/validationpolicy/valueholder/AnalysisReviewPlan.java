package org.openelisglobal.validationpolicy.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OGC-1364 v2: the reviews one analysis needs, copied from its lab unit's
 * policy when its first level is signed, so a later policy change does not
 * alter a review already under way.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "analysis_review_plan", schema = "clinlims")
public class AnalysisReviewPlan extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "analysis_id")
    private Integer analysisId;

    @Column(name = "review_levels", nullable = false)
    private int reviewLevels;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_review_plan_level", schema = "clinlims", joinColumns = @JoinColumn(name = "analysis_id"))
    @MapKeyColumn(name = "level")
    @Column(name = "role_id")
    private Map<Integer, Integer> levelRoleIds = new HashMap<>();

    @Override
    public Integer getId() {
        return analysisId;
    }

    @Override
    public void setId(Integer analysisId) {
        this.analysisId = analysisId;
    }

    public int getReviewLevels() {
        return reviewLevels;
    }

    public void setReviewLevels(int reviewLevels) {
        this.reviewLevels = reviewLevels;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Map<Integer, Integer> getLevelRoleIds() {
        return levelRoleIds;
    }
}
