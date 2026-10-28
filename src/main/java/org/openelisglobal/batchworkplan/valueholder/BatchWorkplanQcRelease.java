package org.openelisglobal.batchworkplan.valueholder;

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
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One control lot checked when a batch workplan was released (OGC-427). The
 * rows are the audit trail of the release: which lot stood behind the batch,
 * what its QC status was at that moment, and, when the release overrode a
 * status other than Pass, the non-conforming event it raised. A PER_RUN lot
 * reads its last release from here. Rows are written once and never updated.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "batch_workplan_qc_release", schema = "clinlims")
public class BatchWorkplanQcRelease extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "batch_workplan_qc_release_generator")
    @SequenceGenerator(name = "batch_workplan_qc_release_generator", sequenceName = "batch_workplan_qc_release_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "batch_workplan_id", nullable = false)
    private Long batchWorkplanId;

    @Column(name = "control_lot_id", length = 36)
    private String controlLotId;

    @Column(name = "test_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "qc_status", nullable = false, length = 30)
    private BatchQcStatus qcStatus;

    @Column(name = "qc_result_id", length = 36)
    private String qcResultId;

    @Column(name = "qc_rule_violation_id", length = 36)
    private String qcRuleViolationId;

    @Column(name = "nce_id")
    private Integer nceId;

    @Column(name = "overridden", nullable = false)
    private boolean overridden;

    @Column(name = "released_at", nullable = false)
    private Timestamp releasedAt;

    @Column(name = "released_by_user_id", nullable = false)
    private Integer releasedByUserId;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatchWorkplanId() {
        return batchWorkplanId;
    }

    public void setBatchWorkplanId(Long batchWorkplanId) {
        this.batchWorkplanId = batchWorkplanId;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public void setControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public BatchQcStatus getQcStatus() {
        return qcStatus;
    }

    public void setQcStatus(BatchQcStatus qcStatus) {
        this.qcStatus = qcStatus;
    }

    public String getQcResultId() {
        return qcResultId;
    }

    public void setQcResultId(String qcResultId) {
        this.qcResultId = qcResultId;
    }

    public String getQcRuleViolationId() {
        return qcRuleViolationId;
    }

    public void setQcRuleViolationId(String qcRuleViolationId) {
        this.qcRuleViolationId = qcRuleViolationId;
    }

    public Integer getNceId() {
        return nceId;
    }

    public void setNceId(Integer nceId) {
        this.nceId = nceId;
    }

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(boolean overridden) {
        this.overridden = overridden;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Integer getReleasedByUserId() {
        return releasedByUserId;
    }

    public void setReleasedByUserId(Integer releasedByUserId) {
        this.releasedByUserId = releasedByUserId;
    }
}
