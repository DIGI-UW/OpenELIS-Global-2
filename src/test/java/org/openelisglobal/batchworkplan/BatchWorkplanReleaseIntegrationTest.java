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

        BatchWorkplanResponse released = batchWorkplanService.generate(batchId, true, true, ACTOR);

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
    }

    @Test
    public void aPassingBenchRunTodayReleasesWithoutAnNce() {
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));

        assertEquals(BatchQcStatus.PASS, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
        BatchWorkplanResponse released = batchWorkplanService.generate(batchId, false, false, ACTOR);

        assertEquals(BatchWorkplanStatus.ACTIVE, released.getStatus());
        assertFalse(released.getReleases().get(0).overridden());
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*)::int FROM clinlims.nc_event WHERE trigger_source_type = 'WORKPLAN_BATCH'"
                                + " AND trigger_source_id LIKE ?",
                        Integer.class, batchId + ":%"));
    }

    @Test
    public void yesterdaysRunLeavesADailyLotOverdue() {
        recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusDays(1));
        assertEquals(BatchQcStatus.OVERDUE, batchWorkplanService.getQcStatus(batchId, ACTOR).status());
    }

    @Test
    public void anOpenRejectionOnTheLotsRunIsAViolationUntilItIsResolved() {
        QCResult run = recordBenchRun(QCQualitativeOutcome.PASS, LocalDateTime.now().minusMinutes(5));
        QCRuleViolation warning = violation(run, "1-2s", "WARNING");
        QCRuleViolation rejection = violation(run, "1-3s", "REJECTION");

        BatchQcStatusResponse qc = batchWorkplanService.getQcStatus(batchId, ACTOR);
        assertEquals(BatchQcStatus.VIOLATION, qc.status());
        assertEquals(List.of("1-3s"), qc.lots().get(0).ruleCodes());

        setResolution(rejection, "ACKNOWLEDGED");
        assertEquals("an acknowledged violation is still open", BatchQcStatus.VIOLATION,
                batchWorkplanService.getQcStatus(batchId, ACTOR).status());

        setResolution(rejection, "RESOLVED");
        assertEquals("a warning never holds a batch", BatchQcStatus.PASS,
                batchWorkplanService.getQcStatus(batchId, ACTOR).status());
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
        batchWorkplanService.generate(batchId, false, false, ACTOR);

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
            batchWorkplanService.generate(batchId, override, mayOverride, ACTOR);
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
