package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.fixture.MicrobiologyTestFixtures;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultrun.form.ResultRunChip;
import org.openelisglobal.resultrun.form.ResultRunException;
import org.openelisglobal.resultrun.form.ResultRunExceptionKind;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunRow;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.form.RunOrderMatch;
import org.openelisglobal.resultrun.form.RunOrderState;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1200 FR-E4: a run lists the values it cannot accept until someone
 * resolves them, a lab number that names no order, a test not on its order or
 * already resulted there, and an analyzer code with no mapping, and each is
 * resolved on the run: found on another order, held or ignored with a reason,
 * touching only its own values.
 */
@Transactional
public class RunExceptionServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNIT = "97850";
    private static final String TEST = "97851";
    private static final String OTHER_TEST = "97852";
    private static final String ANALYZER = "97870";
    private static final String UNKNOWN_LAB_NUMBER = "NOORDER-1200";
    private static final String UNMATCHED_KEY = "UNMATCHED|" + UNKNOWN_LAB_NUMBER;

    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private RunExceptionService runExceptionService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private MicrobiologyTestFixtures fixtures;
    @Autowired
    private TestService testService;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;
    private Patient patient;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?::numeric, ?, NOW())",
                UNIT, "Exception unit");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, name_localization_id, is_active,"
                + " domain, lastupdated) VALUES (?::numeric, 'Exception unit', 'Exception unit', ?::numeric, 'Y',"
                + " 'CLINICAL', NOW())", UNIT, UNIT);
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                + " test_section_id, lastupdated) VALUES (?::numeric, 'Exception test', 'Exception test', 'Y', ?,"
                + " 'CLINICAL', true, ?::numeric, NOW())", TEST, UUID.randomUUID().toString(), UNIT);
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                + " test_section_id, lastupdated) VALUES (?::numeric, 'Other exception test', 'Other exception test',"
                + " 'Y', ?, 'CLINICAL', true, ?::numeric, NOW())", OTHER_TEST, UUID.randomUUID().toString(), UNIT);
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, low_normal, high_normal,"
                        + " low_valid, high_valid, low_critical, high_critical, lastupdated) VALUES"
                        + " (nextval('clinlims.result_limits_seq'), ?::numeric, 4, 10, 20, 0, 1000, 5, 50, NOW())",
                TEST);
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, test_unit_ids, last_updated)"
                + " VALUES (?::numeric, 'Exception analyzer 1200', true, ?, NOW())", ANALYZER, UNIT);
        patient = fixtures.createPatient("EXC");
    }

    @Test
    public void aLabNumberThatNamesNoOrderIsOneUnmatchedExceptionForAllItsValues() {
        AnalyzerResults first = valueFor(UNKNOWN_LAB_NUMBER, "15");
        AnalyzerResults second = valueFor(UNKNOWN_LAB_NUMBER, "16");
        second.setInstrumentPatientName("Smith^Robert");
        second.setTestId(OTHER_TEST);
        second.setTestName("Other exception test");
        AnalyzerResults ordered = valueFor(orderWithTheTest().getSample().getAccessionNumber(), "15");
        ResultRun run = open("m-unmatched", List.of(first, second, ordered));

        ResultRunHeader header = resultRunService.getHeader(run.getId(), viewer()).orElseThrow();

        assertEquals(1, header.exceptions().size());
        ResultRunException unmatched = header.exceptions().get(0);
        assertEquals(ResultRunExceptionKind.UNMATCHED, unmatched.kind());
        assertEquals(UNKNOWN_LAB_NUMBER, unmatched.accessionNumber());
        assertEquals(Set.of(first.getId(), second.getId()), Set.copyOf(unmatched.rowIds()));
        assertEquals("the patient the analyzer sent, to compare with the order found", "Smith^Robert",
                unmatched.reportedPatient());
        assertEquals("only the value on its order is ready", 1, header.chipCounts().normal());
        assertEquals(2, header.chipCounts().exceptions());
    }

    @Test
    public void aTestThatIsNotOnItsOrderIsAnUnorderedException() {
        SampleItem withoutTheTest = fixtures.createSampleWithSampleItem("EXC");
        AnalyzerResults unordered = valueFor(withoutTheTest.getSample().getAccessionNumber(), "15");
        ResultRun run = open("m-unordered", List.of(unordered));

        ResultRunException exception = onlyException(run);

        assertEquals(ResultRunExceptionKind.UNORDERED, exception.kind());
        assertEquals(TEST, exception.testId());
        ResultRunRow row = resultRunService.getRows(run.getId(), viewer(), ResultRunChip.ALL, 1, 25).orElseThrow()
                .rows().get(0);
        assertEquals(ResultRunRowCategory.EXCEPTION, row.category());
    }

    @Test
    public void aTestWhoseOnlyAnalysisWasCancelledIsNotOrdered() {
        SampleItem item = orderWithTheTest();
        String cancelled = statusService.getStatusID(AnalysisStatus.Canceled);
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, name, code, status_type, is_active, display_key,"
                + " description, lastupdated) VALUES (CAST(? AS numeric), 'Test Canceled', 5, 'ANALYSIS', 'Y',"
                + " 'status.test.canceled', 'Test Canceled', NOW()) ON CONFLICT DO NOTHING", cancelled);
        jdbc.update("UPDATE clinlims.analysis SET status_id = ?::numeric WHERE sampitem_id = ?::numeric", cancelled,
                item.getId());
        entityManager.clear();
        ResultRun run = open("m-cancelled", List.of(valueFor(item.getSample().getAccessionNumber(), "15")));

        assertEquals(ResultRunExceptionKind.UNORDERED, onlyException(run).kind());
    }

    @Test
    public void anUnmappedCodeIsAnUnmappedExceptionThatOpensTheAnalyzersMapping() {
        AnalyzerResults unmapped = valueFor(orderWithTheTest().getSample().getAccessionNumber(), "7.1");
        unmapped.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        unmapped.setRawTestCode("TBIL-2");
        unmapped.setSourceProfileId("profile-1200");
        unmapped.setSourceProfileRevision(3);
        unmapped.setTestId(null);
        AnalyzerResults placement = valueFor(orderWithTheTest().getSample().getAccessionNumber(), "15");
        placement.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_AWAITING_PLACEMENT);
        ResultRun run = open("m-unmapped", List.of(unmapped, placement));

        ResultRunException exception = onlyException(run);

        assertEquals(ResultRunExceptionKind.UNMAPPED, exception.kind());
        assertEquals("TBIL-2", exception.code());
        assertEquals(new ResultRunException.MappingTarget(ANALYZER, "TBIL-2", "7.1"), exception.mapping());
        assertEquals("a value awaiting placement is resolved on the import page, not listed here", 2,
                resultRunService.getHeader(run.getId(), viewer()).orElseThrow().chipCounts().exceptions());
    }

    @Test
    public void matchingPlacesTheValuesOnTheOrderFoundSoTheyAreAcceptedThere() {
        AnalyzerResults misread = valueFor(UNKNOWN_LAB_NUMBER, "15");
        ResultRun run = open("m-match", List.of(misread));
        SampleItem item = newSampleItemOfPatient();
        Analysis analysis = fixtures.createAnalysis(item, testService.get(TEST));
        entityManager.flush();
        String target = item.getSample().getAccessionNumber();

        runExceptionService.match(run.getId(), viewer(), List.of(misread.getId()), UNMATCHED_KEY, target,
                "Tube label misread by the scanner", "1");
        entityManager.flush();

        assertEquals(List.of(), resultRunService.getHeader(run.getId(), viewer()).orElseThrow().exceptions());
        AnalyzerResults moved = analyzerResultsService.get(misread.getId());
        assertEquals(target, moved.getAccessionNumber());
        assertTrue("the value is accepted from the run on the order it was matched to",
                resultRunService.stagedValueFor(misread.getId(), analysis, null).isPresent());
        Map<String, Object> event = jdbc.queryForMap(
                "SELECT event, row_id, detail FROM clinlims.result_run_event WHERE run_id = ? AND row_id = CAST(? AS numeric)",
                run.getId(), misread.getId());
        assertEquals("exceptionMatched", event.get("event"));
        assertEquals(UNKNOWN_LAB_NUMBER + " -> " + target + ": Tube label misread by the scanner", event.get("detail"));
    }

    @Test
    public void matchingNeedsAReasonAndAnOrderThatExists() {
        AnalyzerResults misread = valueFor(UNKNOWN_LAB_NUMBER, "15");
        ResultRun run = open("m-match-refused", List.of(misread));
        String target = orderWithTheTest().getSample().getAccessionNumber();

        assertThrows(IllegalArgumentException.class, () -> runExceptionService.match(run.getId(), viewer(),
                List.of(misread.getId()), UNMATCHED_KEY, target, "  ", "1"));
        assertThrows(IllegalArgumentException.class, () -> runExceptionService.match(run.getId(), viewer(),
                List.of(misread.getId()), UNMATCHED_KEY, "NOORDER-OTHER", "Misread", "1"));

        assertEquals(UNKNOWN_LAB_NUMBER, analyzerResultsService.get(misread.getId()).getAccessionNumber());
    }

    @Test
    public void aValueThatIsNotAnExceptionIsNeverMatchedOrIgnoredFromTheBlock() {
        AnalyzerResults ready = valueFor(orderWithTheTest().getSample().getAccessionNumber(), "15");
        ResultRun run = open("m-not-exception", List.of(ready));
        String other = orderWithTheTest().getSample().getAccessionNumber();

        assertThrows(IllegalStateException.class, () -> runExceptionService.match(run.getId(), viewer(),
                List.of(ready.getId()), UNMATCHED_KEY, other, "Misread", "1"));
        assertThrows(IllegalStateException.class, () -> runExceptionService.ignore(run.getId(), viewer(),
                List.of(ready.getId()), UNMATCHED_KEY, "Not needed", "1"));

        assertNotNull(analyzerResultsService.get(ready.getId()));
    }

    @Test
    public void ignoringTakesOnlyThoseValuesOffTheRunAndTheRunCompletesWhenNoneAreLeft() {
        AnalyzerResults unmatched = valueFor(UNKNOWN_LAB_NUMBER, "15");
        AnalyzerResults ready = valueFor(orderWithTheTest().getSample().getAccessionNumber(), "15");
        ResultRun run = open("m-ignore", List.of(unmatched, ready));

        runExceptionService.ignore(run.getId(), viewer(), List.of(unmatched.getId()), UNMATCHED_KEY, "Calibration tube",
                "1");
        entityManager.flush();

        assertEquals(List.of(ready.getId()),
                jdbc.queryForList("SELECT CAST(id AS VARCHAR) FROM clinlims.analyzer_results WHERE run_id = ?",
                        String.class, run.getId()));
        assertEquals(ResultRunState.OPEN, resultRunService.get(run.getId()).getState());
        assertEquals("exceptionIgnored",
                jdbc.queryForObject(
                        "SELECT event FROM clinlims.result_run_event WHERE run_id = ? AND row_id = CAST(? AS numeric)",
                        String.class, run.getId(), unmatched.getId()));

        ResultRun onlyExceptions = open("m-ignore-all", List.of(valueFor(UNKNOWN_LAB_NUMBER, "15")));
        ResultRunException only = onlyException(onlyExceptions);
        runExceptionService.ignore(onlyExceptions.getId(), viewer(), only.rowIds(), only.key(), "Calibration tube",
                "1");
        entityManager.flush();

        assertEquals(ResultRunState.COMPLETE, resultRunService.get(onlyExceptions.getId()).getState());
    }

    @Test
    public void aHoldKeepsTheValuesBlockedAndTheRunShowsItsReasonUntilTheyAreMatched() {
        AnalyzerResults unmatched = valueFor(UNKNOWN_LAB_NUMBER, "15");
        ResultRun run = open("m-hold", List.of(unmatched));

        runExceptionService.hold(run.getId(), viewer(), List.of(unmatched.getId()), UNMATCHED_KEY,
                "Waiting for reception", "1");
        entityManager.flush();

        ResultRunException held = onlyException(run);
        assertEquals("Waiting for reception", held.hold().reason());
        assertNotNull(held.hold().heldAt());
        assertNotNull(analyzerResultsService.get(unmatched.getId()));

        String withoutTheTest = fixtures.createSampleWithSampleItem("EXC").getSample().getAccessionNumber();
        runExceptionService.match(run.getId(), viewer(), List.of(unmatched.getId()), UNMATCHED_KEY, withoutTheTest,
                "Registered at reception", "1");
        entityManager.flush();

        ResultRunException moved = onlyException(run);
        assertEquals(ResultRunExceptionKind.UNORDERED, moved.kind());
        assertNull("the hold was on the unknown lab number, not on the order it was matched to", moved.hold());
    }

    @Test
    public void aHoldStaysOnTheRecordItWasPutOnWhenTheValuesCauseChangesAnotherWay() {
        AnalyzerResults unmapped = valueFor(fixtures.createSampleWithSampleItem("EXC").getSample().getAccessionNumber(),
                "7.1");
        unmapped.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_UNKNOWN_TEST);
        unmapped.setRawTestCode("TBIL-2");
        unmapped.setTestId(null);
        ResultRun run = open("m-hold-record", List.of(unmapped));
        String key = onlyException(run).key();
        runExceptionService.hold(run.getId(), viewer(), List.of(unmapped.getId()), key,
                "Waiting for the vendor's code list", "1");
        entityManager.flush();
        assertEquals("Waiting for the vendor's code list", onlyException(run).hold().reason());
        assertEquals(key, jdbc.queryForObject("SELECT record_key FROM clinlims.result_run_event WHERE run_id = ?"
                + " AND row_id = CAST(? AS numeric)", String.class, run.getId(), unmapped.getId()));

        jdbc.update("UPDATE clinlims.analyzer_results SET test_id = CAST(? AS numeric), import_issue_reason = NULL"
                + " WHERE id = CAST(? AS numeric)", TEST, unmapped.getId());
        entityManager.clear();

        ResultRunException mapped = onlyException(run);
        assertEquals("once mapped, the value's test is not on its order", ResultRunExceptionKind.UNORDERED,
                mapped.kind());
        assertNull("the hold was on the unmapped code, not on the unordered test", mapped.hold());
    }

    @Test
    public void aValueWhoseTestAlreadyHasAResultForItsPartIsAResultedException() {
        SampleItem resulted = orderWithTheTest();
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated) VALUES"
                + " (nextval('clinlims.result_seq'), (SELECT id FROM clinlims.analysis WHERE sampitem_id ="
                + " CAST(? AS numeric)), '60', 'N', NOW())", resulted.getId());
        SampleItem blankResult = orderWithTheTest();
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated) VALUES"
                + " (nextval('clinlims.result_seq'), (SELECT id FROM clinlims.analysis WHERE sampitem_id ="
                + " CAST(? AS numeric)), '', 'N', NOW())", blankResult.getId());
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE id = 'rrc-exc-part'");
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rrc-exc-part', CAST(? AS numeric), 'PART', 'Other part', 2, 'Y',"
                + " NOW(), false)", TEST);
        AnalyzerResults again = valueFor(resulted.getSample().getAccessionNumber(), "61");
        AnalyzerResults otherPart = valueFor(resulted.getSample().getAccessionNumber(), "3");
        otherPart.setComponentId("rrc-exc-part");
        AnalyzerResults awaiting = valueFor(blankResult.getSample().getAccessionNumber(), "15");
        ResultRun run = open("m-resulted", List.of(again, otherPart, awaiting));

        ResultRunException exception = onlyException(run);

        assertEquals(ResultRunExceptionKind.RESULTED, exception.kind());
        assertEquals(List.of(again.getId()), exception.rowIds());
        assertEquals("a blank result and a part no result fills still wait for the value", 2,
                resultRunService.getHeader(run.getId(), viewer()).orElseThrow().chipCounts().normal());
        assertEquals(RunOrderState.RESULTED, runExceptionService
                .ordersMatching(resulted.getSample().getAccessionNumber(), TEST, null).get(0).test());
        assertEquals(RunOrderState.AWAITING, runExceptionService
                .ordersMatching(resulted.getSample().getAccessionNumber(), TEST, "rrc-exc-part").get(0).test());

        runExceptionService.ignore(run.getId(), viewer(), exception.rowIds(), exception.key(),
                "Rerun of a reported tube", "1");
        entityManager.flush();
        assertEquals(List.of(), resultRunService.getHeader(run.getId(), viewer()).orElseThrow().exceptions());
    }

    @Test
    public void aDecisionOnARecordSomeoneElseResolvedSinceThePageLoadedIsRefused() {
        AnalyzerResults unmatched = valueFor(UNKNOWN_LAB_NUMBER, "15");
        ResultRun run = open("m-stale", List.of(unmatched));
        String withoutTheTest = fixtures.createSampleWithSampleItem("EXC").getSample().getAccessionNumber();
        runExceptionService.match(run.getId(), viewer(), List.of(unmatched.getId()), UNMATCHED_KEY, withoutTheTest,
                "Registered at reception", "1");
        entityManager.flush();

        assertThrows("the page still shows the unknown lab number", IllegalStateException.class,
                () -> runExceptionService.ignore(run.getId(), viewer(), List.of(unmatched.getId()), UNMATCHED_KEY,
                        "Not ours", "1"));

        assertNotNull(analyzerResultsService.get(unmatched.getId()));
        assertEquals(ResultRunExceptionKind.UNORDERED, onlyException(run).kind());
    }

    @Test
    public void ordersAreFoundByTheStartOfTheirLabNumberAndSayWhetherTheTestIsOnThem() {
        SampleItem withTheTest = orderWithTheTest();
        String accession = withTheTest.getSample().getAccessionNumber();
        entityManager.flush();

        List<RunOrderMatch> found = runExceptionService.ordersMatching(accession, TEST, null);

        assertEquals(1, found.size());
        assertEquals(accession, found.get(0).accessionNumber());
        assertEquals(RunOrderState.AWAITING, found.get(0).test());
        assertEquals(RunOrderState.NOT_ORDERED, runExceptionService.ordersMatching(accession, "1", null).get(0).test());
        assertEquals("fewer than three characters find nothing", List.of(),
                runExceptionService.ordersMatching(accession.substring(0, 2), TEST, null));
    }

    private ResultRunException onlyException(ResultRun run) {
        List<ResultRunException> exceptions = resultRunService.getHeader(run.getId(), viewer()).orElseThrow()
                .exceptions();
        assertEquals(1, exceptions.size());
        return exceptions.get(0);
    }

    private SampleItem orderWithTheTest() {
        SampleItem item = newSampleItemOfPatient();
        fixtures.createAnalysis(item, testService.get(TEST));
        entityManager.flush();
        return item;
    }

    private SampleItem newSampleItemOfPatient() {
        SampleItem item = fixtures.createSampleWithSampleItem("EXC");
        jdbc.update(
                "INSERT INTO clinlims.sample_human (id, samp_id, patient_id, lastupdated) VALUES"
                        + " (nextval('clinlims.sample_human_seq'), ?::numeric, ?::numeric, NOW())",
                item.getSample().getId(), patient.getId());
        return item;
    }

    private AnalyzerResults valueFor(String accessionNumber, String value) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(ANALYZER);
        row.setAccessionNumber(accessionNumber);
        row.setTestId(TEST);
        row.setTestName("Exception test");
        row.setResult(value);
        row.setRawResultValue(value);
        row.setResultType("N");
        row.setIsControl(false);
        row.setRawSubIdentity("");
        return row;
    }

    private ResultRun open(String messageId, List<AnalyzerResults> rows) {
        Analyzer analyzer = analyzerService.getWithMapping(ANALYZER).orElseThrow();
        ResultRun run = resultRunService.openAnalyzerRun(analyzer, "conn-exception-1200", messageId, rows)
                .orElseThrow();
        analyzerResultsService.insertAnalyzerResults(rows, "1");
        entityManager.flush();
        return run;
    }

    private static RunViewer viewer() {
        return new RunViewer(Set.of(TEST, OTHER_TEST), Set.of(UNIT));
    }
}
