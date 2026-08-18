package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.eqa.controller.rest.EQACycleRestController;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.SampleEQAService;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-610 [EQA V2.2 / T-13] — GET /rest/eqa/cycles/mine carries the display
 * fields My Cycles renders: scheme name/provider/type and progress + entry
 * state computed at analysis grain from orders linked via sample_eqa.cycle_id.
 */
public class EQACycleEnrichmentIntegrationTest extends EQASpineTestBase {

    private static final long TEST_ID = 98111L;
    private static final long SAMPLE_ID = 98112L;
    private static final long SAMPLE_ITEM_ID = 98113L;
    private static final long ANALYSIS_FINALIZED_ID = 98114L;
    private static final long ANALYSIS_NOT_STARTED_ID = 98115L;
    private static final long SAMPLE_EQA_ID = 98116L;
    private static final String ACCESSION = "EQAIT98112";

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    // eqa.controller.* is excluded from the test component scan — construct it
    private EQACycleRestController controller;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        controller = new EQACycleRestController(cycleService, sampleEQAService, sampleService, analysisService);
        ensureStatusRows();
        cleanupSeed();
    }

    /**
     * CI suite-order rule (§00, learned on PRs #4076/#4079): other fixtures
     * truncate status_of_sample, so self-ensure the canonical ANALYSIS rows this
     * test resolves via IStatusService, then refresh the cache — same pattern as
     * SampleEQAOrderStatusIntegrationTest.
     */
    private void ensureStatusRows() {
        String[][] canonical = { { "9604", "Not Tested" }, { "9606", "Finalized" } };
        for (String[] row : canonical) {
            jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                    + " SELECT ?::numeric, 1, 'ANALYSIS', ?, 'restored by EQACycleEnrichmentIntegrationTest'"
                    + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                    + "   WHERE name = ? AND status_type = 'ANALYSIS')", row[0], row[1], row[1]);
        }
        statusService.refreshCache();
    }

    @After
    public void tearDown() {
        cleanupSeed();
    }

    private void cleanupSeed() {
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE id = ?", SAMPLE_EQA_ID);
        jdbc.update("DELETE FROM clinlims.analysis WHERE id IN (?, ?)", ANALYSIS_FINALIZED_ID, ANALYSIS_NOT_STARTED_ID);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void myCyclesCarriesSchemeDisplayFieldsAndEmptyProgress() {
        EQAProgram scheme = insertScheme("Enrichment scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "NHRL");
        Long cycleId = insertCycle(scheme, 3);

        Map<String, Object> row = findCycleRow(cycleId);

        assertEquals(scheme.getName(), row.get("schemeName"));
        assertEquals("NHRL", row.get("provider"));
        assertEquals("REGIONAL_PT", row.get("schemeType"));
        assertEquals(Map.of("entered", 0, "total", 0), row.get("progress"));
        assertTrue("a cycle with no linked orders has no sample rows", ((List<?>) row.get("samples")).isEmpty());
    }

    @Test
    public void linkedOrderYieldsAnalysisGrainProgressAndEntryState() {
        EQAProgram scheme = insertScheme("Progress scheme " + System.nanoTime(), EQASchemeType.INTERNATIONAL_PT,
                "WHO AFRO");
        Long cycleId = insertCycle(scheme, 4);

        // analysis.status_id is numeric in the DB; the service hands back strings
        int finalizedId = Integer.parseInt(statusService.getStatusID(AnalysisStatus.Finalized));
        int notStartedId = Integer.parseInt(statusService.getStatusID(AnalysisStatus.NotStarted));

        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, 'EQA VL IT', 'EQA VL IT', 'Y', ?, NOW())",
                TEST_ID, UUID.randomUUID().toString());
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", SAMPLE_ID, ACCESSION);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 1, NOW())", SAMPLE_ITEM_ID, SAMPLE_ID);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, status_id, lastupdated)"
                        + " VALUES (?, 'MANUAL', ?, ?, ?, NOW())",
                ANALYSIS_FINALIZED_ID, TEST_ID, SAMPLE_ITEM_ID, finalizedId);
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, status_id, lastupdated)"
                        + " VALUES (?, 'MANUAL', ?, ?, ?, NOW())",
                ANALYSIS_NOT_STARTED_ID, TEST_ID, SAMPLE_ITEM_ID, notStartedId);
        jdbc.update(
                "INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, cycle_id, eqa_provider_sample_id,"
                        + " sys_user_id, last_updated) VALUES (?, ?, true, ?, 'WHO-VL-01', ?, NOW())",
                SAMPLE_EQA_ID, SAMPLE_ID, cycleId, USER);

        Map<String, Object> row = findCycleRow(cycleId);

        assertEquals("one of two analyses finalized", Map.of("entered", 1, "total", 2), row.get("progress"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) row.get("samples");
        assertEquals(1, samples.size());
        Map<String, Object> sample = samples.get(0);
        assertEquals(ACCESSION, sample.get("labNo"));
        assertEquals("WHO-VL-01", sample.get("id"));
        assertEquals("a mix of finalized and not-started reads as in progress", "in_progress",
                sample.get("entryStatus"));

        // flipping the open analysis to finalized flips the sample and progress
        jdbc.update("UPDATE clinlims.analysis SET status_id = ? WHERE id = ?", finalizedId, ANALYSIS_NOT_STARTED_ID);
        row = findCycleRow(cycleId);
        assertEquals(Map.of("entered", 2, "total", 2), row.get("progress"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> after = (List<Map<String, Object>>) row.get("samples");
        assertEquals("entered", after.get(0).get("entryStatus"));
    }

    private Map<String, Object> findCycleRow(Long cycleId) {
        return controller.myCycles(null).stream().filter(r -> cycleId.equals(r.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("cycle " + cycleId + " missing from /cycles/mine"));
    }
}
