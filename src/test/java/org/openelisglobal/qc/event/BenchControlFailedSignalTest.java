package org.openelisglobal.qc.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qaevent.service.NCEventService;
import org.openelisglobal.qaevent.service.NceSpecimenService;
import org.openelisglobal.qaevent.service.QcViolationNceServiceImpl;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.qaevent.valueholder.NceSpecimen;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1147 FR-C1 — the Validation QC-fail signal for a bench control.
 *
 * <p>
 * Exercises the published path: {@code createBenchQCResult} commits and the
 * {@code @TransactionalEventListener} runs, so these assertions cover the
 * service, the event and the listener together. Only the idempotency case
 * drives the listener directly, to simulate a redelivered event.
 *
 * <p>
 * Dataset: {@code bench-qc-fail-signal.xml}. Lab unit 701 has an in-control
 * manual control at 11:30 and the failing one at 12:00; analyses 9101/9102 fall
 * inside that window, 9103 predates it, and 9104 is in lab unit 702.
 */
public class BenchControlFailedSignalTest extends BaseWebContextSensitiveTest {

    private static final Timestamp LAST_IN_CONTROL = Timestamp.valueOf("2025-06-10 11:30:00");
    private static final Timestamp FAILURE_TIME = Timestamp.valueOf("2025-06-10 12:00:00");
    private static final String TEST_ID = "6601";
    private static final String RDT_TEST_ID = "6602";
    private static final String LAB_UNIT = "701";
    private static final int TECHNICIAN = 7701;

    @Autowired
    private BenchControlFailedEventListener listener;
    @Autowired
    private QCResultService qcResultService;
    @Autowired
    private QCRuleViolationService violationService;
    @Autowired
    private NCEventService ncEventService;
    @Autowired
    private NceSpecimenService nceSpecimenService;
    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/bench-qc-fail-signal.xml");
    }

    @Test
    public void failingManualControl_createsManualFailViolationWithNoAnalyzer() {
        QCResult failing = persistBenchControl(QCSource.MANUAL, QCQualitativeOutcome.FAIL, TEST_ID, "bench-lot-a",
                new BigDecimal("140.00000"), FAILURE_TIME);

        List<QCRuleViolation> violations = violationsForTest(TEST_ID);
        assertEquals("one violation per failing manual control", 1, violations.size());
        QCRuleViolation violation = violations.get(0);
        assertEquals(BenchControlFailedEventListener.MANUAL_FAIL_RULE_CODE, violation.getRuleCode());
        assertEquals("REJECTION", violation.getSeverity());
        assertEquals(failing.getId(), violation.getTriggeringResultId());
        // The reason qc_rule_violation.instrument_id had to become nullable, in schema
        // AND in the entity mapping — the schema alone silently was not enough.
        assertNull("a bench control has no analyzer", violation.getInstrumentId());
    }

    @Test
    public void failingManualControl_holdsOnlyTheAnalysesInItsLabUnitAndWindow() {
        QCResult failing = persistBenchControl(QCSource.MANUAL, QCQualitativeOutcome.FAIL, TEST_ID, "bench-lot-a",
                new BigDecimal("140.00000"), FAILURE_TIME);

        NcEvent nce = onlyQcNce();
        Set<Integer> held = linkedAnalysisIds(nce);
        // 9101 and 9102 completed between the last in-control control and this failure.
        // 9103 completed before it. 9104 is the same test in the same window but a
        // different lab unit — the distinction the whole lab-unit scoping exists for.
        assertEquals(Set.of(9101, 9102), held);
    }

    @Test
    public void invalidRdtControl_raisesTheSignalWithoutAStatisticalViolation() {
        QCResult failing = persistBenchControl(QCSource.RDT, QCQualitativeOutcome.INVALID, RDT_TEST_ID, null, null,
                FAILURE_TIME);

        // Decision D4: an Invalid control line is not a Westgard rule hit, so it must
        // never appear in the statistical record — but it must still raise the signal.
        assertTrue("RDT must not create a statistical violation", violationsForTest(RDT_TEST_ID).isEmpty());

        NcEvent nce = ncEventService.findByTriggerSource(QcViolationNceServiceImpl.TRIGGER_SOURCE_BENCH_CONTROL,
                failing.getId());
        assertNotNull("an Invalid RDT control must still raise an NCE", nce);
        assertEquals("CRITICAL", nce.getSeverity());
        assertEquals(Boolean.TRUE, nce.getAutoGenerated());
        assertNull("no Westgard rule was involved", nce.getWestgardRule());
    }

    @Test
    public void signalIsIdempotentPerControl() {
        QCResult failing = persistBenchControl(QCSource.RDT, QCQualitativeOutcome.INVALID, RDT_TEST_ID, null, null,
                FAILURE_TIME);

        listener.handleBenchControlFailed(new BenchControlFailedEvent(this, failing));

        assertEquals("a redelivered event must not create a second NCE", 1, qcNces().size());
    }

    @Test
    public void heldAnalysesAreReportedToValidationWhileTheNceIsOpen() {
        QCResult failing = persistBenchControl(QCSource.MANUAL, QCQualitativeOutcome.FAIL, TEST_ID, "bench-lot-a",
                new BigDecimal("140.00000"), FAILURE_TIME);

        // What the validation list will actually ask: of these rows, which are held?
        List<Integer> held = nceSpecimenService.findAnalysisIdsWithOpenQcHold(List.of(9101, 9102, 9103, 9104));

        assertEquals(Set.of(9101, 9102), Set.copyOf(held));
    }

    @Test
    public void closingTheNceReleasesTheHold() {
        QCResult failing = persistBenchControl(QCSource.MANUAL, QCQualitativeOutcome.FAIL, TEST_ID, "bench-lot-a",
                new BigDecimal("140.00000"), FAILURE_TIME);

        NcEvent nce = onlyQcNce();
        nce.setStatus("Closed");
        nce.setSysUserId(String.valueOf(TECHNICIAN));
        ncEventService.save(nce);

        assertTrue("a closed QC NCE must stop holding results",
                nceSpecimenService.findAnalysisIdsWithOpenQcHold(List.of(9101, 9102, 9103, 9104)).isEmpty());
    }

    @Test
    public void passingControlIsNotEvenOfferedToTheSignal() {
        // Guards the caller's condition rather than the listener's: a PASS must never
        // reach here, so nothing exists to hold results.
        QCResult passing = persistBenchControl(QCSource.MANUAL, QCQualitativeOutcome.PASS, TEST_ID, "bench-lot-a",
                new BigDecimal("101.00000"), FAILURE_TIME);

        assertFalse(passing.getQualitativeOutcome().isFailing());
        assertTrue(qcNces().isEmpty());
        assertTrue(nceSpecimenService.findAnalysisIdsWithOpenQcHold(List.of(9101, 9102, 9103, 9104)).isEmpty());
    }

    /**
     * The window query on its own, independent of the signal plumbing: analyses of
     * this test in this lab unit, completed inside the window. Isolates a query
     * fault from a wiring fault when the higher-level assertions fail.
     */
    @Test
    public void labUnitWindowQueryFindsOnlyTheRightAnalyses() {
        List<Object[]> rows = analysisService.getAffectedSampleItemIdsByTestSectionAndTestCompletedInRange(LAB_UNIT,
                TEST_ID, LAST_IN_CONTROL, FAILURE_TIME);

        Set<Integer> analysisIds = rows.stream().map(r -> Integer.valueOf(String.valueOf(r[1])))
                .collect(Collectors.toSet());
        assertEquals(Set.of(9101, 9102), analysisIds);
    }

    // ---------------- helpers ----------------

    /**
     * Persist a bench control directly through the service, plus the in-control
     * control at 11:30 that bounds the window for the failing one.
     */
    private QCResult persistBenchControl(QCSource source, QCQualitativeOutcome outcome, String testId,
            String controlLotId, BigDecimal value, Timestamp runAt) {
        if (controlLotId != null) {
            seedInControlPredecessor(testId, controlLotId);
        }
        org.openelisglobal.qc.form.BenchQCCaptureForm capture = new org.openelisglobal.qc.form.BenchQCCaptureForm();
        capture.setSource(source);
        capture.setTestId(testId);
        capture.setTestSectionId(LAB_UNIT);
        capture.setControlLotId(controlLotId);
        capture.setQualitativeOutcome(outcome);
        capture.setResultValue(value);
        capture.setExpectedValue(new BigDecimal("100.00000"));
        capture.setUncertainty(new BigDecimal("5.00000"));
        capture.setRunDateTime(runAt.toLocalDateTime());
        if (source == QCSource.RDT) {
            capture.setControlLabel("Malaria RDT · LOT-BENCH-1");
        }
        return qcResultService.createBenchQCResult(capture, TECHNICIAN);
    }

    /** The in-control control at 11:30 that sets the window's lower bound. */
    private void seedInControlPredecessor(String testId, String controlLotId) {
        org.openelisglobal.qc.form.BenchQCCaptureForm ok = new org.openelisglobal.qc.form.BenchQCCaptureForm();
        ok.setSource(QCSource.MANUAL);
        ok.setTestId(testId);
        ok.setTestSectionId(LAB_UNIT);
        ok.setControlLotId(controlLotId);
        ok.setQualitativeOutcome(QCQualitativeOutcome.PASS);
        ok.setResultValue(new BigDecimal("100.00000"));
        ok.setRunDateTime(LAST_IN_CONTROL.toLocalDateTime());
        qcResultService.createBenchQCResult(ok, TECHNICIAN);
    }

    private List<QCRuleViolation> violationsForTest(String testId) {
        return violationService.findAll().stream().filter(v -> testId.equals(v.getTestId()))
                .collect(Collectors.toList());
    }

    private List<NcEvent> qcNces() {
        return ncEventService.getAll().stream()
                .filter(e -> e.getTriggerSourceType() != null && e.getTriggerSourceType().startsWith("QC_"))
                .collect(Collectors.toList());
    }

    private NcEvent onlyQcNce() {
        List<NcEvent> nces = qcNces();
        assertEquals("expected exactly one QC NCE", 1, nces.size());
        return nces.get(0);
    }

    private Set<Integer> linkedAnalysisIds(NcEvent nce) {
        return nceSpecimenService.getSpecimenByNceId(nce.getId()).stream().map(NceSpecimen::getAnalysisId)
                .collect(Collectors.toSet());
    }
}
