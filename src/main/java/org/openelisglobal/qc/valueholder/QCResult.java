package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCResult represents a single quality control measurement.
 *
 * <p>
 * Since OGC-1147 this is the single store for all three QC sources (see
 * {@link QCSource}): analyzer-transmitted results, bench quantitative controls,
 * and RDT control lines. That is build-time decision D1(a) — the FRS's default
 * option routed non-analyzer runs through a "shared QcRun table" that has never
 * existed in this codebase. Keeping one store means the Levey-Jennings chart,
 * dashboard, export, rule engine and auto-NCE bridge all keep working
 * unchanged.
 *
 * <p>
 * Consequently {@code controlLotId}, {@code instrumentId} and
 * {@code resultValue} are nullable: a bench run has no analyzer, an RDT
 * cassette has no levelled control lot, and a qualitative outcome has no number
 * (FR-A3). The {@code chk_qc_result_source_shape} database constraint is what
 * keeps the combinations legal — every quantitative row still has a value, so
 * existing readers of {@code resultValue} are unaffected.
 */
@Entity
@Table(name = "qc_result")
public class QCResult extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    // Nullable since OGC-1147: an RDT cassette is identified by controlLabel rather
    // than
    // by a levelled control-material record. Manual quantitative runs should still
    // carry
    // a lot — that is where the fixed mean/SD used for the z-score lives (D3).
    @Column(name = "control_lot_id", length = 36)
    private String controlLotId;

    // testId and instrumentId reference Test.id and Analyzer.id (String,
    // bridged to NUMERIC via LIMSStringNumberUserType). Match that pattern
    // here — per PR #3112 (OGC-346).
    @NotNull
    @Column(name = "test_id", nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testId;

    @Column(name = "instrument_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String instrumentId;

    // Nullable for RDT rows only; the DB CHECK guarantees every quantitative row
    // has one.
    @Column(name = "result_value", precision = 15, scale = 5)
    private BigDecimal resultValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private QCSource source = QCSource.ASTM;

    // D2: qualitative outcomes get their own column — never a magic number in
    // resultValue.
    @Enumerated(EnumType.STRING)
    @Column(name = "qualitative_outcome", length = 10)
    private QCQualitativeOutcome qualitativeOutcome;

    // FR-B2 snapshot of the target in force when this control was captured, so a
    // later
    // edit to a configured target (OGC-1148) can never rewrite QC history.
    @Column(name = "expected_value", precision = 15, scale = 5)
    private BigDecimal expectedValue;

    @Column(name = "uncertainty", precision = 15, scale = 5)
    private BigDecimal uncertainty;

    // Lab unit (test_section). FR-C1 scopes the QC-fail signal by test AND lab
    // unit, and a
    // bench run has no analyzer to scope by instead.
    @Column(name = "test_section_id")
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String testSectionId;

    // Kit or control designation for RDT runs (FR-A3).
    @Column(name = "control_label", length = 120)
    private String controlLabel;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure = "";

    @Column(name = "z_score", precision = 10, scale = 4)
    private BigDecimal zScore;

    @NotNull
    @Column(name = "run_date_time", nullable = false)
    private Timestamp runDateTime;

    @Column(name = "technician_id")
    private Integer technicianId;

    @NotNull
    @Column(name = "result_status", nullable = false, length = 50)
    private String resultStatus = "PENDING";

    @Column(name = "non_conformity_flag")
    private Boolean nonConformityFlag = false;

    @Column(name = "external_notes", columnDefinition = "TEXT")
    private String externalNotes;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public QCResult() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public BigDecimal getResultValue() {
        return resultValue;
    }

    public void setResultValue(BigDecimal resultValue) {
        this.resultValue = resultValue;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getZScore() {
        return zScore;
    }

    public void setZScore(BigDecimal zScore) {
        this.zScore = zScore;
    }

    public Timestamp getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(Timestamp runDateTime) {
        this.runDateTime = runDateTime;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public Boolean getNonConformityFlag() {
        return nonConformityFlag;
    }

    public void setNonConformityFlag(Boolean nonConformityFlag) {
        this.nonConformityFlag = nonConformityFlag;
    }

    public String getExternalNotes() {
        return externalNotes;
    }

    public void setExternalNotes(String externalNotes) {
        this.externalNotes = externalNotes;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }

    public QCSource getSource() {
        return source;
    }

    public void setSource(QCSource source) {
        this.source = source;
    }

    public QCQualitativeOutcome getQualitativeOutcome() {
        return qualitativeOutcome;
    }

    public void setQualitativeOutcome(QCQualitativeOutcome qualitativeOutcome) {
        this.qualitativeOutcome = qualitativeOutcome;
    }

    public BigDecimal getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(BigDecimal expectedValue) {
        this.expectedValue = expectedValue;
    }

    public BigDecimal getUncertainty() {
        return uncertainty;
    }

    public void setUncertainty(BigDecimal uncertainty) {
        this.uncertainty = uncertainty;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getControlLabel() {
        return controlLabel;
    }

    public void setControlLabel(String controlLabel) {
        this.controlLabel = controlLabel;
    }
}
