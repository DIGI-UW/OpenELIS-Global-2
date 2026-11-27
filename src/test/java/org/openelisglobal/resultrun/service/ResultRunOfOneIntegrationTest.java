package org.openelisglobal.resultrun.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.service.InventoryManagementService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultrun.form.RunHoldCause;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.valueholder.RunTestAction;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1200 FR-B3, FR-D7: a result saved on a row in no run is a run of one, and
 * a control recorded on that row before the save opens it, holds the row while
 * it fails and is completed by the save.
 */
public class ResultRunOfOneIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "1";

    @Autowired
    private ResultRunService resultRunService;
    @Autowired
    private RunQcService runQcService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private InventoryLotService inventoryLotService;
    @Autowired
    private InventoryManagementService inventoryManagementService;

    private String testId;
    private String testSectionId;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/panel-item.xml");
        org.openelisglobal.test.valueholder.Test test = testService.getAllActiveTests(false).stream()
                .filter(candidate -> candidate.getTestSection() != null).findFirst().orElseThrow();
        testId = test.getId();
        testSectionId = test.getTestSection().getId();
    }

    @Test
    public void aFirstValueSavedOnARowInNoRunIsARunOfOne() {
        String analysisId = createAnalysis();
        Result saved = storeResult(analysisId, "81");

        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));

        Long runId = runOf(saved);
        assertNotNull("the saved result records into a run of one", runId);
        Map<String, Object> run = jdbcTemplate.queryForMap(
                "SELECT source, state, CAST(analysis_id AS VARCHAR) AS analysis, CAST(test_section_id AS VARCHAR)"
                        + " AS unit, row_count FROM clinlims.result_run WHERE id = ?",
                runId);
        assertEquals("RESULT_ENTRY", run.get("source"));
        assertEquals("the run is completed in the same save", "COMPLETE", run.get("state"));
        assertEquals(analysisId, run.get("analysis"));
        assertEquals(testSectionId, run.get("unit"));
        assertEquals(1, ((Number) run.get("row_count")).intValue());
        assertEquals(Map.of(saved.getId(), runId), resultRunService.runsOfResults(List.of(saved.getId())));
    }

    @Test
    public void anEditOfAStoredResultMakesNoRun() {
        Result edited = storeResult(createAnalysis(), "81");

        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(), List.of(edited)));

        assertNull(runOf(edited));
    }

    @Test
    public void aControlRecordedOnARowOpensItsRunOfOneWhichHoldsTheRowUntilDecidedAndTheSaveCompletes() {
        String analysisId = createAnalysis();

        QCResult control = resultRunService.recordRowControl(analysisId, invalidControl(), viewer(), ACTOR);

        Long runId = control.getRunId();
        assertNotNull("the control opens the row's run of one", runId);
        assertEquals("HELD", stateOf(runId));
        assertEquals("a failed control holds the row at entry", Map.of(analysisId, runId),
                runQcService.entryHoldingRuns(List.of(analysisId)));
        assertEquals("a second control goes to the same run", runId,
                resultRunService.recordRowControl(analysisId, invalidControl(), viewer(), ACTOR).getRunId());

        runQcService.decide(runId, testId, RunTestAction.ACCEPT_DESPITE, "Control repeated and passed", true, ACTOR);
        assertTrue(runQcService.entryHoldingRuns(List.of(analysisId)).isEmpty());
        Result saved = storeResult(analysisId, "82");
        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));

        assertEquals("the save records into the run the control opened", runId, runOf(saved));
        assertEquals("COMPLETE", stateOf(runId));
    }

    @Test
    public void aRowWhoseResultIsStoredOrWhoseTestIsNotTheViewersOpensNoRun() {
        String resulted = createAnalysis();
        storeResult(resulted, "81");
        try {
            resultRunService.recordRowControl(resulted, invalidControl(), viewer(), ACTOR);
            fail("a stored row opens no run");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("already saved"));
        }
        try {
            resultRunService.recordRowControl(createAnalysis(), invalidControl(),
                    new RunViewer(Set.of(), Set.of(testSectionId)), ACTOR);
            fail("another unit's row opens no run");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not one of your lab units"));
        }
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*)::int FROM clinlims.result_run WHERE CAST(analysis_id AS VARCHAR) = ?",
                        Integer.class, resulted));
    }

    /**
     * FR-A3: a Retest or Reject decides a run of one's only row, so the run closes;
     * the row's next control and value open a fresh run, and the value is released
     * like any other rather than held by the decided run.
     */
    @Test
    public void aRetestOrRejectClosesTheRunOfOneSoTheNextControlAndValueOpenAFreshRun() {
        for (RunTestAction action : List.of(RunTestAction.RETEST, RunTestAction.REJECT)) {
            String analysisId = createAnalysis();
            Long decided = resultRunService.recordRowControl(analysisId, invalidControl(), viewer(), ACTOR).getRunId();

            runQcService.decide(decided, testId, action, null, false, ACTOR);
            assertEquals(action + " closes the run of one", "COMPLETE", stateOf(decided));
            assertTrue(runQcService.entryHoldingRuns(List.of(analysisId)).isEmpty());

            Long fresh = resultRunService.recordRowControl(analysisId, validControl(), viewer(), ACTOR).getRunId();
            assertNotEquals("the next control opens a fresh run", decided, fresh);
            Result saved = storeResult(analysisId, "79");
            resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));
            assertEquals("the value records into the fresh run", fresh, runOf(saved));
            assertTrue("the value is not held at validation", runQcService.holdingRuns(List.of(analysisId)).isEmpty());
        }
        String retested = createAnalysis();
        Long setAside = resultRunService.recordRowControl(retested, invalidControl(), viewer(), ACTOR).getRunId();
        runQcService.decide(setAside, testId, RunTestAction.RETEST, null, false, ACTOR);
        assertEquals("the retest sets the run's own analysis aside", List.of(retested),
                jdbcTemplate.queryForList("SELECT CAST(rr.analysis_id AS VARCHAR) FROM clinlims.result_run_retest rr"
                        + " JOIN clinlims.result_run_test_disposition d ON d.id = rr.disposition_id"
                        + " WHERE d.run_id = ?", String.class, setAside));
    }

    /**
     * FR-B3: a run of one opened by a control before the reagent use was recorded
     * takes the lot of that use when the save completes it.
     */
    @Test
    public void aRunOpenedByAControlBeforeTheReagentUseTakesItsLotAtTheSave() {
        String analysisId = createAnalysis();
        Long runId = resultRunService.recordRowControl(analysisId, validControl(), viewer(), ACTOR).getRunId();
        assertNull(jdbcTemplate.queryForObject("SELECT reagent_lot_id FROM clinlims.result_run WHERE id = ?",
                Long.class, runId));
        Long lotId = createLot();
        inventoryManagementService.consumeSelectedLot(lotId, 0.5, null, Long.valueOf(analysisId), ACTOR);

        Result saved = storeResult(analysisId, "81");
        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));

        assertEquals(runId, runOf(saved));
        assertEquals(lotId, jdbcTemplate.queryForObject("SELECT reagent_lot_id FROM clinlims.result_run WHERE id = ?",
                Long.class, runId));
    }

    /**
     * FR-C7, FR-H4: a run of one's control requirement is the test's. Under "Hold
     * when missing" the first value on a row in no run holds the row in its run of
     * one for the missing control; a control recorded on the row releases it and
     * the save completes that run. Without the policy nothing is held.
     */
    @Test
    public void aRequiredControlMissingUnderHoldHoldsTheRowInItsRunOfOne() {
        String analysisId = createAnalysis();
        resultRunService.holdRunsOfOneMissingControl(List.of(analysisId), ACTOR);
        assertEquals("no policy, no run", Integer.valueOf(0), runsOfAnalysis(analysisId));

        jdbcTemplate.update("INSERT INTO clinlims.test_qc_policy (test_id, control_required, control_when_missing,"
                + " last_updated) VALUES (?::numeric, true, 'HOLD', NOW())", testId);
        try {
            resultRunService.holdRunsOfOneMissingControl(List.of(analysisId), ACTOR);
            Long runId = runQcService.entryHoldingRuns(List.of(analysisId)).get(analysisId);
            assertNotNull("the row is held at entry", runId);
            assertEquals("HELD", stateOf(runId));
            assertEquals(RunHoldCause.QC_MISSING, runQcService.holdCauses(runId).get(testId));
            resultRunService.holdRunsOfOneMissingControl(List.of(analysisId), ACTOR);
            assertEquals("a second attempt holds the same run", Integer.valueOf(1), runsOfAnalysis(analysisId));

            assertEquals(runId,
                    resultRunService.recordRowControl(analysisId, validControl(), viewer(), ACTOR).getRunId());
            assertTrue("the control releases the row", runQcService.entryHoldingRuns(List.of(analysisId)).isEmpty());
            Result saved = storeResult(analysisId, "81");
            resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(saved), List.of()));
            assertEquals(runId, runOf(saved));
            assertEquals("COMPLETE", stateOf(runId));
        } finally {
            jdbcTemplate.update("DELETE FROM clinlims.test_qc_policy WHERE test_id = ?::numeric", testId);
        }
    }

    /**
     * FR-B3: the parts of one test saved one after another share its run of one.
     */
    @Test
    public void aLaterPartOfATestRecordsIntoTheRunOfOneOfItsFirst() {
        String analysisId = createAnalysis();
        Result first = storeResult(analysisId, "81");
        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(first), List.of()));
        Result second = storeResult(analysisId, "4.2");
        resultRunService.recordRunsOfOne(ACTOR).transactionalUpdate(saving(List.of(second), List.of()));

        assertNotNull(runOf(first));
        assertEquals(runOf(first), runOf(second));
        assertEquals(Integer.valueOf(1), runsOfAnalysis(analysisId));
    }

    /** Two controls recorded on one row at the same moment open one run. */
    @Test
    public void twoControlsRecordedOnARowAtOnceOpenOneRun() throws Exception {
        String analysisId = createAnalysis();
        ExecutorService benches = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Long>> controls = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            controls.add(benches.submit(() -> {
                ready.countDown();
                go.await();
                return resultRunService.recordRowControl(analysisId, validControl(), viewer(), ACTOR).getRunId();
            }));
        }
        ready.await();
        go.countDown();
        List<Long> runIds = new ArrayList<>();
        for (Future<Long> control : controls) {
            runIds.add(control.get(60, TimeUnit.SECONDS));
        }
        benches.shutdown();

        assertEquals(runIds.get(0), runIds.get(1));
        assertEquals(Integer.valueOf(1), runsOfAnalysis(analysisId));
    }

    @Test
    public void aRowThatDoesNotExistIsRefusedWithAMessage() {
        for (String analysisId : List.of("99999999", "abc")) {
            try {
                resultRunService.recordRowControl(analysisId, validControl(), viewer(), ACTOR);
                fail("an unknown row opens no run");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("not one of your lab units"));
            }
        }
    }

    private Integer runsOfAnalysis(String analysisId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*)::int FROM clinlims.result_run WHERE CAST(analysis_id AS VARCHAR) = ?", Integer.class,
                analysisId);
    }

    private Long createLot() {
        InventoryItem reagent = new InventoryItem();
        reagent.setFhirUuid(UUID.randomUUID());
        reagent.setCode("OGC1200-" + UUID.randomUUID().toString().substring(0, 8));
        reagent.setName("OGC-1200 run of one reagent " + UUID.randomUUID());
        reagent.setUnits("mL");
        reagent.setSysUserId(ACTOR);
        Long reagentId = inventoryItemService.insert(reagent);
        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(reagentId));
        lot.setLotNumber("R1-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusYears(1)));
        lot.setSysUserId(ACTOR);
        return inventoryLotService.insert(lot);
    }

    private BenchQCCaptureForm validControl() {
        return new BenchQCCaptureForm(QCSource.RDT, testId, testSectionId, null, "KIT-" + UUID.randomUUID(), null, null,
                QCQualitativeOutcome.VALID, null, null, LocalDateTime.now().minusMinutes(1), "OGC-1200 row control");
    }

    private BenchQCCaptureForm invalidControl() {
        return new BenchQCCaptureForm(QCSource.RDT, testId, testSectionId, null, "KIT-" + UUID.randomUUID(), null, null,
                QCQualitativeOutcome.INVALID, null, null, LocalDateTime.now().minusMinutes(1), "OGC-1200 row control");
    }

    private RunViewer viewer() {
        return new RunViewer(Set.of(testId), Set.of(testSectionId));
    }

    private Long runOf(Result result) {
        return jdbcTemplate.queryForObject("SELECT run_id FROM clinlims.result WHERE id = CAST(? AS numeric)",
                Long.class, result.getId());
    }

    private String stateOf(Long runId) {
        return jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId);
    }

    private Result storeResult(String analysisId, String value) {
        String id = jdbcTemplate.queryForObject("SELECT CAST(nextval('clinlims.result_seq') AS VARCHAR)", String.class);
        jdbcTemplate.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (CAST(? AS numeric), CAST(? AS numeric), ?, 'N', now())", id, analysisId, value);
        return resultService.get(id);
    }

    private IResultSaveService saving(List<Result> created, List<Result> modified) {
        return new IResultSaveService() {
            @Override
            public String getCurrentUserId() {
                return ACTOR;
            }

            @Override
            public List<ResultSet> getNewResults() {
                return created.stream().map(result -> new ResultSet(result, null, null, null, null, null, false))
                        .toList();
            }

            @Override
            public List<ResultSet> getModifiedResults() {
                return modified.stream().map(result -> new ResultSet(result, null, null, null, null, null, false))
                        .toList();
            }
        };
    }

    private String createAnalysis() {
        String statusId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.status_of_sample ORDER BY id LIMIT 1",
                String.class);
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());

        Sample sample = new Sample();
        sample.setAccessionNumber("OGC1200" + UUID.randomUUID().toString().substring(0, 6));
        sample.setDomain("H");
        sample.setEnteredDate(today);
        sample.setReceivedDate(today);
        sample.setSysUserId(ACTOR);
        String sampleId = sampleService.insert(sample);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sampleService.get(sampleId));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusId);
        sampleItem.setSysUserId(ACTOR);
        String sampleItemId = sampleItemService.insert(sampleItem);

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItemService.get(sampleItemId));
        analysis.setTest(testService.get(testId));
        analysis.setStatusId(statusId);
        analysis.setAnalysisType("MANUAL");
        analysis.setRevision("1");
        analysis.setIsReportable("Y");
        analysis.setSysUserId(ACTOR);
        return analysisService.insert(analysis);
    }
}
