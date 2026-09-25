package org.openelisglobal.resultvalidation.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import org.junit.Test;
import org.openelisglobal.alert.valueholder.Alert;
import org.openelisglobal.alert.valueholder.AlertStatus;
import org.openelisglobal.alert.valueholder.AlertType;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

/**
 * OGC-1027 — the pure "Check before release" rules. Plain JUnit: no Spring, no
 * database, so each rule's edge cases (and the FR-B1 fail-safe posture) are
 * pinned deterministically.
 */
public class ValidationSignalsTest {

    // ---- resultFlag (OGC-1121) -----------------------------------------------

    private static ResultLimit authoredLimit() {
        ResultLimit limit = new ResultLimit();
        limit.setId("1");
        limit.setLowValid(0d);
        limit.setHighValid(1000d);
        limit.setLowCritical(2d);
        limit.setHighCritical(150d);
        limit.setLowNormal(5d);
        limit.setHighNormal(100d);
        return limit;
    }

    @Test
    public void resultFlag_invalidBeatsCriticalBeatsAbnormalBeatsNormal() {
        ResultLimit limit = authoredLimit();
        assertEquals("INVALID", ValidationSignals.resultFlag(limit, "N", "2000"));
        assertEquals("CRITICAL", ValidationSignals.resultFlag(limit, "N", "200"));
        assertEquals("CRITICAL", ValidationSignals.resultFlag(limit, "N", "1"));
        assertEquals("ABNORMAL", ValidationSignals.resultFlag(limit, "N", "120"));
        assertEquals("ABNORMAL", ValidationSignals.resultFlag(limit, "N", "3"));
        assertEquals("NORMAL", ValidationSignals.resultFlag(limit, "N", "50"));
    }

    @Test
    public void resultFlag_unauthoredCriticalBoundsNeverFire() {
        ResultLimit limit = authoredLimit();
        limit.setLowCritical(Double.POSITIVE_INFINITY);
        limit.setHighCritical(Double.POSITIVE_INFINITY);
        assertEquals("ABNORMAL", ValidationSignals.resultFlag(limit, "N", "200"));
        limit.setLowCritical(Double.NEGATIVE_INFINITY);
        assertEquals("ABNORMAL", ValidationSignals.resultFlag(limit, "N", "1"));
    }

    @Test
    public void resultFlag_isNullWhenThereIsNothingToJudge() {
        ResultLimit limit = authoredLimit();
        assertNull("non-numeric type", ValidationSignals.resultFlag(limit, "D", "50"));
        assertNull("blank value", ValidationSignals.resultFlag(limit, "N", " "));
        assertNull("unparseable value", ValidationSignals.resultFlag(limit, "N", "abc"));
        assertNull("no limit", ValidationSignals.resultFlag(null, "N", "50"));
        limit.setId(null);
        assertNull("the selector's synthetic empty limit", ValidationSignals.resultFlag(limit, "N", "50"));
    }

    @Test
    public void authoredBound_isNullForTheInfinitySentinels() {
        assertNull(ValidationSignals.authoredBound(Double.POSITIVE_INFINITY));
        assertNull(ValidationSignals.authoredBound(Double.NEGATIVE_INFINITY));
        assertEquals(Double.valueOf(2d), ValidationSignals.authoredBound(2d));
    }

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

    // ---- entered by (OGC-1028) ----------------------------------------------

    private static ResultSignature signature(String name, boolean supervisor) {
        ResultSignature signature = new ResultSignature();
        signature.setNonUserName(name);
        signature.setIsSupervisor(supervisor);
        return signature;
    }

    @Test
    public void enteredBy_usesTheBenchSignatureAndIgnoresSupervisors() {
        assertEquals("Tech One", ValidationSignals
                .enteredBy(Arrays.asList(signature("Tech One", false), signature("Supervisor", true))));
    }

    @Test
    public void enteredBy_lastBenchSignatureWins() {
        assertEquals("Tech Two",
                ValidationSignals.enteredBy(Arrays.asList(signature("Tech One", false), signature("Tech Two", false))));
    }

    @Test
    public void enteredBy_blankWhenNoBenchSignature() {
        assertEquals("", ValidationSignals.enteredBy(null));
        assertEquals("", ValidationSignals.enteredBy(Collections.emptyList()));
        assertEquals("", ValidationSignals.enteredBy(Collections.singletonList(signature("Supervisor", true))));
        assertEquals("", ValidationSignals.enteredBy(Collections.singletonList(signature("", false))));
    }

    // ---- Clear lane, server-side (OGC-1029 FR-B1, OGC-1226 FR-1 to FR-4) ------

    private static AnalysisItem clearRow() {
        AnalysisItem row = new AnalysisItem();
        row.setNormalRange("10 - 20");
        row.setNormal(true);
        row.setQcStatus(ValidationSignals.QC_UNKNOWN);
        row.setNceOpen(false);
        row.setModified(false);
        row.setCritical(false);
        row.setNonconforming(false);
        row.setAckPending(false);
        return row;
    }

    private static AnalysisItem rowWith(Consumer<AnalysisItem> change) {
        AnalysisItem row = clearRow();
        change.accept(row);
        return row;
    }

    @Test
    public void isClear_whenEveryClearanceInputIsAffirmativelyClean() {
        assertTrue(ValidationSignals.isClear(clearRow()));
        assertTrue(ValidationSignals.isClear(rowWith(row -> row.setQcStatus(ValidationSignals.QC_PASS))));
    }

    @Test
    public void isClear_anAbsentQcVerdictIsNotAnInput() {
        assertTrue("a patient result never carries a QC evaluation and must still clear",
                ValidationSignals.isClear(rowWith(row -> row.setQcStatus(ValidationSignals.QC_UNKNOWN))));
        assertTrue(ValidationSignals.isClear(rowWith(row -> row.setQcStatus(null))));
        assertFalse("a recorded failure still stops the row",
                ValidationSignals.isClear(rowWith(row -> row.setQcStatus(ValidationSignals.QC_FAIL))));
    }

    @Test
    public void isClear_anySignalExcludesTheRow() {
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setNormal(false))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setNceOpen(true))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setModified(true))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setCritical(true))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setNonconforming(true))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setAckPending(true))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setQcStatus(ValidationSignals.QC_FAIL))));
    }

    @Test
    public void isClear_failSafeOnIndeterminateInputsTheRowGenuinelyHas() {
        assertFalse("no reference range means no in-range verdict",
                ValidationSignals.isClear(rowWith(row -> row.setNormalRange(""))));
        assertFalse(ValidationSignals.isClear(rowWith(row -> row.setNormalRange(null))));
        assertFalse(ValidationSignals.isClear(null));
    }

    // ---- the same rule at result entry (OGC-1226 FR-7) ------------------------

    @Test
    public void isClearAtEntry_numericInsideAnAuthoredNormalRange() {
        assertTrue(ValidationSignals.isClearAtEntry(authoredLimit(), "N", "50", ValidationSignals.QC_UNKNOWN, false,
                false, false));
        assertTrue(ValidationSignals.isClearAtEntry(authoredLimit(), "N", " 5 ", null, false, false, false));
    }

    @Test
    public void isClearAtEntry_abnormalCriticalOrInvalidNumericIsNotClear() {
        assertFalse("abnormal", ValidationSignals.isClearAtEntry(authoredLimit(), "N", "120",
                ValidationSignals.QC_UNKNOWN, false, false, false));
        assertFalse("critical", ValidationSignals.isClearAtEntry(authoredLimit(), "N", "1",
                ValidationSignals.QC_UNKNOWN, false, false, false));
        assertFalse("invalid", ValidationSignals.isClearAtEntry(authoredLimit(), "N", "5000",
                ValidationSignals.QC_UNKNOWN, false, false, false));
        assertFalse("not a number", ValidationSignals.isClearAtEntry(authoredLimit(), "N", "high",
                ValidationSignals.QC_UNKNOWN, false, false, false));
    }

    @Test
    public void isClearAtEntry_noRangeToJudgeAgainstIsNotClear() {
        assertFalse("no limit", ValidationSignals.isClearAtEntry(null, "N", "50", null, false, false, false));
        ResultLimit unauthored = authoredLimit();
        unauthored.setLowNormal(Double.NEGATIVE_INFINITY);
        unauthored.setHighNormal(Double.POSITIVE_INFINITY);
        assertFalse("numeric limit with no authored normal bounds",
                ValidationSignals.isClearAtEntry(unauthored, "N", "50", null, false, false, false));
        assertFalse("free text has no range",
                ValidationSignals.isClearAtEntry(authoredLimit(), "A", "negative", null, false, false, false));
        assertFalse("a select list with no expected-normal choice",
                ValidationSignals.isClearAtEntry(authoredLimit(), "D", "12", null, false, false, false));
    }

    @Test
    public void isClearAtEntry_selectListClearsOnlyOnTheExpectedNormalChoice() {
        ResultLimit limit = authoredLimit();
        limit.setDictionaryNormalId("12");
        assertTrue(ValidationSignals.isClearAtEntry(limit, "D", "12", null, false, false, false));
        assertFalse(ValidationSignals.isClearAtEntry(limit, "D", "13", null, false, false, false));
    }

    @Test
    public void isClearAtEntry_anyRaisedSignalHoldsTheResultForAValidator() {
        assertFalse("recorded QC failure", ValidationSignals.isClearAtEntry(authoredLimit(), "N", "50",
                ValidationSignals.QC_FAIL, false, false, false));
        assertFalse("open non-conformity",
                ValidationSignals.isClearAtEntry(authoredLimit(), "N", "50", null, true, false, false));
        assertFalse("modified after first save",
                ValidationSignals.isClearAtEntry(authoredLimit(), "N", "50", null, false, true, false));
        assertFalse("nonconforming",
                ValidationSignals.isClearAtEntry(authoredLimit(), "N", "50", null, false, false, true));
    }

    @Test
    public void allClear_requiresEveryComponentRowToBeClear() {
        assertTrue(ValidationSignals.allClear(Arrays.asList(clearRow(), clearRow())));
        assertFalse("one abnormal component pushes the whole analysis to Needs-review",
                ValidationSignals.allClear(Arrays.asList(clearRow(), rowWith(row -> row.setNormal(false)))));
        assertFalse(ValidationSignals.allClear(Collections.emptyList()));
        assertFalse(ValidationSignals.allClear(null));
    }

    // ---- stale-page guard (OGC-1030, FR-J1) ----------------------------------

    @Test
    public void isStale_whenTheAnalysisMovedSinceTheRowWasServed() {
        java.sql.Timestamp now = new java.sql.Timestamp(1_700_000_000_000L);
        assertTrue(ValidationSignals.isStale("1699999999000", now));
        assertFalse(ValidationSignals.isStale("1700000000000", now));
        assertFalse(ValidationSignals.isStale(" 1700000000000 ", now));
    }

    @Test
    public void isStale_legacyRowsWithoutATokenAreNotChecked() {
        java.sql.Timestamp now = new java.sql.Timestamp(1_700_000_000_000L);
        assertFalse(ValidationSignals.isStale(null, now));
        assertFalse(ValidationSignals.isStale("", now));
        assertFalse(ValidationSignals.isStale("1700000000000", null));
    }

    @Test
    public void isStale_anUnreadableTokenIsTreatedAsStale() {
        assertTrue(ValidationSignals.isStale("not-a-number", new java.sql.Timestamp(1_700_000_000_000L)));
    }

    // ---- next revision after a validator's modification (OGC-1028, FR-D4) ----

    @Test
    public void nextRevision_incrementsASavedRevision() {
        assertEquals("2", ValidationSignals.nextRevision("1"));
        assertEquals("4", ValidationSignals.nextRevision(" 3 "));
    }

    @Test
    public void nextRevision_alwaysReadsAsModifiedEvenForLegacyAnalyses() {
        assertEquals("2", ValidationSignals.nextRevision("0"));
        assertEquals("2", ValidationSignals.nextRevision(null));
        assertEquals("2", ValidationSignals.nextRevision(""));
        assertEquals("2", ValidationSignals.nextRevision("abc"));
        assertTrue(ValidationSignals.isModified(ValidationSignals.nextRevision(null)));
        assertTrue(ValidationSignals.isModified(ValidationSignals.nextRevision("0")));
    }

    // ---- component of a row (OGC-1028, FR-C4) -------------------------------

    private static TestResultComponent component(String id, String label, int order) {
        TestResultComponent component = new TestResultComponent();
        component.setId(id);
        component.setLabel(label);
        component.setDisplayOrder(order);
        return component;
    }

    @Test
    public void componentOf_matchesTheRowsComponentById() {
        TestResultComponent match = ValidationSignals
                .componentOf(Arrays.asList(component("1", "Ct N2", 2), component("2", "Ct E", 3)), "2");
        assertEquals("Ct E", match.getLabel());
        assertEquals(Integer.valueOf(3), match.getDisplayOrder());
    }

    @Test
    public void componentOf_nullForLegacySingleComponentRows() {
        assertNull(ValidationSignals.componentOf(Arrays.asList(component("1", "Ct N2", 2)), null));
        assertNull(ValidationSignals.componentOf(Arrays.asList(component("1", "Ct N2", 2)), ""));
        assertNull(ValidationSignals.componentOf(Arrays.asList(component("1", "Ct N2", 2)), "99"));
        assertNull(ValidationSignals.componentOf(null, "1"));
    }
}
