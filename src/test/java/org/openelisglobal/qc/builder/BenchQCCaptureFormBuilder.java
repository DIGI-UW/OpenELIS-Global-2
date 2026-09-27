package org.openelisglobal.qc.builder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCSource;

/**
 * Fluent construction for {@link BenchQCCaptureForm}, which is a record: its
 * canonical constructor takes twelve positional arguments, so a literal call
 * reads as an unlabelled argument list and a test that cares about three fields
 * still has to spell out nine nulls. Five bench QC test classes build captures,
 * which is why this sits beside {@link QCControlLotBuilder} rather than being
 * copied into each.
 */
public class BenchQCCaptureFormBuilder {

    private final QCSource source;
    private String testId;
    private String testSectionId;
    private String controlLotId;
    private String controlLabel;
    private BigDecimal resultValue;
    private String unitOfMeasure;
    private QCQualitativeOutcome qualitativeOutcome;
    private BigDecimal expectedValue;
    private BigDecimal uncertainty;
    private LocalDateTime runDateTime;
    private String notes;

    private BenchQCCaptureFormBuilder(QCSource source) {
        this.source = source;
    }

    /**
     * MANUAL for a quantitative run, RDT for a control line. Any other source is
     * one the bench path must refuse, which some cases here deliberately post.
     */
    public static BenchQCCaptureFormBuilder create(QCSource source) {
        return new BenchQCCaptureFormBuilder(source);
    }

    public BenchQCCaptureFormBuilder withTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public BenchQCCaptureFormBuilder withTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
        return this;
    }

    public BenchQCCaptureFormBuilder withControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
        return this;
    }

    public BenchQCCaptureFormBuilder withControlLabel(String controlLabel) {
        this.controlLabel = controlLabel;
        return this;
    }

    public BenchQCCaptureFormBuilder withResultValue(BigDecimal resultValue) {
        this.resultValue = resultValue;
        return this;
    }

    public BenchQCCaptureFormBuilder withUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
        return this;
    }

    public BenchQCCaptureFormBuilder withOutcome(QCQualitativeOutcome qualitativeOutcome) {
        this.qualitativeOutcome = qualitativeOutcome;
        return this;
    }

    /** The target snapshotted onto the run, with its tolerance. */
    public BenchQCCaptureFormBuilder withTarget(BigDecimal expectedValue, BigDecimal uncertainty) {
        this.expectedValue = expectedValue;
        this.uncertainty = uncertainty;
        return this;
    }

    public BenchQCCaptureFormBuilder withRunDateTime(LocalDateTime runDateTime) {
        this.runDateTime = runDateTime;
        return this;
    }

    /** Most bench fixtures state their run times as {@code yyyy-MM-dd HH:mm:ss}. */
    public BenchQCCaptureFormBuilder withRunDateTime(String timestamp) {
        return withRunDateTime(Timestamp.valueOf(timestamp).toLocalDateTime());
    }

    public BenchQCCaptureFormBuilder withNotes(String notes) {
        this.notes = notes;
        return this;
    }

    public BenchQCCaptureForm build() {
        return new BenchQCCaptureForm(source, testId, testSectionId, controlLotId, controlLabel, resultValue,
                unitOfMeasure, qualitativeOutcome, expectedValue, uncertainty, runDateTime, notes);
    }
}
