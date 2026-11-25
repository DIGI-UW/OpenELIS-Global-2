package org.openelisglobal.batchworkplan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.batchworkplan.dao.BatchWorkplanDAO;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.service.BatchReleaseRefusedException;
import org.openelisglobal.batchworkplan.service.BatchWorkplanService;
import org.openelisglobal.batchworkplan.valueholder.BatchQcStatus;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplan;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanItem;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;
import org.openelisglobal.inventory.service.InventoryItemService;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryEnums.LotStatus;
import org.openelisglobal.inventory.valueholder.InventoryEnums.QCStatus;
import org.openelisglobal.inventory.valueholder.InventoryItem;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.service.QCControlLotService;
import org.openelisglobal.qc.service.QCResultService;
import org.openelisglobal.qc.valueholder.QCControlLot;
import org.openelisglobal.qc.valueholder.QCFrequencyType;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.openelisglobal.qc.valueholder.QCSource;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testreagentlink.service.TestReagentLinkService;
import org.openelisglobal.testreagentlink.valueholder.TestReagentLink;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-427 batch release against a real database: the changelog's columns and
 * permission key, the QC reads behind a batch's status (a bench lot, an open
 * rejection joined through its run), the bench QC capture the inline form posts
 * to, and a release that overrides QC and raises an NCE.
 */
public class BatchWorkplanReleaseIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String ACTOR = "1";

    @Autowired
    private BatchWorkplanService batchWorkplanService;
    @Autowired
    private org.openelisglobal.resultrun.service.ResultRunService resultRunService;
    @Autowired
    private org.openelisglobal.resultrun.service.RunQcService runQcService;
    @Autowired
    private BatchWorkplanDAO batchWorkplanDAO;
    @Autowired
    private QCControlLotService controlLotService;
    @Autowired
    private QCResultService qcResultService;
    @Autowired
    private QCRuleViolationDAO violationDAO;
    @Autowired
    private InventoryItemService inventoryItemService;
    @Autowired
    private InventoryLotService inventoryLotService;
    @Autowired
    private TestReagentLinkService testReagentLinkService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;

    private String testId;
    private String testSectionId;
    private QCControlLot benchLot;
    private Long reagentId;
    private Long reagentLotId;
    private Long batchId;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/panel-item.xml");
        org.openelisglobal.test.valueholder.Test test = testService.getAllActiveTests(false).stream()
                .filter(candidate -> candidate.getTestSection() != null).findFirst().orElseThrow();
        testId = test.getId();
        testSectionId = test.getTestSection().getId();

        benchLot = new QCControlLot();
        benchLot.setId(UUID.randomUUID().toString());
        benchLot.setLotNumber("OGC427-" + UUID.randomUUID());
        benchLot.setProductName("Batch release control");
        benchLot.setControlLevel("NORMAL");
        benchLot.setTestId(testId);
        benchLot.setCalculationMethod("MANUFACTURER_FIXED");
        benchLot.setManufacturerMean(100.0);
        benchLot.setManufacturerStdDev(5.0);
        benchLot.setActivationDate(new Timestamp(System.currentTimeMillis()));
        benchLot.setSystemUserId(Integer.valueOf(ACTOR));
        benchLot = controlLotService.createControlLot(benchLot);

        InventoryItem reagent = new InventoryItem();
        reagent.setFhirUuid(UUID.randomUUID());
        reagent.setCode("OGC427-" + UUID.randomUUID().toString().substring(0, 8));
        reagent.setName("OGC427 reagent " + UUID.randomUUID());
        reagent.setUnits("mL");
        reagent.setSysUserId(ACTOR);
        reagentId = inventoryItemService.insert(reagent);

        InventoryLot lot = new InventoryLot();
        lot.setFhirUuid(UUID.randomUUID());
        lot.setInventoryItem(inventoryItemService.get(reagentId));
        lot.setLotNumber("R-" + UUID.randomUUID().toString().substring(0, 8));
        lot.setInitialQuantity(10.0);
        lot.setCurrentQuantity(10.0);
        lot.setQcStatus(QCStatus.PASSED);
        lot.setStatus(LotStatus.ACTIVE);
        lot.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusYears(1)));
        lot.setSysUserId(ACTOR);
        reagentLotId = inventoryLotService.insert(lot);

        TestReagentLink link = new TestReagentLink();
        link.setId(UUID.randomUUID().toString());
        link.setTestId(testId);
        link.setReagentId(reagentId);
        link.setUsageType("PRIMARY");
        link.setSysUserId(ACTOR);
        testReagentLinkService.insert(link);

        batchId = createDraftBatch(createAnalysis());
    }

    @Test
    public void theChangelogAddsTheFrequencyRuleAndTheOverridePermission() {
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT qc_frequency_type, qc_required, shift_definition_hours FROM clinlims.qc_control_lot WHERE id = ?",
                benchLot.getId());
        assertEquals("DAILY", row.get("qc_frequency_type"));
        assertEquals(Boolean.TRUE, row.get("qc_required"));
        assertEquals(8, ((Number) row.get("shift_definition_hours")).intValue());
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*)::int FROM clinlims.system_module WHERE name = 'qa.qc.override'", Integer.class));
    }

    @Test
    public void aBenchBatchWithNoQcRunIsNotRunAndOnlyAnOverrideReleasesIt() {
        BatchQcStatusResponse qc = batchWorkplanService.getQcStatus(batchId, ACTOR);
        assertEquals(BatchQcStatus.NOT_RUN, qc.status());
        assertEquals(benchLot.getId(), qc.lots().get(0).controlLotId());
        assertTrue(qc.lots().get(0).canRecordInline());

        assertRefused(BatchReleaseRefusedException.Reason.OVERRIDE_REQUIRED, false, true);
        assertRefused(BatchReleaseRefusedException.Reason.OVERRIDE_NOT_PERMITTED, true, false);
        assertEquals("DRAFT", batchStatus());

        BatchWorkplanResponse released = batchWorkplanService.generate(batchId, true, "Control repeat booked", true,
                ACTOR);

        assertEquals(BatchWorkplanStatus.ACTIVE, released.getStatus());
        assertEquals(1, released.getReleases().size());
        assertTrue(released.getReleases().get(0).overridden());
        assertNotNull(released.getReleases().get(0).nceNumber());
        Map<String, Object> nce = jdbcTemplate.queryForMap(
                "SELECT trigger_source_type, severity, auto_generated FROM clinlims.nc_event WHERE id = ?",
                released.getReleases().get(0).nceId());
        assertEquals("WORKPLAN_BATCH", nce.get("trigger_source_type"));
        assertEquals("MAJOR", nce.get("severity"));
        assertEquals(Boolean.TRUE, nce.get("auto_generated"));
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject(
                        "SELECT count(*)::int FROM clinlims.batch_workplan_qc_release WHERE batch_workplan_id = ?"
                                + " AND nce_id IS NOT NULL AND overridden AND qc_status = 'NOT_RUN'",
                        Integer.class, batchId));
        assertEquals("OGC-1588: the reason is kept with the release", "Control repeat booked",
                jdbcTemplate.queryForObject(
                        "SELECT override_reason FROM clinlims.batch_workplan_qc_release WHERE batch_workplan_id = ?",
                        String.class, batchId));
        assertEquals("Control repeat booked", released.getReleases().get(0).overrideReason());
        assertTrue(jdbcTemplate
                .queryForObject("SELECT description FROM clinlims.nc_event WHERE id = ?", String.class,
                        released.getReleases().get(0).nceId())
                .contains("Reason given for the override: Control repeat booked"));
    }

    @Test
    public void aPassingBenchRunTodayReleasesWithoutAnNce() {
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));

        assertEquals(BatchQcStatus.PASS, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
        BatchWorkplanResponse released = batchWorkplanService.generate(batchId, false, null, false, ACTOR);

        assertEquals(BatchWorkplanStatus.ACTIVE, released.getStatus());
        assertFalse(released.getReleases().get(0).overridden());
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*)::int FROM clinlims.nc_event WHERE trigger_source_type = 'WORKPLAN_BATCH'"
                                + " AND trigger_source_id LIKE ?",
                        Integer.class, batchId + ":%"));
    }

    /**
     * OGC-1200 FR-B2, FR-D5, FR-G4: a released batch is a run of the batch's lab
     * unit, lot and analyses. A control recorded on it holds the batch's test, a
     * second release keeps the same run, and marking the batch complete completes
     * the run.
     */
    @Test
    public void aReleasedBatchIsARunThatHoldsItsTestOnAFailedControlAndCompletesWithIt() {
        jdbcTemplate.update("UPDATE clinlims.batch_workplan SET test_section_id = CAST(? AS numeric) WHERE id = ?",
                testSectionId, batchId);
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));
        BatchWorkplanResponse released = batchWorkplanService.generate(batchId, false, null, false, ACTOR);

        Long runId = released.getRunId();
        assertNotNull("the released batch names its run", runId);
        java.util.Map<String, Object> run = jdbcTemplate.queryForMap(
                "SELECT source, state, CAST(test_section_id AS VARCHAR) AS unit, reagent_lot_id, batch_workplan_id,"
                        + " row_count FROM clinlims.result_run WHERE id = ?",
                runId);
        assertEquals("WORKPLAN", run.get("source"));
        assertEquals("OPEN", run.get("state"));
        assertEquals(testSectionId, run.get("unit"));
        assertEquals((Object) batchId, ((Number) run.get("batch_workplan_id")).longValue());
        assertNotNull(run.get("reagent_lot_id"));
        assertEquals(1, ((Number) run.get("row_count")).intValue());

        String analysisId = jdbcTemplate.queryForObject(
                "SELECT CAST(analysis_id AS VARCHAR) FROM clinlims.batch_workplan_item WHERE batch_workplan_id = ?",
                String.class, batchId);
        assertEquals(java.util.Map.of(analysisId, runId), resultRunService.activeBatchRunsOf(List.of(analysisId)));
        org.openelisglobal.resultrun.form.RunViewer unitViewer = new org.openelisglobal.resultrun.form.RunViewer(
                java.util.Set.of(testId), java.util.Set.of(testSectionId));
        org.openelisglobal.resultrun.form.ResultRunHeader header = resultRunService.getHeader(runId, unitViewer)
                .orElseThrow();
        assertEquals("WORKPLAN", header.source());
        assertEquals(1, header.total());
        assertEquals(0, header.dispositioned());
        assertNotNull("the run reads its reagent lot", header.reagentLot());
        assertEquals(1,
                resultRunService.getRows(runId, unitViewer, org.openelisglobal.resultrun.form.ResultRunChip.ALL, 1, 25)
                        .orElseThrow().totalRows());
        assertTrue("another lab unit does not see the run",
                resultRunService.getHeader(runId,
                        new org.openelisglobal.resultrun.form.RunViewer(java.util.Set.of(), java.util.Set.of("-1")))
                        .isEmpty());

        org.openelisglobal.result.valueholder.Result entered = new org.openelisglobal.result.valueholder.Result();
        entered.setAnalysis(analysisService.get(analysisId));
        resultRunService.recordIntoWorkplanRuns(List.of(entered), ACTOR);
        assertEquals("a result entered for the batched analysis records into its run", runId, entered.getRunId());

        runQcService.recordControl(runId,
                new BenchQCCaptureForm(QCSource.MANUAL, testId, testSectionId, benchLot.getId(), null,
                        new BigDecimal("140"), null, QCQualitativeOutcome.FAIL, null, null,
                        LocalDateTime.now().minusMinutes(1), "OGC-1200 batch control"),
                Integer.parseInt(ACTOR));
        assertEquals("a failed control on the batch's run holds its analyses at entry",
                java.util.Map.of(analysisId, runId), runQcService.entryHoldingRuns(List.of(analysisId)));
        assertEquals("HELD",
                jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId));

        batchWorkplanService.transitionBatch(batchId, BatchWorkplanStatus.COMPLETED, ACTOR);
        assertEquals("COMPLETE",
                jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId));
        assertTrue(resultRunService.activeBatchRunsOf(List.of(analysisId)).isEmpty());
    }

    /**
     * OGC-1200 FR-C5 on a batch run: accepting a held test despite its failed
     * control names the batch's analyses in the NCE before any value is entered;
     * taking the batch back to setup closes the decided run, and the re-release
     * opens a fresh one whose controls cover the batch again.
     */
    @Test
    public void aDecisionOnABatchRunCoversTheBatchAndARereleaseOpensAFreshRun() {
        Long runId = releaseIntoRun();
        String analysisId = batchAnalysisId();
        recordInvalidControl(runId);

        org.openelisglobal.resultrun.valueholder.ResultRunTestDisposition decision = runQcService.decide(runId, testId,
                org.openelisglobal.resultrun.valueholder.RunTestAction.ACCEPT_DESPITE, "Control repeated and passed",
                true, ACTOR);
        assertTrue("the NCE names the batch's analysis",
                jdbcTemplate.queryForObject("SELECT description FROM clinlims.nc_event WHERE id = ?", String.class,
                        decision.getNceId()).contains("1 affected result(s)"));

        batchWorkplanService.transitionBatch(batchId, BatchWorkplanStatus.DRAFT, ACTOR);
        assertEquals("the decided run is closed with the edit", "COMPLETE",
                jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId));
        BatchWorkplanResponse draft = batchWorkplanService.getBatch(batchId, ACTOR);
        assertEquals(runId, draft.getRunId());
        assertFalse("the tile records a plain bench control while no run is open", draft.isRunOpen());
        assertTrue(resultRunService.activeBatchRunsOf(List.of(analysisId)).isEmpty());

        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(1));
        BatchWorkplanResponse rereleased = batchWorkplanService.generate(batchId, false, null, false, ACTOR);

        Long freshRunId = rereleased.getRunId();
        assertNotNull(freshRunId);
        assertFalse("a re-release after a decision opens a fresh run", runId.equals(freshRunId));
        assertTrue(rereleased.isRunOpen());
        assertEquals(java.util.Map.of(analysisId, freshRunId), resultRunService.activeBatchRunsOf(List.of(analysisId)));
        assertFalse("the fresh run's QC is open to a new control", runQcService.hasDecisions(freshRunId));
        assertEquals("the closed run lists no waiting rows", 0,
                resultRunService.getHeader(runId, unitViewer()).orElseThrow().chipCounts().all());
        assertEquals(1, resultRunService.getHeader(freshRunId, unitViewer()).orElseThrow().chipCounts().all());
    }

    /**
     * OGC-1200 FR-C5, FR-A3 on a batch run: a Retest decision sets the batch's
     * analyses aside, and the fresh run the re-release opens retests them, which
     * supersedes the first run.
     */
    @Test
    public void aRetestedBatchRunIsSupersededByTheFreshRunOfItsRerelease() {
        Long runId = releaseIntoRun();
        String analysisId = batchAnalysisId();
        recordInvalidControl(runId);

        runQcService.decide(runId, testId, org.openelisglobal.resultrun.valueholder.RunTestAction.RETEST, null, false,
                ACTOR);
        assertEquals("the batch's analysis is set aside for retest", List.of(analysisId),
                jdbcTemplate.queryForList("SELECT CAST(rr.analysis_id AS VARCHAR) FROM clinlims.result_run_retest rr"
                        + " JOIN clinlims.result_run_test_disposition d ON d.id = rr.disposition_id"
                        + " WHERE d.run_id = ?", String.class, runId));

        batchWorkplanService.transitionBatch(batchId, BatchWorkplanStatus.DRAFT, ACTOR);
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(1));
        Long freshRunId = batchWorkplanService.generate(batchId, false, null, false, ACTOR).getRunId();

        runQcService.confirmRetest(freshRunId, testId, runId, ACTOR);
        assertEquals("SUPERSEDED",
                jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId));
    }

    /**
     * OGC-1200 FR-B2 with a batch released more than once: each run owns the
     * analyses resulted in it and, while it is the batch's open run, those it still
     * waits for. A value recorded in the first run is neither counted nor set aside
     * by the run that follows it.
     */
    @Test
    public void eachRunOfABatchCountsAndDecidesOnlyItsOwnAnalyses() {
        jdbcTemplate.update(
                "INSERT INTO clinlims.batch_workplan_item (id, batch_workplan_id, analysis_id, sort_order)"
                        + " VALUES (nextval('clinlims.batch_workplan_item_seq'), ?," + " CAST(? AS numeric), 2)",
                batchId, createAnalysis());
        Long firstRunId = releaseIntoRun();
        String resulted = batchAnalysisIds().get(0);
        String waiting = batchAnalysisIds().get(1);
        recordInvalidControl(firstRunId);
        runQcService.decide(firstRunId, testId, org.openelisglobal.resultrun.valueholder.RunTestAction.ACCEPT_DESPITE,
                "Control repeated and passed", true, ACTOR);
        jdbcTemplate.update(
                "INSERT INTO clinlims.result (id, analysis_id, value, result_type, run_id, lastupdated)"
                        + " VALUES (nextval('clinlims.result_seq'), CAST(? AS numeric), '81'," + " 'N', ?, now())",
                resulted, firstRunId);

        batchWorkplanService.transitionBatch(batchId, BatchWorkplanStatus.DRAFT, ACTOR);
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(1));
        Long laterRunId = batchWorkplanService.generate(batchId, false, null, false, ACTOR).getRunId();

        org.openelisglobal.resultrun.form.ResultRunHeader first = resultRunService.getHeader(firstRunId, unitViewer())
                .orElseThrow();
        assertEquals("the first run counts the value recorded in it", 1, first.dispositioned());
        assertEquals(2, first.total());
        org.openelisglobal.resultrun.form.ResultRunHeader later = resultRunService.getHeader(laterRunId, unitViewer())
                .orElseThrow();
        assertEquals("the later run holds only the analysis still waiting", 1, later.total());
        assertEquals(0, later.dispositioned());

        recordInvalidControl(laterRunId);
        runQcService.decide(laterRunId, testId, org.openelisglobal.resultrun.valueholder.RunTestAction.RETEST, null,
                false, ACTOR);
        assertEquals("the later run sets aside only its own analysis", List.of(waiting),
                jdbcTemplate.queryForList("SELECT CAST(rr.analysis_id AS VARCHAR) FROM clinlims.result_run_retest rr"
                        + " JOIN clinlims.result_run_test_disposition d ON d.id = rr.disposition_id"
                        + " WHERE d.run_id = ?", String.class, laterRunId));
    }

    /**
     * OGC-1200 FR-E3 on a batch run: the run lists the usable lots of its tests'
     * reagents, records the one chosen on it for itself and its batch, offers it as
     * the last-used lot on the next run, refuses a lot that is not usable, and is
     * marked complete with its batch.
     */
    @Test
    public void aBatchRunRecordsTheLotChosenOnItAndIsMarkedCompleteThere() {
        Long runId = releaseIntoRun();
        org.openelisglobal.resultrun.form.RunLotOptions options = resultRunService.lotOptions(runId, unitViewer())
                .orElseThrow();
        assertEquals(reagentLotId, options.currentLotId());
        InventoryLot second = new InventoryLot();
        second.setFhirUuid(UUID.randomUUID());
        second.setInventoryItem(inventoryItemService.get(reagentId));
        second.setLotNumber("R2-" + UUID.randomUUID().toString().substring(0, 8));
        second.setInitialQuantity(10.0);
        second.setCurrentQuantity(10.0);
        second.setQcStatus(QCStatus.PASSED);
        second.setStatus(LotStatus.ACTIVE);
        second.setExpirationDate(Timestamp.valueOf(LocalDateTime.now().plusYears(2)));
        second.setSysUserId(ACTOR);
        Long secondLotId = inventoryLotService.insert(second);
        assertTrue("the run offers its reagent's usable lots",
                resultRunService.lotOptions(runId, unitViewer()).orElseThrow().reagents().stream()
                        .filter(reagent -> reagent.id().equals(reagentId)).flatMap(reagent -> reagent.lots().stream())
                        .anyMatch(lot -> lot.id().equals(secondLotId)));

        assertEquals(second.getLotNumber(), resultRunService
                .setReagentLot(runId, reagentId, secondLotId, unitViewer(), ACTOR).orElseThrow().reagentLot());
        assertEquals("the batch records the lot chosen on its run", secondLotId, jdbcTemplate.queryForObject(
                "SELECT reagent_lot_id FROM clinlims.batch_workplan WHERE id = ?", Long.class, batchId));
        try {
            resultRunService.setReagentLot(runId, reagentId, -1L, unitViewer(), ACTOR);
            fail("a lot that is not usable is refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("expired, used up"));
        }

        batchId = createDraftBatch(createAnalysis());
        Long nextRunId = releaseIntoRun();
        assertEquals("the next run offers the lot last used", secondLotId,
                resultRunService.lotOptions(nextRunId, unitViewer()).orElseThrow().lastUsedLotId());

        assertEquals("COMPLETE", resultRunService.markComplete(nextRunId, unitViewer(), ACTOR).orElseThrow().state());
        assertEquals("COMPLETED", jdbcTemplate.queryForObject("SELECT status FROM clinlims.batch_workplan WHERE id = ?",
                String.class, batchId));
        try {
            resultRunService.markComplete(nextRunId, unitViewer(), ACTOR);
            fail("a closed run is not marked complete again");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("no longer open"));
        }
    }

    private List<String> batchAnalysisIds() {
        return jdbcTemplate.queryForList("SELECT CAST(analysis_id AS VARCHAR) FROM clinlims.batch_workplan_item"
                + " WHERE batch_workplan_id = ? ORDER BY sort_order", String.class, batchId);
    }

    private Long releaseIntoRun() {
        jdbcTemplate.update("UPDATE clinlims.batch_workplan SET test_section_id = CAST(? AS numeric) WHERE id = ?",
                testSectionId, batchId);
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));
        return batchWorkplanService.generate(batchId, false, null, false, ACTOR).getRunId();
    }

    private String batchAnalysisId() {
        return jdbcTemplate.queryForObject(
                "SELECT CAST(analysis_id AS VARCHAR) FROM clinlims.batch_workplan_item WHERE batch_workplan_id = ?",
                String.class, batchId);
    }

    private void recordInvalidControl(Long runId) {
        runQcService.recordControl(runId,
                new BenchQCCaptureForm(QCSource.RDT, testId, testSectionId, null, "KIT-OGC1200", null, null,
                        QCQualitativeOutcome.INVALID, null, null, LocalDateTime.now().minusMinutes(2),
                        "OGC-1200 batch control"),
                Integer.parseInt(ACTOR));
        assertEquals("HELD",
                jdbcTemplate.queryForObject("SELECT state FROM clinlims.result_run WHERE id = ?", String.class, runId));
    }

    private org.openelisglobal.resultrun.form.RunViewer unitViewer() {
        return new org.openelisglobal.resultrun.form.RunViewer(java.util.Set.of(testId),
                java.util.Set.of(testSectionId));
    }

    @Test
    public void yesterdaysRunLeavesADailyLotOverdue() {
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusDays(1));
        assertEquals(BatchQcStatus.OVERDUE, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
    }

    @Test
    public void anOpenRejectionOnTheLotsRunIsAViolationUntilItIsAcknowledged() {
        QCResult run = recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));
        QCRuleViolation warning = violation(run, "1-2s", "WARNING");
        QCRuleViolation rejection = violation(run, "1-3s", "REJECTION");

        BatchQcStatusResponse qc = batchWorkplanService.getQcStatus(batchId, ACTOR);
        assertEquals(BatchQcStatus.VIOLATION, qc.status());
        assertEquals(List.of("1-3s"), qc.lots().get(0).ruleCodes());

        setResolution(rejection, "ACKNOWLEDGED");
        assertEquals("acknowledging is how the QC dashboard clears a violation, and a warning never holds a batch",
                BatchQcStatus.PASS, batchWorkplanService.getQcStatus(batchId, ACTOR).status());

        setResolution(rejection, "RESOLVED");
        assertEquals(BatchQcStatus.PASS, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
        assertNotNull(warning.getId());
    }

    /**
     * A failing manual control is REJECTED at capture and, once the bench-failure
     * listener has run, also carries a MANUAL_FAIL rejection violation. Either way
     * the batch is held and can only be released by an override.
     */
    @Test
    public void aFailedBenchRunHoldsTheBatch() {
        recordBenchRun(QCQualitativeOutcome.FAIL, LocalDateTime.now().minusMinutes(5));
        BatchQcStatusResponse qc = batchWorkplanService.getQcStatus(batchId, ACTOR);
        assertTrue("was " + qc.status(), qc.status() == BatchQcStatus.FAILED || qc.status() == BatchQcStatus.VIOLATION);
        assertTrue(qc.overridable());
        assertEquals("REJECTED", qc.lots().get(0).lastRun().resultStatus());
    }

    @Test
    public void aPerRunLotNeedsAFreshRunAfterEachRelease() {
        benchLot.setQcFrequencyType(QCFrequencyType.PER_RUN);
        controlLotService.update(benchLot);
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));
        batchWorkplanService.generate(batchId, false, null, false, ACTOR);

        Long second = createDraftBatch(createAnalysis());

        assertEquals(BatchQcStatus.OVERDUE, batchWorkplanService.getQcStatus(second, ACTOR).status());
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now());
        assertEquals(BatchQcStatus.PASS, batchWorkplanService.getQcStatus(second, ACTOR).status());
    }

    @Test
    public void anExpiredControlLotBlocksTheRelease() {
        controlLotService.deactivateControlLot(benchLot.getId());
        assertEquals(BatchQcStatus.LOT_EXPIRED, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
        assertRefused(BatchReleaseRefusedException.Reason.BLOCKED, true, true);
    }

    private void assertRefused(BatchReleaseRefusedException.Reason reason, boolean override, boolean mayOverride) {
        try {
            batchWorkplanService.generate(batchId, override, override ? "Control repeat booked" : null, mayOverride,
                    ACTOR);
            fail("Expected the release to be refused with " + reason);
        } catch (BatchReleaseRefusedException refused) {
            assertEquals(reason, refused.getReason());
        }
    }

    private String batchStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM clinlims.batch_workplan WHERE id = ?", String.class,
                batchId);
    }

    private QCResult recordBenchRun(QCQualitativeOutcome outcome, LocalDateTime at) {
        BenchQCCaptureForm capture = new BenchQCCaptureForm(QCSource.MANUAL, testId, testSectionId, benchLot.getId(),
                null, new BigDecimal("100"), null, outcome, null, null, at, "OGC-427 integration run");
        return qcResultService.createBenchQCResult(capture, Integer.parseInt(ACTOR));
    }

    private QCRuleViolation violation(QCResult run, String ruleCode, String severity) {
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId(UUID.randomUUID().toString());
        violation.setTriggeringResultId(run.getId());
        violation.setRuleCode(ruleCode);
        violation.setSeverity(severity);
        violation.setTestId(testId);
        violation.setViolationDateTime(new Timestamp(System.currentTimeMillis()));
        violation.setSystemUserId(Integer.valueOf(ACTOR));
        violation.setSysUserId(ACTOR);
        violationDAO.insert(violation);
        return violation;
    }

    private void setResolution(QCRuleViolation violation, String resolutionStatus) {
        jdbcTemplate.update("UPDATE clinlims.qc_rule_violation SET resolution_status = ? WHERE id = ?",
                resolutionStatus, violation.getId());
    }

    private Long createDraftBatch(String analysisId) {
        BatchWorkplan batch = new BatchWorkplan();
        batch.setName("OGC-427 integration");
        batch.setStatus(BatchWorkplanStatus.DRAFT);
        batch.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        batch.setCreatedByUserId(Integer.valueOf(ACTOR));
        batch.setUpdatedByUserId(Integer.valueOf(ACTOR));
        batch.setSysUserId(ACTOR);
        batch.setReagentId(reagentId);
        batch.setReagentLotId(reagentLotId);
        BatchWorkplanItem item = new BatchWorkplanItem();
        item.setAnalysisId(analysisId);
        item.setSortOrder(1);
        batch.addItem(item);
        return batchWorkplanDAO.insert(batch);
    }

    private String createAnalysis() {
        String statusId = jdbcTemplate.queryForObject("SELECT id FROM clinlims.status_of_sample ORDER BY id LIMIT 1",
                String.class);
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());

        Sample sample = new Sample();
        sample.setAccessionNumber("OGC427" + UUID.randomUUID().toString().substring(0, 6));
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
        analysis.setStartedDate(today);
        analysis.setSysUserId(ACTOR);
        return analysisService.insert(analysis);
    }
}
