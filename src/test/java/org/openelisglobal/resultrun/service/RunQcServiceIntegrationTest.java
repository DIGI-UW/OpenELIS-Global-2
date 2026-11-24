package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QcHoldService;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.qc.valueholder.RunVerdictSource;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.form.RunLink;
import org.openelisglobal.resultrun.form.RunQcBlock;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.resultrun.valueholder.ResultRunSource;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1200 slice 4: a control on a run gives each test it covers a verdict, a
 * failed test holds only that test's rows, and a hold is not cleared by a later
 * passing control (FR-C2, FR-C3, FR-C4, FR-C6).
 */
@Transactional
public class RunQcServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNIT = "97790";
    private static final String TEST = "97791";
    private static final String OTHER_TEST = "97792";
    private static final String ANALYZER = "97793";
    private static final String LOT = "ogc1200-s4-lot-l1";

    @Autowired
    private RunQcService runQcService;
    @Autowired
    private org.openelisglobal.analyzerresults.service.AnalyzerResultsService analyzerResultsService;
    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private ResultRunEventDAO resultRunEventDAO;
    @Autowired
    private QCResultDAO qcResultDAO;
    @Autowired
    private QcHoldService qcHoldService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private TestService testService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;
    private ResultRun run;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?::numeric, ?, NOW())",
                UNIT, "Run QC unit");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, name_localization_id, is_active,"
                + " domain, lastupdated) VALUES (?::numeric, 'Run QC unit', 'Run QC unit', ?::numeric, 'Y',"
                + " 'CLINICAL', NOW())", UNIT, UNIT);
        for (String test : List.of(TEST, OTHER_TEST)) {
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                            + " test_section_id, lastupdated) VALUES (?::numeric, ?, ?, 'Y', ?, 'CLINICAL', true,"
                            + " ?::numeric, NOW())",
                    test, "Run QC " + test, "Run QC " + test, UUID.randomUUID().toString(), UNIT);
        }
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, test_unit_ids, last_updated)"
                + " VALUES (?::numeric, 'Run QC analyzer', true, ?, NOW())", ANALYZER, UNIT);
        jdbc.update("INSERT INTO clinlims.qc_control_lot (id, product_name, lot_number, control_level, test_id,"
                + " instrument_id, status, sys_user_id) VALUES (?, 'Level normal', 'LOT-N', 'NORMAL', ?::int, ?::int,"
                + " 'ACTIVE', 1)", LOT, TEST, ANALYZER);
        jdbc.update(
                "INSERT INTO clinlims.test_qc_target (id, test_id, control_level, expected_value, uncertainty,"
                        + " is_active) VALUES (?, ?::numeric, 'NORMAL', 100, 5, true)",
                UUID.randomUUID().toString(), TEST);

        run = new ResultRun();
        run.setSource(ResultRunSource.ANALYZER);
        run.setAnalyzerId(ANALYZER);
        run.setOpenedAt(new Timestamp(System.currentTimeMillis()));
        run.setLastActivityAt(new Timestamp(System.currentTimeMillis()));
        run.setSysUserId("1");
        resultRunService.insert(run);
        entityManager.flush();
    }

    private QCResult analyzerControl(String testId, String value) {
        return analyzerControl(run.getId(), testId, value);
    }

    private QCResult analyzerControl(Long runId, String testId, String value) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO clinlims.qc_result (id, control_lot_id, test_id, instrument_id, source, result_value,"
                + " run_date_time, result_status, sys_user_id) VALUES (?, ?, ?::numeric, ?::numeric, 'ASTM',"
                + " ?::numeric, NOW(), 'PENDING', 1)", id, LOT, testId, ANALYZER, value);
        QCResult control = qcResultDAO.get(id).orElseThrow();
        runQcService.attachAnalyzerControl(control, runId, null);
        entityManager.flush();
        return qcResultDAO.get(id).orElseThrow();
    }

    private List<String> events() {
        return resultRunEventDAO.getEvents(run.getId()).stream().map(ResultRunEvent::getEvent).toList();
    }

    @Test
    public void aControlOutsideItsTargetFailsAndHoldsOnlyItsTest() {
        QCResult control = analyzerControl(TEST, "120");

        assertEquals(RunControlVerdict.FAIL, control.getRunVerdict());
        assertEquals(RunVerdictSource.COMPUTED, control.getVerdictSource());
        assertEquals(0, new BigDecimal("100").compareTo(control.getExpectedValue()));
        assertEquals(run.getId(), control.getRunId());
        assertEquals(Set.of(TEST), runQcService.heldTestIds(run.getId()));
        assertEquals(RunTestVerdict.FAIL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(ResultRunEvent.CONTROL_RECORDED, ResultRunEvent.TEST_HELD), events());
        assertEquals("HELD", runQcService.qcBlock(run.getId()).overall());
    }

    @Test
    public void aControlWithinItsTargetPassesAndHoldsNothing() {
        QCResult control = analyzerControl(TEST, "103");

        assertEquals(RunControlVerdict.PASS, control.getRunVerdict());
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals(RunTestVerdict.PASS, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(ResultRunEvent.CONTROL_RECORDED), events());
    }

    @Test
    public void aLaterPassingControlDoesNotClearTheHold() {
        analyzerControl(TEST, "120");
        analyzerControl(TEST, "101");

        assertEquals(RunTestVerdict.FAIL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals(
                List.of(ResultRunEvent.CONTROL_RECORDED, ResultRunEvent.TEST_HELD, ResultRunEvent.CONTROL_RECORDED),
                events());
    }

    @Test
    public void aControlTheTechnicianRecordsCarriesTheirVerdict() {
        analyzerControl(OTHER_TEST, "5");
        QCResult control = runQcService.recordControl(run.getId(), new BenchQCCaptureForm(QCSource.RDT, OTHER_TEST,
                UNIT, null, "Kit A", null, null, QCQualitativeOutcome.INVALID, null, null, null, null), 1);
        entityManager.flush();

        QCResult stored = qcResultDAO.get(control.getId()).orElseThrow();
        assertEquals(RunControlVerdict.FAIL, stored.getRunVerdict());
        assertEquals(RunVerdictSource.DECLARED, stored.getVerdictSource());
        assertEquals(Set.of(OTHER_TEST), runQcService.heldTestIds(run.getId()));
    }

    @Test
    public void aControlForATestTheRunDoesNotCoverIsRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> runQcService.recordControl(run.getId(), new BenchQCCaptureForm(QCSource.RDT, OTHER_TEST, UNIT,
                        null, "Kit A", null, null, QCQualitativeOutcome.INVALID, null, null, null, null), 1));
        assertTrue(runQcService.controlsOf(run.getId()).isEmpty());
    }

    @Test
    public void aMeasuredControlWhoseOutcomeContradictsItsNumbersIsCaught() {
        assertTrue(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("50"), null,
                        QCQualitativeOutcome.PASS, new BigDecimal("10"), BigDecimal.ONE, null, null)));
        assertFalse(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("50"), null,
                        QCQualitativeOutcome.FAIL, new BigDecimal("10"), BigDecimal.ONE, null, null)));
        assertFalse(RunQcServiceImpl.contradictsItsNumbers(
                new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT, null, new BigDecimal("10.5"), null,
                        QCQualitativeOutcome.PASS, new BigDecimal("10"), BigDecimal.ONE, null, null)));
    }

    @Test
    public void aResultStoredFromARunThatHoldsItsTestIsNotReleasedWhateverTheSiteSetting() {
        SampleItem item = fixtures.createSampleWithSampleItem("RQC");
        Analysis held = fixtures.createAnalysis(item, testService.get(TEST));
        Analysis other = fixtures.createAnalysis(item, testService.get(OTHER_TEST));
        for (Analysis analysis : List.of(held, other)) {
            jdbc.update(
                    "INSERT INTO clinlims.result (id, analysis_id, value, run_id, lastupdated) VALUES"
                            + " (nextval('clinlims.result_seq'), ?::numeric, '42', ?, now())",
                    analysis.getId(), run.getId());
        }
        entityManager.flush();
        assertEquals("a test whose result was already accepted is still covered by the run",
                RunTestVerdict.NOT_REQUIRED, runQcService.testVerdicts(run.getId()).get(TEST));
        analyzerControl(TEST, "120");

        assertEquals(Set.of(held.getId()),
                qcHoldService.analysisIdsBlockedFromRelease(List.of(held.getId(), other.getId())));

        TestResultItem heldRow = new TestResultItem();
        heldRow.setAnalysisId(held.getId());
        heldRow.setTestId(TEST);
        heldRow.setResultValue("42");
        TestResultItem otherRow = new TestResultItem();
        otherRow.setAnalysisId(other.getId());
        otherRow.setTestId(OTHER_TEST);
        otherRow.setResultValue("42");
        resultRunService.markWaiting(List.of(heldRow, otherRow));
        assertTrue("the worklist shows the stored result held, so it is not edited",
                heldRow.getWaitingRun() != null && heldRow.getWaitingRun().held());
        assertEquals(run.getId(), heldRow.getWaitingRun().runId());
        assertEquals(null, otherRow.getWaitingRun());
    }

    private Analysis storedResultOf(String testId) {
        SampleItem item = fixtures.createSampleWithSampleItem("RQD");
        Analysis analysis = fixtures.createAnalysis(item, testService.get(testId));
        jdbc.update(
                "INSERT INTO clinlims.result (id, analysis_id, value, run_id, lastupdated) VALUES"
                        + " (nextval('clinlims.result_seq'), ?::numeric, '42', ?, now())",
                analysis.getId(), run.getId());
        entityManager.flush();
        return analysis;
    }

    @Test
    public void acceptingDespiteTheFailureNeedsAReasonFilesANonConformityAndReleasesTheResults() {
        Analysis held = storedResultOf(TEST);
        analyzerControl(TEST, "120");

        assertThrows(IllegalArgumentException.class,
                () -> runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE, " ", false, "1"));

        ResultRunTestDisposition decision = runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE,
                "Control material expired; results checked against a second run", false, "1");
        entityManager.flush();

        assertTrue("an Accept despite always files its non-conformity", decision.getNceId() != null);
        assertEquals(1,
                (int) jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.nce_specimen WHERE nce_id = ?" + " AND analysis_id = ?::numeric",
                        Integer.class, decision.getNceId(), held.getId()));
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertTrue("its stored results may now be released",
                qcHoldService.analysisIdsBlockedFromRelease(List.of(held.getId())).isEmpty());
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertEquals("DECIDED", runQcService.qcBlock(run.getId()).overall());
        assertTrue(events().containsAll(List.of(ResultRunEvent.ACCEPTED_DESPITE_QC, ResultRunEvent.TEST_RELEASED)));
    }

    @Test
    public void aRejectedTestKeepsItsResultsFromReleaseAndIsDecidedOnce() {
        Analysis held = storedResultOf(TEST);
        analyzerControl(TEST, "120");

        ResultRunTestDisposition decision = runQcService.decide(run.getId(), TEST, RunTestAction.REJECT, null, false,
                "1");
        entityManager.flush();

        assertEquals("no non-conformity unless asked for", null, decision.getNceId());
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals(Set.of(held.getId()), qcHoldService.analysisIdsBlockedFromRelease(List.of(held.getId())));
        assertTrue(events().contains(ResultRunEvent.REJECTED_QC));
        assertThrows(IllegalArgumentException.class,
                () -> runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE, "late", false, "1"));
    }

    @Test
    public void aRetestedTestIsOpenToANewValueWhileTheRunsOwnValueStaysUnreleased() {
        Analysis held = storedResultOf(TEST);
        analyzerControl(TEST, "120");
        assertEquals("held for entry until decided", Set.of(held.getId()),
                runQcService.entryHoldingRuns(List.of(held.getId())).keySet());

        runQcService.decide(run.getId(), TEST, RunTestAction.RETEST, null, false, "1");
        entityManager.flush();

        assertTrue("a decided test takes a new value", runQcService.entryHoldingRuns(List.of(held.getId())).isEmpty());
        assertEquals("the run's own value is still not released", Set.of(held.getId()),
                qcHoldService.analysisIdsBlockedFromRelease(List.of(held.getId())));
        TestResultItem row = new TestResultItem();
        row.setAnalysisId(held.getId());
        row.setTestId(TEST);
        row.setResultValue("42");
        resultRunService.markWaiting(List.of(row));
        assertNull("the worklist offers the row for entry again", row.getWaitingRun());
    }

    @Test
    public void aControlForATestAlreadyDecidedOnTheRunIsRefused() {
        analyzerControl(TEST, "120");
        runQcService.decide(run.getId(), TEST, RunTestAction.REJECT, null, false, "1");
        entityManager.flush();

        assertThrows(IllegalArgumentException.class,
                () -> runQcService.recordControl(run.getId(), new BenchQCCaptureForm(QCSource.MANUAL, TEST, UNIT, LOT,
                        null, new BigDecimal("130"), null, null, null, null, null, null), 1));
        assertEquals(1, runQcService.controlsOf(run.getId()).size());
    }

    @Test
    public void aDecisionThatLosesARaceIsReportedAsAlreadyDecided() {
        java.sql.SQLException duplicate = new java.sql.SQLException("duplicate key", "23505");
        assertTrue(RunQcServiceImpl.isDuplicateDecision(
                new LIMSRuntimeException("insert", new org.hibernate.exception.ConstraintViolationException("dup",
                        duplicate, "uq_run_test_disposition"))));
        assertFalse(RunQcServiceImpl.isDuplicateDecision(new LIMSRuntimeException("insert",
                new org.hibernate.exception.ConstraintViolationException("fk", duplicate, "fk_something_else"))));
        assertFalse(RunQcServiceImpl.isDuplicateDecision(new IllegalStateException("other")));
    }

    private ResultRun laterRun() {
        ResultRun later = new ResultRun();
        later.setSource(ResultRunSource.ANALYZER);
        later.setAnalyzerId(ANALYZER);
        later.setOpenedAt(new Timestamp(System.currentTimeMillis()));
        later.setLastActivityAt(new Timestamp(System.currentTimeMillis()));
        later.setSysUserId("1");
        resultRunService.insert(later);
        entityManager.flush();
        return later;
    }

    private void acceptedIn(ResultRun later, Analysis analysis) {
        jdbc.update("UPDATE clinlims.result SET run_id = ? WHERE analysis_id = ?::numeric", later.getId(),
                analysis.getId());
        entityManager.flush();
        entityManager.clear();
    }

    private void retestTheTest() {
        analyzerControl(TEST, "120");
        runQcService.decide(run.getId(), TEST, RunTestAction.RETEST, null, false, "1");
        jdbc.update("UPDATE clinlims.result_run SET state = 'COMPLETE' WHERE id = ?", run.getId());
        entityManager.flush();
        entityManager.clear();
    }

    private RunQcBlock.TestLine line(Long runId, String testId) {
        return runQcService.qcBlock(runId).tests().stream().filter(line -> testId.equals(line.testId())).findFirst()
                .orElseThrow();
    }

    private static List<Long> runIds(List<RunLink> links) {
        return links.stream().map(RunLink::runId).toList();
    }

    /**
     * OGC-1200 slice 5b: Retest remembers which of the run's samples it sets aside
     * for that test, so a later run can be offered as their retest (FR-C5).
     */
    @Test
    public void aRetestRemembersTheSamplesItSetsAside() {
        Analysis retested = storedResultOf(TEST);
        storedResultOf(OTHER_TEST);
        analyzerControl(TEST, "120");

        runQcService.decide(run.getId(), TEST, RunTestAction.RETEST, null, false, "1");
        entityManager.flush();

        assertEquals(List.of(retested.getId()),
                jdbc.queryForList("SELECT CAST(analysis_id AS varchar) FROM clinlims.result_run_retest", String.class));
    }

    /**
     * FR-C5: a later run covering the same samples suggests "retest of RUN-x" for
     * that test only, and not while its own control holds the test undecided.
     */
    @Test
    public void aLaterRunOfTheSameSamplesSuggestsTheRetestLink() {
        Analysis retested = storedResultOf(TEST);
        Analysis other = storedResultOf(OTHER_TEST);
        retestTheTest();
        ResultRun later = laterRun();
        acceptedIn(later, retested);
        acceptedIn(later, other);

        RunQcBlock.TestLine line = line(later.getId(), TEST);
        assertEquals(List.of(run.getId()), runIds(line.retestSuggestions()));
        assertEquals(1, line.retestSuggestions().get(0).count());
        assertEquals(1, line.retestSuggestions().get(0).total());
        assertEquals("RUN-" + run.getId(), line.retestSuggestions().get(0).label());
        assertTrue(line(later.getId(), OTHER_TEST).retestSuggestions().isEmpty());
        assertTrue("an earlier run is never offered a later one",
                line(run.getId(), TEST).retestSuggestions().isEmpty());

        analyzerControl(later.getId(), TEST, "120");
        assertTrue("not while the later run's own control holds the test",
                line(later.getId(), TEST).retestSuggestions().isEmpty());
    }

    /**
     * FR-A3: confirming the link supersedes the retested test, and the whole run
     * when every test it covers was retested and linked.
     */
    @Test
    public void confirmingTheLinkSupersedesTheTestAndAFullyRetestedRun() {
        Analysis retested = storedResultOf(TEST);
        retestTheTest();
        ResultRun later = laterRun();
        acceptedIn(later, retested);

        runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1");
        entityManager.flush();

        assertEquals(List.of(run.getId()), runIds(line(later.getId(), TEST).retestOf()));
        assertTrue(line(later.getId(), TEST).retestSuggestions().isEmpty());
        assertEquals(List.of(later.getId()), runIds(line(run.getId(), TEST).supersededBy()));
        assertEquals(ResultRunState.SUPERSEDED, resultRunService.get(run.getId()).getState());
        assertEquals(2, events().stream().filter(ResultRunEvent.SUPERSEDED::equals).count());
        assertEquals(1,
                (int) jdbc.queryForObject("SELECT count(*) FROM clinlims.result_run_retest WHERE retested_in_run_id = ?"
                        + " AND linked_by = 1 AND linked_at IS NOT NULL", Integer.class, later.getId()));
    }

    /**
     * FR-A3: a run whose other tests were accepted stays Complete; only the
     * retested test reads superseded.
     */
    @Test
    public void aRunWithOtherTestsAcceptedStaysCompleteWhenOneTestIsSuperseded() {
        Analysis retested = storedResultOf(TEST);
        storedResultOf(OTHER_TEST);
        retestTheTest();
        ResultRun later = laterRun();
        acceptedIn(later, retested);

        runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1");
        entityManager.flush();

        assertEquals(ResultRunState.COMPLETE, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(later.getId()), runIds(line(run.getId(), TEST).supersededBy()));
    }

    /**
     * A retest that covers only some of the samples links those; the rest stay
     * suggested on the next run that covers them.
     */
    @Test
    public void aPartialRetestLeavesTheRestSuggestedOnTheNextRun() {
        Analysis first = storedResultOf(TEST);
        Analysis second = storedResultOf(TEST);
        retestTheTest();
        ResultRun later = laterRun();
        acceptedIn(later, first);

        assertEquals(1, line(later.getId(), TEST).retestSuggestions().get(0).count());
        assertEquals(2, line(later.getId(), TEST).retestSuggestions().get(0).total());
        runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1");
        entityManager.flush();
        assertEquals(ResultRunState.COMPLETE, resultRunService.get(run.getId()).getState());

        ResultRun last = laterRun();
        acceptedIn(last, second);
        assertEquals(List.of(run.getId()), runIds(line(last.getId(), TEST).retestSuggestions()));
        runQcService.confirmRetest(last.getId(), TEST, run.getId(), "1");
        entityManager.flush();
        assertEquals(ResultRunState.SUPERSEDED, resultRunService.get(run.getId()).getState());
        assertEquals(List.of(later.getId(), last.getId()), runIds(line(run.getId(), TEST).supersededBy()));
    }

    /**
     * FR-A3: a run that still had a row to review when its retest was confirmed is
     * superseded once that row leaves, as it would have been without the row.
     */
    @Test
    public void aRunWithARowLeftToReviewIsSupersededWhenTheRowLeaves() {
        Analysis retested = storedResultOf(TEST);
        retestTheTest();
        jdbc.update("UPDATE clinlims.result_run SET state = 'OPEN' WHERE id = ?", run.getId());
        AnalyzerResults leftover = stagedRow(run.getId(), "RQD-LEFTOVER");
        ResultRun later = laterRun();
        acceptedIn(later, retested);

        runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1");
        entityManager.flush();
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());

        analyzerResultsService.delete(leftover.getId(), "1");
        entityManager.flush();
        entityManager.clear();

        assertEquals(ResultRunState.SUPERSEDED, resultRunService.get(run.getId()).getState());
        assertEquals(2, events().stream().filter(ResultRunEvent.SUPERSEDED::equals).count());
        ResultRunEvent whole = resultRunEventDAO.getEvents(run.getId()).stream()
                .filter(event -> ResultRunEvent.SUPERSEDED.equals(event.getEvent()) && event.getTestId() == null)
                .findFirst().orElseThrow();
        assertEquals("the run is superseded by whoever removed its last row", "1", whole.getActorId());
    }

    /**
     * A run with nothing set aside completes as before when its last row leaves.
     */
    @Test
    public void aRunWithNothingRetestedCompletesWhenItsLastRowLeaves() {
        AnalyzerResults only = stagedRow(run.getId(), "RQD-ONLY");

        analyzerResultsService.delete(analyzerResultsService.get(only.getId()));
        entityManager.flush();
        entityManager.clear();

        assertEquals(ResultRunState.COMPLETE, resultRunService.get(run.getId()).getState());
        assertFalse(events().contains(ResultRunEvent.SUPERSEDED));
    }

    private AnalyzerResults stagedRow(Long runId, String accession) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(ANALYZER);
        row.setAccessionNumber(accession);
        row.setTestName("Run QC " + TEST);
        row.setTestId(TEST);
        row.setResult("42");
        row.setIsControl(false);
        row.setRunId(runId);
        row.setRawSubIdentity("");
        row.setCompleteDate(new Timestamp(System.currentTimeMillis()));
        row.setSysUserId("1");
        row.setId(analyzerResultsService.insert(row));
        return row;
    }

    /**
     * A re-run that reached OpenELIS before the Retest click was flagged as a
     * duplicate of the run's own row. Once Retest sets that row aside, the re-run's
     * row is a reading of its own again, and its run is suggested as the retest.
     */
    @Test
    public void aRetestReleasesTheLaterRunsReadingFromBeingADuplicate() {
        SampleItem item = fixtures.createSampleWithSampleItem("RQR");
        fixtures.createAnalysis(item, testService.get(TEST));
        String accession = item.getSample().getAccessionNumber();
        ResultRun later = laterRun();
        AnalyzerResults earlier = stagedRow(run.getId(), accession);
        AnalyzerResults reread = stagedRow(later.getId(), accession);
        reread.setDuplicateAnalyzerResultId(earlier.getId());
        reread.setReadOnly(true);
        analyzerResultsService.update(reread);
        earlier.setDuplicateAnalyzerResultId(reread.getId());
        analyzerResultsService.update(earlier);
        entityManager.flush();
        analyzerControl(TEST, "120");

        runQcService.decide(run.getId(), TEST, RunTestAction.RETEST, null, false, "1");
        entityManager.flush();
        entityManager.clear();

        AnalyzerResults released = analyzerResultsService.get(reread.getId());
        assertNull(released.getDuplicateAnalyzerResultId());
        assertFalse(released.isReadOnly());
        assertEquals(List.of(run.getId()), runIds(line(later.getId(), TEST).retestSuggestions()));
    }

    /** A link is recorded once; a second confirmation is refused. */
    @Test
    public void aLinkIsRecordedOnce() {
        Analysis retested = storedResultOf(TEST);
        retestTheTest();
        ResultRun later = laterRun();
        acceptedIn(later, retested);
        runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1");
        entityManager.flush();

        assertThrows(IllegalArgumentException.class,
                () -> runQcService.confirmRetest(later.getId(), TEST, run.getId(), "1"));
        assertThrows("nothing to link on a run that retests nothing", IllegalArgumentException.class,
                () -> runQcService.confirmRetest(later.getId(), OTHER_TEST, run.getId(), "1"));
    }

    @Test
    public void onlyAHeldTestCanBeDecided() {
        analyzerControl(TEST, "103");

        assertThrows(IllegalArgumentException.class,
                () -> runQcService.decide(run.getId(), TEST, RunTestAction.RETEST, null, false, "1"));
    }
}
