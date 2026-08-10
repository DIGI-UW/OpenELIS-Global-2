package org.openelisglobal.qc.form;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCSource;

/**
 * OGC-1147 — one bench control run as captured at Results Entry: an RDT control
 * line or a manual quantitative control.
 *
 * <p>
 * Serves both as the {@code POST /rest/qc/results} request body and as the
 * service argument, so the capture contract is stated once. The capture UI
 * itself belongs to OGC-1025 (Results Entry FR-D3/D4).
 *
 * <p>
 * Note {@code controlLotId} is an identifier, not a lot number typed by the
 * tech: the Results Entry v4 mockup shows a free-text lot field, which would
 * accumulate lot strings that match no {@code qc_control_lot} row and cannot
 * carry the fixed mean/SD a Levey-Jennings plot needs. Lots are selected, never
 * invented here — the same contract OGC-1148 specifies for the target editor.
 */
public class BenchQCCaptureForm {

    @NotNull
    private QCSource source;

    @NotNull
    private String testId;

    /** Lab unit the run happened in. Scopes the QC-fail signal (FR-C1). */
    @NotNull
    private String testSectionId;

    /**
     * Existing control lot. Required for manual quantitative runs — that is where
     * the expected mean/SD lives — and optional for RDT, where the cassette is
     * identified by {@link #controlLabel}.
     */
    private String controlLotId;

    /**
     * Kit or control designation, for RDT runs with no levelled control material.
     */
    private String controlLabel;

    /** Measured value. Required for MANUAL, must be absent for RDT (FR-A3). */
    private BigDecimal resultValue;

    private String unitOfMeasure;

    /** VALID/INVALID for RDT, PASS/FAIL for MANUAL. */
    @NotNull
    private QCQualitativeOutcome qualitativeOutcome;

    /**
     * The expected value and tolerance in force at capture, snapshotted onto the
     * result so a later edit to a configured target never rewrites QC history
     * (FR-B2). Tech-entered today; prefilled from OGC-1148's targets once those
     * ship.
     */
    private BigDecimal expectedValue;

    private BigDecimal uncertainty;

    /** When the control was run. Defaults to now if the client omits it. */
    private LocalDateTime runDateTime;

    private String notes;

    public QCSource getSource() {
        return source;
    }

    public void setSource(QCSource source) {
        this.source = source;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public void setControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
    }

    public String getControlLabel() {
        return controlLabel;
    }

    public void setControlLabel(String controlLabel) {
        this.controlLabel = controlLabel;
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

    public LocalDateTime getRunDateTime() {
        return runDateTime;
    }

    public void setRunDateTime(LocalDateTime runDateTime) {
        this.runDateTime = runDateTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
