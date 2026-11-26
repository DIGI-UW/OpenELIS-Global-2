package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.openelisglobal.qc.service.QcItemRules;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.qc.valueholder.RunControlVerdict;
import org.openelisglobal.qc.valueholder.RunVerdictSource;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultrun.dao.ResultRunEventDAO;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.form.RunHoldCause;
import org.openelisglobal.resultrun.form.RunLink;
import org.openelisglobal.resultrun.form.RunQcBlock;
import org.openelisglobal.resultrun.form.RunQcFact;
import org.openelisglobal.resultrun.form.RunQcItemForm;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunEvent;
import org.openelisglobal.resultrun.valueholder.ResultRunQcItem;
import org.openelisglobal.resultrun.valueholder.ResultRunSource;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition;
import org.openelisglobal.resultrun.valueholder.RunQcItemKind;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.service.TestQcPolicyService;
import org.openelisglobal.testcatalog.valueholder.TestQcPolicy;
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
    private org.openelisglobal.systemuser.service.SystemUserService systemUserService;
    @Autowired
    private TestQcPolicyService testQcPolicyService;
    @Autowired
    private org.openelisglobal.resultvalidation.service.ResultValidationService resultValidationService;
    @Autowired
    private org.openelisglobal.result.service.ResultService resultService;
    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;
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
        org.openelisglobal.systemuser.valueholder.SystemUser technician = systemUserService.getUserById("1");
        assertEquals("the control names who recorded it as sign-offs do",
                technician.getFirstName().trim() + " " + technician.getLastName().trim(),
                line(run.getId(), OTHER_TEST).controls().stream().filter(c -> c.id().equals(control.getId()))
                        .findFirst().orElseThrow().operator());
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

    /**
     * OGC-1200 FR-H1, FR-C7: a test that requires a control on every run, and a run
     * whose message brought none for it, is held as QC missing at once, and a
     * control recorded on the run releases it.
     */
    @Test
    public void aRequiredControlMissingUnderAHoldPolicyHoldsItsTestUntilOneIsRecorded() {
        requireControl(TEST, "HOLD");
        String heldOrder = orderOf(TEST);
        stagedRow(run.getId(), heldOrder, TEST);
        stagedRow(run.getId(), orderOf(OTHER_TEST), OTHER_TEST);
        resultRunService.refreshQcState(run.getId(), "1");
        entityManager.flush();

        assertEquals(RunTestVerdict.NO_CONTROL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(RunTestVerdict.NOT_REQUIRED, runQcService.testVerdicts(run.getId()).get(OTHER_TEST));
        assertEquals(Map.of(TEST, RunHoldCause.QC_MISSING), runQcService.holdCauses(run.getId()));
        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals("HELD", runQcService.qcBlock(run.getId()).overall());
        assertEquals(RunHoldCause.QC_MISSING, line(run.getId(), TEST).holdCause());
        assertEquals("HOLD", line(run.getId(), TEST).whenMissing());
        assertEquals("qcMissing", heldDetail(TEST));
        ResultRunRowClassifier.ClassifiedRow held = classified(heldOrder);
        assertEquals(ResultRunRowCategory.HELD, held.category());
        assertEquals(RunHoldCause.QC_MISSING, held.holdCause());

        analyzerControl(TEST, "101");

        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertTrue(events().contains(ResultRunEvent.TEST_RELEASED));
    }

    /**
     * FR-A4: under a Warn policy the missing control is named on the run and on the
     * test's rows, and nothing is held.
     */
    @Test
    public void aRequiredControlMissingUnderAWarnPolicyWarnsAndHoldsNothing() {
        requireControl(TEST, "WARN");
        String warnedOrder = orderOf(TEST);
        stagedRow(run.getId(), warnedOrder, TEST);
        resultRunService.refreshQcState(run.getId(), "1");
        entityManager.flush();

        assertEquals(RunTestVerdict.NO_CONTROL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertEquals("NO_CONTROL", runQcService.qcBlock(run.getId()).overall());
        assertNull(line(run.getId(), TEST).holdCause());
        assertEquals("WARN", line(run.getId(), TEST).whenMissing());
        ResultRunRowClassifier.ClassifiedRow warned = classified(warnedOrder);
        assertEquals("the row carries the warning", List.of("CONTROL"), warned.missingQc());
        assertEquals(List.of("CONTROL"), line(run.getId(), TEST).missing());
        assertEquals(ResultRunRowCategory.NORMAL, warned.category());
    }

    /**
     * A policy saved in the Test Catalog holds the open runs of the test at once,
     * and never a run that is already complete.
     */
    @Test
    public void aPolicySavedLaterHoldsTheOpenRunsOfTheTestAndNeverACompleteOne() {
        stagedRow(run.getId(), "RQP-OPEN", TEST);
        ResultRun complete = laterRun();
        storedResultIn(complete, TEST);
        complete.setState(ResultRunState.COMPLETE);
        resultRunService.update(complete);
        entityManager.flush();
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());

        TestQcPolicy policy = TestQcPolicy.none(TEST);
        policy.setControlRequired(true);
        policy.setControlWhenMissing(TestQcPolicy.WhenMissing.HOLD);
        testQcPolicyService.saveSection(TEST, List.of(), policy, "1");
        entityManager.flush();

        assertEquals(ResultRunState.HELD, resultRunService.get(run.getId()).getState());
        assertEquals(ResultRunState.COMPLETE, resultRunService.get(complete.getId()).getState());
        assertTrue("a complete run is not held by a policy saved after it",
                runQcService.heldTestIds(complete.getId()).isEmpty());
    }

    /**
     * FR-C5, FR-G3: a test held for a missing control is decided like a failed one;
     * accepting it despite the hold files a non-conformity that says the control
     * was missing, and Validation sees a known QC problem.
     */
    @Test
    public void acceptingDespiteAMissingControlFilesANonConformityAndReachesValidationAsAQcProblem() {
        requireControl(TEST, "HOLD");
        Analysis stored = storedResultOf(TEST);
        resultRunService.refreshQcState(run.getId(), "1");
        entityManager.flush();
        assertEquals(Map.of(TEST, RunHoldCause.QC_MISSING), runQcService.holdCauses(run.getId()));

        ResultRunTestDisposition decision = runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE,
                "Control material out of stock; results reviewed by the supervisor", false, "1");
        entityManager.flush();

        assertEquals(1,
                (int) jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.nc_event WHERE id = ? AND title LIKE 'Missing control on %'",
                        Integer.class, decision.getNceId()));
        assertTrue(runQcService.heldTestIds(run.getId()).isEmpty());
        assertEquals("the missing control stays visible once decided", "DECIDED_MISSING",
                runQcService.qcBlock(run.getId()).overall());
        assertTrue("accepted despite the missing control is a QC problem for the validator",
                runQcService.qcOfAnalyses(List.of(stored.getId())).get(stored.getId()).failed());
    }

    /**
     * A test held for a missing control and rejected leaves no value and no control
     * on the run, and the QC block still shows what was decided for it.
     */
    @Test
    public void aRejectedMissingControlStaysListedWithItsDecision() {
        requireControl(TEST, "HOLD");
        stagedRow(run.getId(), orderOf(TEST), TEST);
        resultRunService.refreshQcState(run.getId(), "1");
        entityManager.flush();

        runQcService.decide(run.getId(), TEST, RunTestAction.REJECT, null, false, "1");
        entityManager.flush();

        RunQcBlock.TestLine rejected = line(run.getId(), TEST);
        assertEquals("REJECT", rejected.decision());
        assertEquals(RunTestVerdict.NO_CONTROL.name(), rejected.verdict());
        assertEquals("DECIDED_MISSING", runQcService.qcBlock(run.getId()).overall());
    }

    private void requireControl(String testId, String whenMissing) {
        jdbc.update("INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                + " last_updated) VALUES (?::numeric, true, ?, NOW())", testId, whenMissing);
    }

    private String heldDetail(String testId) {
        return resultRunEventDAO.getEvents(run.getId()).stream()
                .filter(event -> ResultRunEvent.TEST_HELD.equals(event.getEvent()) && testId.equals(event.getTestId()))
                .map(ResultRunEvent::getDetail).findFirst().orElse(null);
    }

    private ResultRunRowClassifier.ClassifiedRow classified(String accession) {
        return resultRunService.classifiedRows(run.getId(), new RunViewer(Set.of(TEST, OTHER_TEST), Set.of(UNIT)))
                .orElseThrow().stream().filter(row -> accession.equals(row.row().getAccessionNumber())).findFirst()
                .orElseThrow();
    }

    private AnalyzerResults stagedRow(Long runId, String accession, String testId) {
        AnalyzerResults row = stagedRow(runId, accession);
        if (!TEST.equals(testId)) {
            row.setTestId(testId);
            row.setTestName("Run QC " + testId);
            analyzerResultsService.update(row);
        }
        return row;
    }

    /**
     * OGC-1200 slice 10a (FR-C10, FR-C12): a lab blank above the test's ceiling
     * fails and holds the test under the default Hold policy; one at or below
     * passes. Under a Warn failure policy the test only warns.
     */
    @Test
    public void aLabBlankAboveTheCeilingHoldsItsTestAndUnderAWarnPolicyOnlyWarns() {
        stagedRow(run.getId(), orderOf(TEST), TEST);
        stagedRow(run.getId(), orderOf(OTHER_TEST), OTHER_TEST);
        limits(TEST, "0.5", "20");
        limits(OTHER_TEST, "0.5", "20");

        assertEquals(QcItemRules.Evaluation.PASS, item(TEST, RunQcItemKind.BLANK, "0.4", null).getEvaluation());
        assertEquals(RunTestVerdict.PASS, runQcService.testVerdicts(run.getId()).get(TEST));
        ResultRunQcItem failing = item(TEST, RunQcItemKind.BLANK, "0.9", null);
        assertEquals(QcItemRules.Evaluation.FAIL, failing.getEvaluation());
        assertEquals("Result = 0.9 (threshold 0.5)", failing.getDetail());
        assertEquals(RunTestVerdict.FAIL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(RunHoldCause.QC_FAILED, runQcService.holdCauses(run.getId()).get(TEST));

        jdbc.update(
                "INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                        + " blank_when_fails, last_updated) VALUES (?::numeric, false, 'WARN', 'WARN', NOW())",
                OTHER_TEST);
        item(OTHER_TEST, RunQcItemKind.BLANK, "0.9", null);
        assertEquals(RunTestVerdict.PASS_WITH_WARNING, runQcService.testVerdicts(run.getId()).get(OTHER_TEST));
        assertFalse("a warned blank failure holds nothing",
                runQcService.holdCauses(run.getId()).containsKey(OTHER_TEST));
        RunQcBlock.TestLine line = runQcService.qcBlock(run.getId()).tests().stream()
                .filter(test -> OTHER_TEST.equals(test.testId())).findFirst().orElseThrow();
        assertEquals(1, line.items().size());
        assertEquals("BLANK", line.items().get(0).kind());
        assertEquals("FAIL", line.items().get(0).evaluation());
    }

    /**
     * FR-C11: a lab duplicate of a sample in the run is evaluated against that
     * sample's result, by the test's RPD limit; recorded before the result, it is
     * evaluated once the result is saved.
     */
    @Test
    public void aLabDuplicateIsEvaluatedAgainstItsSamplesResultOnceSaved() {
        String accession = orderOf(TEST);
        stagedRow(run.getId(), accession, TEST);
        String parent = analysisOf(accession);
        limits(TEST, null, "10");

        ResultRunQcItem duplicate = item(TEST, RunQcItemKind.DUPLICATE, "100", parent);
        assertEquals(QcItemRules.Evaluation.NOT_EVALUATED, duplicate.getEvaluation());
        assertEquals(RunTestVerdict.PASS, runQcService.testVerdicts(run.getId()).get(TEST));

        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, lastupdated) VALUES"
                + " (nextval('clinlims.result_seq'), ?::numeric, '130', now())", parent);
        entityManager.flush();
        runQcService.reevaluateDuplicates(run.getId(), "1");
        entityManager.flush();
        entityManager.clear();

        assertEquals("FAIL", jdbc.queryForObject("SELECT evaluation FROM clinlims.result_run_qc_item WHERE id = ?",
                String.class, duplicate.getId()));
        assertEquals(RunHoldCause.QC_FAILED, runQcService.holdCauses(run.getId()).get(TEST));
        try {
            item(TEST, RunQcItemKind.DUPLICATE, "100", analysisOf(orderOf(TEST)));
            fail("a duplicate of a sample outside the run is refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("repeats a sample of this run"));
        }
    }

    /**
     * FR-H1, FR-C7, FR-H4: a blank the test requires per run, missing under a Hold
     * policy, holds the test until one is recorded; a run of one requires none.
     */
    @Test
    public void aRequiredBlankMissingUnderHoldHoldsTheTestAndARunOfOneRequiresNone() {
        stagedRow(run.getId(), orderOf(TEST), TEST);
        jdbc.update("INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                + " blank_required, blank_when_missing, last_updated) VALUES (?::numeric, false, 'WARN', true, 'HOLD',"
                + " NOW())", TEST);
        limits(TEST, "0.5", null);

        assertEquals(RunTestVerdict.NO_CONTROL, runQcService.testVerdicts(run.getId()).get(TEST));
        assertEquals(RunHoldCause.QC_MISSING, runQcService.holdCauses(run.getId()).get(TEST));
        item(TEST, RunQcItemKind.BLANK, "0.1", null);
        assertEquals(RunTestVerdict.PASS, runQcService.testVerdicts(run.getId()).get(TEST));
        assertTrue(runQcService.holdCauses(run.getId()).isEmpty());

        TestQcPolicy policy = testQcPolicyService.policyOf(TEST);
        assertEquals(RunTestVerdict.NO_CONTROL, RunQcServiceImpl.verdictOf(List.of(), List.of(), policy, false));
        assertEquals(RunTestVerdict.NOT_REQUIRED, RunQcServiceImpl.verdictOf(List.of(), List.of(), policy, true));
    }

    /**
     * FR-H1: what a test lacks is named as what it is, on the run, on the rows the
     * run warns and in the non-conformity a decision files, never as a control.
     */
    @Test
    public void aMissingBlankOrDuplicateIsNamedAsItselfOnTheRunTheRowsAndTheNonConformity() {
        String warnedOrder = orderOf(OTHER_TEST);
        stagedRow(run.getId(), orderOf(TEST), TEST);
        stagedRow(run.getId(), warnedOrder, OTHER_TEST);
        jdbc.update("INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                + " blank_required, blank_when_missing, last_updated) VALUES (?::numeric, false, 'WARN', true, 'HOLD',"
                + " NOW())", TEST);
        jdbc.update("INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                + " duplicate_required, duplicate_when_missing, last_updated) VALUES (?::numeric, false, 'WARN', true,"
                + " 'WARN', NOW())", OTHER_TEST);

        RunQcBlock.TestLine held = line(run.getId(), TEST);
        assertEquals(List.of("BLANK"), held.missing());
        assertEquals("HOLD", held.whenMissing());
        RunQcBlock.TestLine warned = line(run.getId(), OTHER_TEST);
        assertEquals(List.of("DUPLICATE"), warned.missing());
        assertEquals("WARN", warned.whenMissing());
        assertEquals(List.of("DUPLICATE"), classified(warnedOrder).missingQc());

        ResultRunTestDisposition decision = runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE,
                "Blank material out of stock", false, "1");
        entityManager.flush();
        Map<String, Object> nce = jdbc.queryForMap("SELECT title, description FROM clinlims.nc_event WHERE id = ?",
                decision.getNceId());
        assertTrue(nce.toString(), String.valueOf(nce.get("title")).startsWith("Missing blank on "));
        assertTrue(nce.toString(), String.valueOf(nce.get("description"))
                .contains("accepted without its required blank. The test requires a blank on every run"));
        assertNull("the decided test is held no longer", line(run.getId(), TEST).holdCause());
        assertEquals("control, blank and duplicate",
                RunQcServiceImpl.kindsInWords(List.of("CONTROL", "BLANK", "DUPLICATE")));
    }

    /**
     * FR-C12, FR-G3: a blank that failed is named in the non-conformity of the
     * decision on it, and the validator sees the run's blanks beside its verdict.
     */
    @Test
    public void aFailedBlankIsNamedInTheNonConformityAndReachesValidationWithTheRunsBlanks() {
        Analysis stored = storedResultOf(TEST);
        limits(TEST, "0.5", null);
        item(TEST, RunQcItemKind.BLANK, "0.9", null);
        assertEquals(RunHoldCause.QC_FAILED, runQcService.holdCauses(run.getId()).get(TEST));

        RunQcFact fact = runQcService.qcOfAnalyses(List.of(stored.getId())).get(stored.getId());
        assertEquals(1, fact.items().size());
        assertEquals("Result = 0.9 (threshold 0.5)", fact.items().get(0).detail());
        assertTrue(fact.missing().isEmpty());

        ResultRunTestDisposition decision = runQcService.decide(run.getId(), TEST, RunTestAction.ACCEPT_DESPITE,
                "Reviewed by the supervisor", false, "1");
        entityManager.flush();
        String description = jdbc.queryForObject("SELECT description FROM clinlims.nc_event WHERE id = ?", String.class,
                decision.getNceId());
        assertTrue(description, description.contains("Failing blank: Result = 0.9 (threshold 0.5)."));
        assertFalse(description, description.contains("Failing control"));
    }

    /**
     * FR-C11: a duplicate follows its sample's result. A result corrected after the
     * duplicate was judged, from the run, Results Entry or Validation, judges it
     * again, and the run holds or releases its test with it.
     */
    @Test
    public void aDuplicateFollowsItsSamplesResultWhenTheResultIsCorrected() {
        String accession = orderOf(TEST);
        stagedRow(run.getId(), accession, TEST);
        String parent = analysisOf(accession);
        limits(TEST, null, "10");
        ResultRunQcItem duplicate = item(TEST, RunQcItemKind.DUPLICATE, "100", parent);
        assertEquals("Its sample has no result yet", duplicate.getDetail());

        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, run_id, lastupdated) VALUES"
                + " (nextval('clinlims.result_seq'), ?::numeric, '130', ?, now())", parent, run.getId());
        resultRunService.recordIntoWorkplanRuns(List.of(resultOf(parent)), "1").transactionalUpdate(null);
        entityManager.flush();
        entityManager.clear();
        assertEquals("FAIL RPD = 26.1% (threshold 10%)", judgementOf(duplicate.getId()));
        assertEquals(RunHoldCause.QC_FAILED, runQcService.holdCauses(run.getId()).get(TEST));

        jdbc.update("UPDATE clinlims.result SET value = '105' WHERE analysis_id = ?::numeric", parent);
        resultRunService.recordIntoWorkplanRuns(List.of(resultOf(parent)), "1").transactionalUpdate(null);
        entityManager.flush();
        entityManager.clear();
        assertEquals("a result already in the run, saved again, judges its duplicate again",
                "PASS RPD = 4.9% (threshold 10%)", judgementOf(duplicate.getId()));
        assertTrue(runQcService.holdCauses(run.getId()).isEmpty());

        jdbc.update("UPDATE clinlims.result SET value = '140' WHERE analysis_id = ?::numeric", parent);
        Result validated = resultService.getResultsByAnalysis(analysisService.get(parent)).get(0);
        resultValidationService.persistdata(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(List.of(validated)),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, new ArrayList<>(), "1");
        entityManager.flush();
        entityManager.clear();
        assertEquals("a value changed in Validation judges the duplicate again", "FAIL RPD = 33.3% (threshold 10%)",
                judgementOf(duplicate.getId()));
        assertEquals(3, events().stream().filter(ResultRunEvent.QC_ITEM_EVALUATED::equals).count());
    }

    private Result resultOf(String analysisId) {
        Analysis analysis = new Analysis();
        analysis.setId(analysisId);
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setRunId(run.getId());
        return result;
    }

    private String judgementOf(Long itemId) {
        return jdbc.queryForObject("SELECT evaluation || ' ' || detail FROM clinlims.result_run_qc_item WHERE id = ?",
                String.class, itemId);
    }

    @Test
    public void aBlankOrDuplicateWithoutAValueOrOnADecidedTestIsRefused() {
        stagedRow(run.getId(), orderOf(TEST), TEST);
        try {
            item(TEST, RunQcItemKind.BLANK, " ", null);
            fail("a blank without a value is refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Enter the value"));
        }
        try {
            item("999999", RunQcItemKind.BLANK, "0.1", null);
            fail("a test not in the run is refused");
        } catch (IllegalArgumentException expected) {
            assertFalse(expected.getMessage().isBlank());
        }
    }

    private ResultRunQcItem item(String testId, RunQcItemKind kind, String value, String parentAnalysisId) {
        ResultRunQcItem item = runQcService.recordQcItem(run.getId(),
                new RunQcItemForm(testId, kind, value, null, parentAnalysisId), "1");
        entityManager.flush();
        return item;
    }

    private void limits(String testId, String blankCeiling, String rpdLimit) {
        testService.saveQcThreshold(testId, blankCeiling == null ? null : new BigDecimal(blankCeiling),
                rpdLimit == null ? null : new BigDecimal(rpdLimit), null, "1");
        entityManager.flush();
    }

    private String analysisOf(String accession) {
        return jdbc.queryForObject("SELECT CAST(a.id AS VARCHAR) FROM clinlims.analysis a"
                + " JOIN clinlims.sample_item si ON si.id = a.sampitem_id JOIN clinlims.sample s ON s.id = si.samp_id"
                + " WHERE s.accession_number = ?", String.class, accession);
    }

    /** A new order with the test on it, waiting for a result. */
    private String orderOf(String testId) {
        SampleItem item = fixtures.createSampleWithSampleItem("RQP");
        fixtures.createAnalysis(item, testService.get(testId));
        entityManager.flush();
        return item.getSample().getAccessionNumber();
    }

    private void storedResultIn(ResultRun target, String testId) {
        SampleItem item = fixtures.createSampleWithSampleItem("RQP");
        Analysis analysis = fixtures.createAnalysis(item, testService.get(testId));
        jdbc.update(
                "INSERT INTO clinlims.result (id, analysis_id, value, run_id, lastupdated) VALUES"
                        + " (nextval('clinlims.result_seq'), ?::numeric, '42', ?, now())",
                analysis.getId(), target.getId());
        entityManager.flush();
    }
}
