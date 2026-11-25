package org.openelisglobal.resultrun.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One sample a Retest decision set aside for its test (OGC-1200 FR-C5), and the
 * later run a user confirmed as its retest. Once every retested sample of a
 * finished run is linked, the run is superseded (FR-A3).
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "result_run_retest", schema = "clinlims")
public class ResultRunRetest extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_run_retest_generator")
    @SequenceGenerator(name = "result_run_retest_generator", sequenceName = "result_run_retest_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "disposition_id", nullable = false)
    private Long dispositionId;

    @Column(name = "analysis_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analysisId;

    @Column(name = "retested_in_run_id")
    private Long retestedInRunId;

    @Column(name = "linked_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String linkedBy;

    @Column(name = "linked_at")
    private Timestamp linkedAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getDispositionId() {
        return dispositionId;
    }

    public void setDispositionId(Long dispositionId) {
        this.dispositionId = dispositionId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Long getRetestedInRunId() {
        return retestedInRunId;
    }

    public void setRetestedInRunId(Long retestedInRunId) {
        this.retestedInRunId = retestedInRunId;
    }

    public String getLinkedBy() {
        return linkedBy;
    }

    public void setLinkedBy(String linkedBy) {
        this.linkedBy = linkedBy;
    }

    public Timestamp getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Timestamp linkedAt) {
        this.linkedAt = linkedAt;
    }
}
