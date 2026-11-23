package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.form.WaitingRun;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1200 slice 1: an analyzer message opens a run, the run is listed and read
 * by whoever may see its values (D-043), and a pending test whose value waits
 * in an open run is named as waiting there.
 */
@Transactional
public class ResultRunServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNIT_A = "97650";
    private static final String UNIT_B = "97660";
    private static final String TEST_A = "97651";
    private static final String TEST_B = "97661";
    private static final String ANALYZER = "97670";
    private static final String CONNECTION = "conn-run-1200";

    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private AnalyzerResultsService analyzerResultsService;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private javax.sql.DataSource dataSource;
    @PersistenceContext
    private EntityManager entityManager;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        labUnit(UNIT_A, "Run unit A");
        labUnit(UNIT_B, "Run unit B");
        test(TEST_A, UNIT_A, "Run test A");
        test(TEST_B, UNIT_B, "Run test B");
        jdbc.update("INSERT INTO clinlims.analyzer (id, name, is_active, test_unit_ids, last_updated)"
                + " VALUES (?::numeric, 'Run analyzer 1200', true, ?, NOW())", ANALYZER, UNIT_A + "," + UNIT_B);
    }

    private void labUnit(String id, String name) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?::numeric, ?, NOW())",
                id, name);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, name_localization_id, is_active,"
                        + " domain, lastupdated) VALUES (?::numeric, ?, ?, ?::numeric, 'Y', 'CLINICAL', NOW())",
                id, name, name, id);
    }

    private void test(String id, String unit, String name) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, orderable,"
                + " test_section_id, lastupdated) VALUES (?::numeric, ?, ?, 'Y', ?, 'CLINICAL', true, ?::numeric,"
                + " NOW())", id, name, name, UUID.randomUUID().toString(), unit);
    }

    private AnalyzerResults row(String accession, String testId, String value, boolean control) {
        AnalyzerResults row = new AnalyzerResults();
        row.setAnalyzerId(ANALYZER);
        row.setAccessionNumber(accession);
        row.setTestId(testId);
        row.setTestName(testId == null ? "Unmapped code" : "Test " + testId);
        row.setResult(value);
        row.setRawResultValue(value);
        row.setIsControl(control);
        row.setInstrumentOperator("op-7");
        row.setRawSubIdentity("");
        return row;
    }

    private ResultRun open(String messageId, List<AnalyzerResults> rows) {
        Analyzer analyzer = analyzerService.getWithMapping(ANALYZER).orElseThrow();
        ResultRun run = resultRunService.openAnalyzerRun(analyzer, CONNECTION, messageId, rows).orElseThrow();
        analyzerResultsService.insertAnalyzerResults(rows, "1");
        entityManager.flush();
        return run;
    }

    private static RunViewer seesUnitA() {
        return new RunViewer(Set.of(TEST_A), Set.of(UNIT_A));
    }

    @Test
    public void aMessageOpensOneRunAndNamesItOnEveryValue() {
        List<AnalyzerResults> rows = List.of(row("RUN1200A", TEST_A, "5.1", false),
                row("RUN1200B", TEST_B, "6.2", false));

        ResultRun run = open("msg-1", rows);

        assertEquals("OPEN",
                jdbc.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, run.getId()));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.analyzer_results WHERE run_id = ?", Integer.class, run.getId()));
        assertEquals("op-7", run.getOperatorName());
        assertEquals(2, run.getRowCount());
        assertEquals("RUN-" + run.getId(), run.getLabel());
        assertNull("an analyzer working two lab units does not pin the run to one", run.getTestSectionId());
    }

    /**
     * One run per analyzer message: values staged again for a message whose run
     * already exists (its receipt was removed) join that run and reopen it.
     */
    @Test
    public void valuesStagedAgainForAMessageJoinItsRunAndReopenIt() {
        List<AnalyzerResults> first = List.of(row("RUN1200X", TEST_A, "1", false));
        ResultRun run = open("msg-again", first);
        analyzerResultsService.delete(first.get(0).getId(), "1");
        entityManager.flush();

        ResultRun again = open("msg-again", List.of(row("RUN1200Y", TEST_A, "2", false)));

        assertEquals(run.getId(), again.getId());
        assertEquals("OPEN",
                jdbc.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, run.getId()));
        assertEquals(Integer.valueOf(2), jdbc.queryForObject("SELECT row_count FROM clinlims.result_run WHERE id = ?",
                Integer.class, run.getId()));
        assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.result_run WHERE source_message_id = 'msg-again'", Integer.class));
    }

    @Test
    public void valuesCapturedForSetupOpenNoRun() {
        AnalyzerResults capture = row("RUN1200C", TEST_A, "1", false);
        capture.setImportIssueReason(AnalyzerResults.IMPORT_ISSUE_SETUP_CAPTURE);
        Analyzer analyzer = analyzerService.getWithMapping(ANALYZER).orElseThrow();

        Optional<ResultRun> run = resultRunService.openAnalyzerRun(analyzer, CONNECTION, "msg-setup",
                new ArrayList<>(List.of(capture)));

        assertFalse(run.isPresent());
        assertNull(capture.getRunId());
    }

    @Test
    public void theRunsChipListsOnlyRunsWithAValueTheUserMaySee() {
        ResultRun mixed = open("msg-2",
                List.of(row("RUN1200D", TEST_A, "1", false), row("RUN1200E", TEST_B, "2", false)));
        ResultRun onlyB = open("msg-3", List.of(row("RUN1200F", TEST_B, "3", false)));

        ResultRunListResponse forA = resultRunService.listActiveRuns(seesUnitA(), null);

        assertTrue(forA.runs().stream().anyMatch(run -> run.id().equals(mixed.getId())));
        assertFalse("no value of this run is the user's",
                forA.runs().stream().anyMatch(run -> run.id().equals(onlyB.getId())));
        assertEquals(forA.runs().size(), forA.open());
        assertEquals(0, forA.held());
    }

    @Test
    public void aLabUnitFilterKeepsRunsWithAValueOfThatUnit() {
        ResultRun mixed = open("msg-4",
                List.of(row("RUN1200G", TEST_A, "1", false), row("RUN1200H", TEST_B, "2", false)));
        RunViewer both = new RunViewer(Set.of(TEST_A, TEST_B), Set.of(UNIT_A, UNIT_B));
        ResultRun onlyA = open("msg-5", List.of(row("RUN1200I", TEST_A, "1", false)));

        ResultRunListResponse unitB = resultRunService.listActiveRuns(both, UNIT_B);

        assertTrue(unitB.runs().stream().anyMatch(run -> run.id().equals(mixed.getId())));
        assertFalse(unitB.runs().stream().anyMatch(run -> run.id().equals(onlyA.getId())));
    }

    @Test
    public void aRunTheUserMayNotSeeHasNoHeaderRowsOrRawMessage() {
        ResultRun onlyB = open("msg-6", List.of(row("RUN1200J", TEST_B, "3", false)));

        assertFalse(resultRunService.getHeader(onlyB.getId(), seesUnitA()).isPresent());
        assertFalse(resultRunService.getRows(onlyB.getId(), seesUnitA(), 1, 50).isPresent());
        assertFalse(resultRunService.getRawMessage(onlyB.getId(), seesUnitA()).isPresent());
    }

    @Test
    public void theHeaderCountsWorkedValuesAgainstEveryValueTheRunHeld() {
        List<AnalyzerResults> rows = List.of(row("RUN1200K", TEST_A, "1", false), row("RUN1200L", TEST_A, "2", false),
                row("RUN1200M", TEST_A, "3", false));
        ResultRun run = open("msg-7", rows);
        analyzerResultsService.delete(rows.get(0).getId(), "1");
        entityManager.flush();

        ResultRunHeader header = resultRunService.getHeader(run.getId(), seesUnitA()).orElseThrow();

        assertEquals(1, header.dispositioned());
        assertEquals(3, header.total());
        assertEquals("OPEN", header.state());
        assertEquals("Run analyzer 1200", header.analyzerName());
        assertEquals("op-7", header.operatorName());
        assertEquals(List.of("Run unit A", "Run unit B"), header.labUnits());
        assertFalse("no receipt was attached", header.rawAvailable());
    }

    @Test
    public void rowsPageOverTheValuesTheUserMaySee() {
        List<AnalyzerResults> rows = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            rows.add(row("RUN1200P" + i, TEST_A, String.valueOf(i), false));
        }
        rows.add(row("RUN1200Q", TEST_B, "99", false));
        ResultRun run = open("msg-8", rows);

        ResultRunRowsPage second = resultRunService.getRows(run.getId(), seesUnitA(), 2, 25).orElseThrow();

        assertEquals("only the user's 30 values count", 30, second.totalRows());
        assertEquals(5, second.rows().size());
        assertEquals(2, second.page());
        assertEquals("RUN1200P25", second.rows().get(0).accessionNumber());
        assertEquals("an unknown page size falls back to 50", 50,
                resultRunService.getRows(run.getId(), seesUnitA(), 1, 7).orElseThrow().pageSize());
    }

    @Test
    public void aValueNoTestIsMatchedToShowsToWhoeverWorksAnAnalyzerLabUnit() {
        ResultRun run = open("msg-9", List.of(row("RUN1200R", null, "4", false)));

        assertTrue(resultRunService.getHeader(run.getId(), seesUnitA()).isPresent());
        assertFalse(resultRunService.getHeader(run.getId(), new RunViewer(Set.of(TEST_A), Set.of("1"))).isPresent());
    }

    @Test
    public void aPendingTestWithAValueInAnOpenRunIsNamedAsWaitingThere() {
        ResultRun run = open("msg-10",
                List.of(row("RUN1200S", TEST_A, "4", false), row("RUN1200T", TEST_A, "QC", true)));

        Map<String, WaitingRun> waiting = resultRunService.findWaiting(List.of("RUN1200S", "RUN1200T"));

        assertEquals(run.getId(), waiting.get(ResultRunService.waitingKey("RUN1200S", TEST_A)).runId());
        assertNull("a control is not a patient value", waiting.get(ResultRunService.waitingKey("RUN1200T", TEST_A)));
    }

    @Test
    public void worklistRowsAreMarkedOnlyWhilePending() {
        ResultRun run = open("msg-11",
                List.of(row("RUN1200U", TEST_A, "4", false), row("RUN1200V", TEST_A, "5", false)));
        TestResultItem pending = item("RUN1200U", null);
        TestResultItem resulted = item("RUN1200V", "5");

        resultRunService.markWaiting(List.of(pending, resulted));

        assertEquals("RUN-" + run.getId(), pending.getWaitingRun().label());
        assertNull("a row already resulted is not waiting", resulted.getWaitingRun());
    }

    /**
     * A test with parts is waiting part by part: the part a run holds a value for
     * says so, the main value names the primary part, and a part the run sent
     * nothing for is entered at the bench as usual.
     */
    @Test
    public void eachPartOfATestIsMarkedWaitingOnlyForTheValueTheRunHoldsForIt() {
        jdbc.update("INSERT INTO clinlims.test_result_component (id, test_id, code, label, display_order, is_active,"
                + " lastupdated, is_primary) VALUES ('rrs-main', ?::numeric, 'VL', 'Viral load', 0, 'Y', NOW(), true),"
                + " ('rrs-ct', ?::numeric, 'CT', 'Ct', 1, 'Y', NOW(), false),"
                + " ('rrs-call', ?::numeric, 'CALL', 'Call', 2, 'Y', NOW(), false)", TEST_A, TEST_A, TEST_A);
        AnalyzerResults main = row("RUN1200P", TEST_A, "1009.64", false);
        AnalyzerResults ct = row("RUN1200P", TEST_A, "33.0", false);
        ct.setComponentId("rrs-ct");
        ResultRun run = open("msg-13", List.of(main, ct));
        TestResultItem mainRow = item("RUN1200P", null);
        mainRow.setTestResultComponentId("rrs-main");
        TestResultItem ctRow = item("RUN1200P", null);
        ctRow.setTestResultComponentId("rrs-ct");
        TestResultItem callRow = item("RUN1200P", null);
        callRow.setTestResultComponentId("rrs-call");

        resultRunService.markWaiting(List.of(mainRow, ctRow, callRow));

        assertEquals("RUN-" + run.getId(), mainRow.getWaitingRun().label());
        assertEquals("RUN-" + run.getId(), ctRow.getWaitingRun().label());
        assertNull("the run sent nothing for the call", callRow.getWaitingRun());
    }

    @Test
    public void aCompletedRunHoldsNothingWaiting() {
        List<AnalyzerResults> rows = List.of(row("RUN1200W", TEST_A, "4", false));
        ResultRun run = open("msg-12", rows);
        analyzerResultsService.delete(rows.get(0).getId(), "1");
        entityManager.flush();

        assertEquals("COMPLETE",
                jdbc.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, run.getId()));
        assertTrue(resultRunService.findWaiting("RUN1200W", TEST_A).isEmpty());
        assertFalse("a completed run leaves the chip", resultRunService.listActiveRuns(seesUnitA(), null).runs()
                .stream().anyMatch(summary -> summary.id().equals(run.getId())));
    }

    private static TestResultItem item(String accession, String value) {
        TestResultItem item = new TestResultItem();
        item.setAccessionNumber(accession);
        item.setTestId(TEST_A);
        item.setResultValue(value);
        return item;
    }
}
