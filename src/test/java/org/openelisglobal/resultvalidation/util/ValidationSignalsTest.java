package org.openelisglobal.resultvalidation.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;

/**
 * OGC-1027 — the pure "Check before release" rules. Plain JUnit: no Spring, no
 * database, so each rule's edge cases (and the FR-B1 fail-safe posture) are
 * pinned deterministically.
 */
public class ValidationSignalsTest {

    // ---- modified -----------------------------------------------------------

    @Test
    public void isModified_firstSaveIsNotModified() {
        assertFalse(ValidationSignals.isModified("1"));
    }

    @Test
    public void isModified_secondRevisionIsModified() {
        assertTrue(ValidationSignals.isModified("2"));
        assertTrue(ValidationSignals.isModified(" 7 "));
    }

    @Test
    public void isModified_unsavedOrUnparseableIsNotModified() {
        assertFalse(ValidationSignals.isModified("0"));
        assertFalse(ValidationSignals.isModified(null));
        assertFalse(ValidationSignals.isModified(""));
        assertFalse(ValidationSignals.isModified("abc"));
    }

    // ---- critical -----------------------------------------------------------

    private static ResultLimit limit(double lowCritical, double highCritical) {
        ResultLimit limit = new ResultLimit();
        limit.setLowCritical(lowCritical);
        limit.setHighCritical(highCritical);
        return limit;
    }

    private static Result numeric(String value) {
        Result result = new Result();
        result.setResultType("N");
        result.setValue(value);
        return result;
    }

    @Test
    public void isCritical_belowAuthoredLowBound() {
        assertTrue(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("1.5")));
    }

    @Test
    public void isCritical_aboveAuthoredHighBound() {
        assertTrue(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("10.5")));
    }

    @Test
    public void isCritical_withinBoundsIsNotCritical() {
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("5")));
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("2.0")));
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("10.0")));
    }

    @Test
    public void isCritical_unauthoredBoundsNeverFire() {
        // POSITIVE_INFINITY is the "not authored" sentinel the alert engine uses for
        // both bounds; NEGATIVE_INFINITY is what the legacy limit loader uses for the
        // low bound. Neither may ever make a value read as critical.
        ResultLimit none = limit(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertFalse(ValidationSignals.isCritical(none, numeric("-999999")));
        assertFalse(ValidationSignals.isCritical(none, numeric("999999")));
        ResultLimit legacyLow = limit(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        assertFalse(ValidationSignals.isCritical(legacyLow, numeric("-999999")));
    }

    @Test
    public void isCritical_onlyNumericResultsWithAValue() {
        Result coded = new Result();
        coded.setResultType("D");
        coded.setValue("1");
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), coded));
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("")));
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), numeric("not-a-number")));
        assertFalse(ValidationSignals.isCritical(null, numeric("1")));
        assertFalse(ValidationSignals.isCritical(limit(2.0, 10.0), null));
    }

    // ---- NCE open -----------------------------------------------------------

    @Test
    public void isNceOpen_terminalStatusesAreClosed() {
        assertFalse(ValidationSignals.isNceOpen("Closed"));
        assertFalse(ValidationSignals.isNceOpen("Completed"));
    }

    @Test
    public void isNceOpen_workingStatusesAreOpen() {
        assertTrue(ValidationSignals.isNceOpen("Pending"));
        assertTrue(ValidationSignals.isNceOpen("Under Investigation"));
        assertTrue(ValidationSignals.isNceOpen("Corrective Action"));
        assertTrue(ValidationSignals.isNceOpen("CAPA"));
    }

    @Test
    public void isNceOpen_missingStatusIsOpenFailSafe() {
        assertTrue(ValidationSignals.isNceOpen(null));
        assertTrue(ValidationSignals.isNceOpen(""));
        assertTrue(ValidationSignals.isNceOpen("   "));
    }

    // ---- ack pending --------------------------------------------------------

    private static Alert alert(AlertType type, AlertStatus status) {
        Alert alert = new Alert();
        alert.setAlertType(type);
        alert.setStatus(status);
        return alert;
    }

    @Test
    public void hasOpenCriticalAlert_openCriticalAlertIsPending() {
        assertTrue(ValidationSignals
                .hasOpenCriticalAlert(Collections.singletonList(alert(AlertType.CRITICAL_RESULT, AlertStatus.OPEN))));
    }

    @Test
    public void hasOpenCriticalAlert_acknowledgedOrResolvedIsNotPending() {
        assertFalse(ValidationSignals
                .hasOpenCriticalAlert(Arrays.asList(alert(AlertType.CRITICAL_RESULT, AlertStatus.ACKNOWLEDGED),
                        alert(AlertType.CRITICAL_RESULT, AlertStatus.RESOLVED))));
    }

    @Test
    public void hasOpenCriticalAlert_otherOpenAlertTypesDoNotCount() {
        AlertType other = Arrays.stream(AlertType.values()).filter(t -> t != AlertType.CRITICAL_RESULT).findFirst()
                .orElse(AlertType.CRITICAL_RESULT);
        assertFalse(ValidationSignals.hasOpenCriticalAlert(Collections.singletonList(alert(other, AlertStatus.OPEN)))
                && other != AlertType.CRITICAL_RESULT);
    }

    @Test
    public void hasOpenCriticalAlert_noAlertsIsNotPending() {
        assertFalse(ValidationSignals.hasOpenCriticalAlert(null));
        assertFalse(ValidationSignals.hasOpenCriticalAlert(Collections.emptyList()));
    }
}
