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
 * itself belongs to OGC-1025 (Results Entry).
 *
 * <p>
 * Note {@code controlLotId} is an identifier, not a lot number typed by the
 * tech: a free-text lot field would accumulate lot strings that match no
 * {@code qc_control_lot} row and cannot carry the fixed mean/SD a
 * Levey-Jennings plot needs. Lots are selected, never invented here — the same
 * contract OGC-1148 specifies for the target editor.
 *
 * @param source             where the run came from; only the bench-entered
 *                           sources are accepted here
 * @param testId             the test the control was run for
 * @param testSectionId      lab unit the run happened in; scopes the QC-fail
 *                           signal
 * @param controlLotId       existing control lot. Required for manual
 *                           quantitative runs — that is where the expected
 *                           mean/SD lives — and optional for RDT, where the
 *                           cassette is identified by {@code controlLabel}
 * @param controlLabel       kit or control designation, for RDT runs with no
 *                           levelled control material
 * @param resultValue        measured value. Required for MANUAL, must be absent
 *                           for RDT
 * @param unitOfMeasure      unit the value was read in; falls back to the test
 *                           definition's unit when omitted
 * @param qualitativeOutcome VALID/INVALID for RDT, PASS/FAIL for MANUAL
 * @param expectedValue      the expected value in force at capture, snapshotted
 *                           onto the result so a later edit to a configured
 *                           target never rewrites QC history. Tech-entered
 *                           today; prefilled from OGC-1148's targets once those
 *                           ship
 * @param uncertainty        the tolerance around that expected value, captured
 *                           for the same reason
 * @param runDateTime        when the control was run. Defaults to now if the
 *                           client omits it
 * @param notes              free-text note the tech left with the run
 */
public record BenchQCCaptureForm(@NotNull QCSource source, @NotNull String testId, @NotNull String testSectionId,
        String controlLotId, String controlLabel, BigDecimal resultValue, String unitOfMeasure,
        @NotNull QCQualitativeOutcome qualitativeOutcome, BigDecimal expectedValue, BigDecimal uncertainty,
        LocalDateTime runDateTime, String notes) {
}
