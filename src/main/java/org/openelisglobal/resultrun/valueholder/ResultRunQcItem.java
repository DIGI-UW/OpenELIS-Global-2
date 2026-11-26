package org.openelisglobal.resultrun.valueholder;

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
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.qc.service.QcItemRules;

/**
 * A lab blank or lab duplicate recorded on a run for one test (OGC-1200 FR-C10,
 * FR-C11), with what it came to. A duplicate is a second measurement of a
 * sample in the run, its parent; it is evaluated against the parent's result in
 * the run, and again when that result is saved.
 */
@Entity
@Access(AccessType.FIELD)
@Table(name = "result_run_qc_item", schema = "clinlims")
public class ResultRunQcItem extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "result_run_qc_item_generator")
    @SequenceGenerator(name = "result_run_qc_item_generator", sequenceName = "result_run_qc_item_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "test_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 10)
    private RunQcItemKind kind;

    @Column(name = "source", nullable = false, length = 10)
    private String source = "MANUAL";

    @Column(name = "observed_value", precision = 15, scale = 5)
    private BigDecimal observedValue;

    @Column(name = "observed_outcome", length = 40)
    private String observedOutcome;

    @Column(name = "parent_analysis_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String parentAnalysisId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation", nullable = false, length = 15)
    private QcItemRules.Evaluation evaluation;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "recorded_by", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Timestamp recordedAt;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public RunQcItemKind getKind() {
        return kind;
    }

    public void setKind(RunQcItemKind kind) {
        this.kind = kind;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getObservedValue() {
        return observedValue;
    }

    public void setObservedValue(BigDecimal observedValue) {
        this.observedValue = observedValue;
    }

    public String getObservedOutcome() {
        return observedOutcome;
    }

    public void setObservedOutcome(String observedOutcome) {
        this.observedOutcome = observedOutcome;
    }

    public String getParentAnalysisId() {
        return parentAnalysisId;
    }

    public void setParentAnalysisId(String parentAnalysisId) {
        this.parentAnalysisId = parentAnalysisId;
    }

    public QcItemRules.Evaluation getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(QcItemRules.Evaluation evaluation) {
        this.evaluation = evaluation;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Timestamp getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Timestamp recordedAt) {
        this.recordedAt = recordedAt;
    }
}
