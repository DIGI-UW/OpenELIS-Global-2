package org.openelisglobal.eqa;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.scheduler.EQADeadlineAlertScheduler;
import org.openelisglobal.eqa.service.EQABlindingService;
import org.openelisglobal.eqa.service.EQABlindingService.BlindOrderSpec;
import org.openelisglobal.eqa.service.EQALabelPDFService;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-612 (FR-V2.4) — the in-house blinding backend against the real schema:
 * seal-and-distribute creates standard orders keyed by blind code, the unblind
 * pass scores numeric and categorical results (AC-V2.4-07/-08), flags absent
 * results as missed deadlines with their competency event (AC-V2.4-16), opens
 * the follow-up register row (AC-V2.4-09), refuses to double-score
 * (AC-V2.4-11), and the label sheet leaks no target while regenerating
 * byte-identically (AC-V2.4-14/-15).
 */
public class EQABlindingIntegrationTest extends EQASpineTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long NUMERIC_ANALYTE = 9801L;
    private static final long CATEGORICAL_ANALYTE = 9802L;
    private static final long LATE_ANALYTE = 9803L;
    private static final String SEEDED_TEST_ID = "9807";

    @Autowired
    private EQABlindingService blindingService;
    @Autowired
    private EQALabelPDFService labelPDFService;
    @Autowired
    private EQAPanelSampleDAO panelSampleDAO;
    @Autowired
    private IStatusService statusService;
    @Autowired
    private EQAPanelService panelService;

    /**
     * The eqa.scheduler package is deliberately excluded from the test component
     * scan (schedulers must not fire mid-suite), so the scheduler is built by hand
     * with only what the unblind pass reads.
     */
    private EQADeadlineAlertScheduler scheduler;

    @Before
    public void seedCatalogAndStatuses() {
        scheduler = new EQADeadlineAlertScheduler();
        org.springframework.test.util.ReflectionTestUtils.setField(scheduler, "eqaPanelDAO", eqaPanelDAO);
        org.springframework.test.util.ReflectionTestUtils.setField(scheduler, "blindingService", blindingService);

        // Self-ensured seeds: other fixtures truncate status_of_sample and the
        // test catalog in full-suite runs, so everything this class relies on
        // is (re)inserted idempotently and the status cache refreshed.
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT 9604, 1, 'ANALYSIS', 'Not Tested', 'restored by EQABlindingIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + "   WHERE name = 'Not Tested' AND status_type = 'ANALYSIS')");
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT 9801, 1, 'ORDER', 'Test Entered', 'restored by EQABlindingIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + "   WHERE name = 'Test Entered' AND status_type = 'ORDER')");
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT 9802, 1, 'SAMPLE', 'Entered', 'restored by EQABlindingIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + "   WHERE name = 'Entered' AND status_type = 'SAMPLE')");
        statusService.refreshCache();

        jdbc.update("INSERT INTO clinlims.localization (id, description)" + " SELECT 9807, 'EQA Blinding Test'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.localization WHERE id = 9807)");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                + " name_localization_id)"
                + " SELECT 9807, 'EQA Blinding Section', 'EQA Blinding Section', 'N', 9807, 9807"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test_section WHERE id = 9807)");
        jdbc.update("INSERT INTO clinlims.test (id, name, description, test_section_id, is_active, orderable,"
                + " sort_order, lastupdated, name_localization_id, guid)"
                + " SELECT 9807, 'EQA Blinding Test', 'EQA Blinding Test', 9807, 'Y', true, 9807, now(), 9807,"
                + " 'eqa-blinding-9807'" + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = 9807)");
    }

    @After
    public void cleanUpOrders() {
        // Results reference analyses (FK), so they go first; the base class
        // would clear them too, but only after this method has run.
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE eqa_provider_sample_id LIKE 'IHBLIND-%'");
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id IN (SELECT a.id FROM clinlims.analysis a"
                + " JOIN clinlims.sample_item si ON a.sampitem_id = si.id JOIN clinlims.sample s ON si.samp_id = s.id"
                + " WHERE s.accession_number LIKE 'IHBLIND-%')");
        jdbc.update("DELETE FROM clinlims.analysis WHERE sampitem_id IN (SELECT si.id FROM clinlims.sample_item si"
                + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number LIKE 'IHBLIND-%')");
        jdbc.update("DELETE FROM clinlims.sample_item WHERE samp_id IN"
                + " (SELECT id FROM clinlims.sample WHERE accession_number LIKE 'IHBLIND-%')");
        List<Long> patientIds = jdbc.queryForList(
                "SELECT DISTINCT sh.patient_id FROM clinlims.sample_human sh"
                        + " JOIN clinlims.sample s ON sh.samp_id = s.id WHERE s.accession_number LIKE 'IHBLIND-%'",
                Long.class);
        jdbc.update("DELETE FROM clinlims.sample_human WHERE samp_id IN"
                + " (SELECT id FROM clinlims.sample WHERE accession_number LIKE 'IHBLIND-%')");
        jdbc.update("DELETE FROM clinlims.sample WHERE accession_number LIKE 'IHBLIND-%'");
        for (Long patientId : patientIds) {
            Long personId = jdbc.queryForObject("SELECT person_id FROM clinlims.patient WHERE id = ?", Long.class,
                    patientId);
            jdbc.update("DELETE FROM clinlims.patient WHERE id = ?", patientId);
            jdbc.update("DELETE FROM clinlims.person WHERE id = ?", personId);
        }
        jdbc.update("DELETE FROM clinlims.eqa_lab_program_enrollment WHERE provider = 'In-house'");
        // The base class also clears this, but its @After runs later and the
        // organization row is FK-referenced by the follow-up register.
        jdbc.update("DELETE FROM clinlims.eqa_participant_followup");
        jdbc.update("DELETE FROM clinlims.organization WHERE name = 'This laboratory'");
    }

    // ---- fixture builders ----

    /**
     * The unblind endpoint maps its response after the scoring transaction has
     * committed, so the panel it holds is detached. The DTO must still resolve the
     * lazily loaded cycle, or the call answers 500 with the work already done. GET
     * /panels/{id} reads and maps in two transactions the same way.
     */
    @Test
    public void unblind_returnsAPanelTheEndpointCanRenderAfterTheTransactionEnds() {
        EQAProgram scheme = inHouseScheme("IH Detached Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.DISTRIBUTED, LocalDate.now().minusDays(1));
        insertPanelSample(panel, "IH-01", "IHBLIND-D1", NUMERIC_ANALYTE, "100", "95", "105");

        Map<String, Object> dto = panelService
                .toPanelDto(blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL));

        assertEquals("SCORED", dto.get("status"));
        assertEquals("MANUAL", dto.get("unblindMethod"));
        assertEquals(cycle.getId(), dto.get("cycleId"));
        assertEquals(cycle.getCycleNumber(), dto.get("cycleNumber"));
        assertEquals(cycle.getCycleName(), dto.get("cycleName"));

        Map<String, Object> read = panelService.toPanelDto(panelService.get(panel.getId()));
        assertEquals("SCORED", read.get("status"));
        assertEquals(cycle.getCycleNumber(), read.get("cycleNumber"));
    }

    private EQAProgram inHouseScheme(String name) {
        return insertScheme(name, EQASchemeType.IN_HOUSE, null);
    }

    private EQAPanel panelWith(EQAProgram scheme, EQACycle cycle, EQAPanelStatus status, LocalDate unblindDate) {
        return insertPanel(scheme, p -> {
            p.setCycle(cycle);
            p.setStatus(status);
            p.setUnblindDate(unblindDate == null ? null : Date.valueOf(unblindDate));
            // Prep evidence the seal gate now requires (AC-V2.4-13).
            p.setAliquotsProduced(8);
            p.setHomogeneityQcPassed(true);
        });
    }

    /**
     * Enters a result the way an analyst does — through the standard pipeline,
     * against the analysis the blinded order created — rather than writing
     * eqa_participant_result.result_value directly. Writing that column in a test
     * is what hid the fact that nothing in production fills it.
     */
    private void enterResultViaPipeline(String blindCode, String value) {
        Long analysisId = jdbc.queryForObject(
                "SELECT a.id FROM clinlims.analysis a JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                        + " JOIN clinlims.sample s ON si.samp_id = s.id WHERE s.accession_number = ?",
                Long.class, blindCode);
        jdbc.update(
                "INSERT INTO clinlims.result (id, analysis_id, result_type, value, sort_order, is_reportable,"
                        + " lastupdated, fhir_uuid)"
                        + " VALUES (nextval('clinlims.result_seq'), ?, 'N', ?, 1, 'Y', now(), gen_random_uuid())",
                analysisId, value);
    }

    private Map<String, Object> panelRow(Long panelId) {
        return jdbc.queryForMap(
                "SELECT status, unblind_method, unblinded_by, unblinded_at FROM clinlims.eqa_panel" + " WHERE id = ?",
                panelId);
    }

    private Map<String, Object> resultRow(Long resultId) {
        return jdbc.queryForMap("SELECT submission_status, performance_status, result_value, panel_sample_id"
                + " FROM clinlims.eqa_participant_result WHERE id = ?", resultId);
    }

    private Long insertPanelSample(EQAPanel panel, String sampleCode, String blindCode, long analyteId, String target,
            String low, String high) {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setPanel(panel);
        sample.setSampleCode(sampleCode);
        sample.setBlindCode(blindCode);
        sample.setAnalyteId(analyteId);
        sample.setTargetValue(target);
        if (low != null) {
            sample.setAcceptanceRangeLow(new BigDecimal(low));
        }
        if (high != null) {
            sample.setAcceptanceRangeHigh(new BigDecimal(high));
        }
        sample.setSysUserId(USER);
        return panelSampleDAO.insert(sample);
    }

    private Long insertResult(EQACycle cycle, Long roundId, long enrollmentId, long analyteId,
            EQASubmissionStatus status, String value, Long analystId) {
        return insertResult(cycle, roundId, enrollmentId, analyteId, status, value, analystId, null);
    }

    private Long insertResult(EQACycle cycle, Long roundId, long enrollmentId, long analyteId,
            EQASubmissionStatus status, String value, Long analystId, Long panelSampleId) {
        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(cycle);
        result.setRound(eqaRoundDAO.get(roundId).orElseThrow(AssertionError::new));
        result.setLabEnrollmentId(enrollmentId);
        result.setAnalyteId(analyteId);
        result.setSubmissionStatus(status);
        result.setResultValue(value);
        result.setAssignedAnalystId(analystId);
        result.setPanelSampleId(panelSampleId);
        result.setSysUserId(USER);
        return eqaParticipantResultDAO.insert(result);
    }

    private String resultStatus(Long resultId) {
        return jdbc.queryForObject("SELECT submission_status FROM clinlims.eqa_participant_result WHERE id = ?",
                String.class, resultId);
    }

    // ---- seal-and-distribute (FR-V2.4-04, AC-V2.4-01/-17) ----

    @Test
    public void sealAndDistribute_createsBlindOrdersDraftResultsAndDistributes() {
        EQAProgram scheme = inHouseScheme("IH Seal Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(7));
        Long sampleA = insertPanelSample(panel, "IH-01", "IHBLIND-A7", NUMERIC_ANALYTE, "100", "95", "105");
        Long sampleB = insertPanelSample(panel, "IH-02", "IHBLIND-B3", CATEGORICAL_ANALYTE, "Positive", null, null);

        Map<String, Object> dto = blindingService.sealAndDistribute(panel.getId(), List
                .of(new BlindOrderSpec(sampleA, SEEDED_TEST_ID, 1L), new BlindOrderSpec(sampleB, SEEDED_TEST_ID, 1L)),
                USER);

        assertEquals("DISTRIBUTED", dto.get("status"));
        assertEquals(List.of("IHBLIND-A7", "IHBLIND-B3"), dto.get("orderAccessionNumbers"));

        // The blind code IS the accession number of a real order (FR-V2.4-15).
        Long orderId = jdbc.queryForObject("SELECT id FROM clinlims.sample WHERE accession_number = 'IHBLIND-A7'",
                Long.class);
        assertEquals("one NotStarted analysis on the seeded test", Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.analysis a JOIN clinlims.sample_item si ON a.sampitem_id = si.id"
                                + " WHERE si.samp_id = ? AND a.test_id = 9807 AND a.status_id = ?::numeric",
                        Integer.class, orderId, statusService.getStatusID(AnalysisStatus.NotStarted)));
        Map<String, Object> eqaRow = jdbc.queryForMap(
                "SELECT cycle_id, round_id, is_eqa_sample FROM clinlims.sample_eqa WHERE sample_id = ?", orderId);
        assertEquals(cycle.getId().longValue(), ((Number) eqaRow.get("cycle_id")).longValue());
        assertNotNull("seal must have created round 1 and linked the order to it", eqaRow.get("round_id"));
        assertEquals(Boolean.TRUE, eqaRow.get("is_eqa_sample"));

        List<Map<String, Object>> drafts = jdbc.queryForList(
                "SELECT analyte_id, submission_status, assigned_analyst_id, analysis_id"
                        + " FROM clinlims.eqa_participant_result WHERE cycle_id = ? ORDER BY analyte_id",
                cycle.getId());
        assertEquals(2, drafts.size());
        assertEquals(NUMERIC_ANALYTE, ((Number) drafts.get(0).get("analyte_id")).longValue());
        assertEquals("DRAFT", drafts.get(0).get("submission_status"));
        assertEquals(1L, ((Number) drafts.get(0).get("assigned_analyst_id")).longValue());
        assertNotNull("draft is linked to the created analysis", drafts.get(0).get("analysis_id"));

        assertEquals("in-house self-enrollment created once", Integer.valueOf(1),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_lab_program_enrollment"
                        + " WHERE program_name = 'IH Seal Scheme' AND provider = 'In-house'", Integer.class));
    }

    @Test
    public void sealAndDistribute_refusesPartialCoverage() {
        EQAProgram scheme = inHouseScheme("IH Coverage Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(7));
        Long sampleA = insertPanelSample(panel, "IH-01", "IHBLIND-C1", NUMERIC_ANALYTE, "100", "95", "105");
        insertPanelSample(panel, "IH-02", "IHBLIND-C2", CATEGORICAL_ANALYTE, "Positive", null, null);

        try {
            blindingService.sealAndDistribute(panel.getId(), List.of(new BlindOrderSpec(sampleA, SEEDED_TEST_ID, null)),
                    USER);
            fail("a spec list that skips a panel sample must be refused");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals("panel must not have moved", EQAPanelStatus.PREPARING,
                eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());
        assertEquals("no orders were created", Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.sample WHERE accession_number LIKE 'IHBLIND-C%'", Integer.class));
    }

    // ---- unblind + scoring (FR-V2.4-06/-07/-08/-14) ----

    @Test
    public void unblind_scoresEveryResultAndRoutesFailures() throws Exception {
        // One enrollment is enough now: the grain is per aliquot, so several
        // results for the same lab and analyte coexist legitimately.
        seedEnrollment(9901, "IH Scoring Scheme");
        EQAProgram scheme = inHouseScheme("IH Scoring Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.DISTRIBUTED, LocalDate.now().minusDays(1));
        Long inRangeSample = insertPanelSample(panel, "IH-01", "IHBLIND-N1", NUMERIC_ANALYTE, "100", "95", "105");
        Long lowSample = insertPanelSample(panel, "IH-02", "IHBLIND-N2", NUMERIC_ANALYTE, "100", "95", "105");
        Long highSample = insertPanelSample(panel, "IH-03", "IHBLIND-N3", NUMERIC_ANALYTE, "100", "95", "105");
        Long catSample = insertPanelSample(panel, "IH-04", "IHBLIND-K1", CATEGORICAL_ANALYTE, "Positive", null, null);
        Long lateSample = insertPanelSample(panel, "IH-05", "IHBLIND-L1", LATE_ANALYTE, "50", "45", "55");

        Long inRange = insertResult(cycle, roundId, 9901, NUMERIC_ANALYTE, EQASubmissionStatus.SUBMITTED, "102", 1L,
                inRangeSample);
        Long belowLow = insertResult(cycle, roundId, 9901, NUMERIC_ANALYTE, EQASubmissionStatus.SUBMITTED, "92", 1L,
                lowSample);
        Long aboveHigh = insertResult(cycle, roundId, 9901, NUMERIC_ANALYTE, EQASubmissionStatus.SUBMITTED, "140", 1L,
                highSample);
        Long catMismatch = insertResult(cycle, roundId, 9901, CATEGORICAL_ANALYTE, EQASubmissionStatus.SUBMITTED,
                "Negative", 1L, catSample);
        Long neverEntered = insertResult(cycle, roundId, 9901, LATE_ANALYTE, EQASubmissionStatus.DRAFT, null, 1L,
                lateSample);

        blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);

        assertEquals("panel ends SCORED", EQAPanelStatus.SCORED,
                eqaPanelDAO.get(panel.getId()).orElseThrow(AssertionError::new).getStatus());

        // AC-V2.4-07/-08: the verdict itself is persisted, not merely implied by
        // a competency event. Both range bounds are exercised.
        assertEquals("ACCEPTABLE", resultRow(inRange).get("performance_status"));
        assertEquals("UNACCEPTABLE", resultRow(belowLow).get("performance_status"));
        assertEquals("UNACCEPTABLE", resultRow(aboveHigh).get("performance_status"));
        assertEquals("UNACCEPTABLE", resultRow(catMismatch).get("performance_status"));
        assertEquals("SCORED", resultRow(inRange).get("submission_status"));
        assertEquals("an empty draft misses the deadline", "MISSED_DEADLINE",
                resultRow(neverEntered).get("submission_status"));
        assertNull("a missed deadline carries no verdict", resultRow(neverEntered).get("performance_status"));

        // FR-V2.4-10: the audit distinguishes this from a scheduled unblind.
        Map<String, Object> panelRow = panelRow(panel.getId());
        assertEquals("MANUAL", panelRow.get("unblind_method"));
        assertEquals(1L, ((Number) panelRow.get("unblinded_by")).longValue());
        assertNotNull(panelRow.get("unblinded_at"));

        List<Map<String, Object>> events = jdbc
                .queryForList("SELECT event_type, participant_result_id FROM clinlims.eqa_analyst_competency_event"
                        + " WHERE cycle_id = ? ORDER BY participant_result_id", cycle.getId());
        assertEquals(4, events.size());
        assertEquals("IN_HOUSE_MISSED_DEADLINE",
                events.stream()
                        .filter(e -> neverEntered.longValue() == ((Number) e.get("participant_result_id")).longValue())
                        .findFirst().orElseThrow().get("event_type"));

        // AC-V2.4-09: one register row, tagged In-house, holding every failure.
        List<Map<String, Object>> followups = jdbc.queryForList(
                "SELECT followup_status, participant_result_summary_json FROM clinlims.eqa_participant_followup"
                        + " WHERE cycle_id = ?",
                cycle.getId());
        assertEquals(1, followups.size());
        assertEquals("NOTIFIED", followups.get(0).get("followup_status"));
        JsonNode summary = JSON.readTree((String) followups.get(0).get("participant_result_summary_json"));
        assertEquals("In-house", summary.get("source").asText());
        assertEquals(3, summary.get("unacceptable").size());
        java.util.Set<String> reported = new java.util.HashSet<>();
        summary.get("unacceptable").forEach(node -> reported.add(node.get("reported").asText()));
        assertEquals(java.util.Set.of("92", "140", "Negative"), reported);
    }

    @Test
    public void unblind_readsTheAnalystsValueFromTheResultPipeline() {
        // The defect this covers: an analyst runs a blinded order through standard
        // result entry, which writes the result table — nothing copies that into
        // eqa_participant_result. Reading the column alone marked every working
        // analyst as having missed the deadline.
        EQAProgram scheme = inHouseScheme("IH Bridge Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(1));
        Long acceptableSample = insertPanelSample(panel, "IH-01", "IHBLIND-BR1", NUMERIC_ANALYTE, "100", "95", "105");
        Long failingSample = insertPanelSample(panel, "IH-02", "IHBLIND-BR2", NUMERIC_ANALYTE, "100", "95", "105");
        Long silentSample = insertPanelSample(panel, "IH-03", "IHBLIND-BR3", LATE_ANALYTE, "50", "45", "55");

        blindingService.sealAndDistribute(panel.getId(),
                List.of(new BlindOrderSpec(acceptableSample, SEEDED_TEST_ID, 1L),
                        new BlindOrderSpec(failingSample, SEEDED_TEST_ID, 1L),
                        new BlindOrderSpec(silentSample, SEEDED_TEST_ID, 1L)),
                USER);

        // Two analysts enter results the normal way; the third never does.
        enterResultViaPipeline("IHBLIND-BR1", "101");
        enterResultViaPipeline("IHBLIND-BR2", "80");

        blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);

        Map<Long, Map<String, Object>> bySample = new java.util.HashMap<>();
        for (Map<String, Object> row : jdbc
                .queryForList("SELECT panel_sample_id, submission_status, performance_status, result_value"
                        + " FROM clinlims.eqa_participant_result WHERE cycle_id = ?", cycle.getId())) {
            bySample.put(((Number) row.get("panel_sample_id")).longValue(), row);
        }
        assertEquals("the entered value is picked up from the pipeline", "101",
                bySample.get(acceptableSample).get("result_value"));
        assertEquals("ACCEPTABLE", bySample.get(acceptableSample).get("performance_status"));
        assertEquals("UNACCEPTABLE", bySample.get(failingSample).get("performance_status"));
        assertEquals("only the analyst who entered nothing misses the deadline", "MISSED_DEADLINE",
                bySample.get(silentSample).get("submission_status"));
    }

    @Test
    public void sealAndDistribute_handlesReplicateAliquotsOfOneAnalyte() {
        // FR-V2.4-02 Mode A: one pool split into N aliquots, all the same analyte.
        // The original per-analyte uniqueness made this impossible to distribute.
        EQAProgram scheme = inHouseScheme("IH Replicate Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(2));
        Long first = insertPanelSample(panel, "IH-01", "IHBLIND-R1", NUMERIC_ANALYTE, "100", "95", "105");
        Long second = insertPanelSample(panel, "IH-02", "IHBLIND-R2", NUMERIC_ANALYTE, "60", "55", "65");

        blindingService.sealAndDistribute(panel.getId(),
                List.of(new BlindOrderSpec(first, SEEDED_TEST_ID, 1L), new BlindOrderSpec(second, SEEDED_TEST_ID, 1L)),
                USER);

        assertEquals("both aliquots produced a draft", Integer.valueOf(2),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_participant_result WHERE cycle_id = ?",
                        Integer.class, cycle.getId()));

        // Each replicate scores against its OWN target, not the first one found.
        enterResultViaPipeline("IHBLIND-R1", "100");
        enterResultViaPipeline("IHBLIND-R2", "100");
        blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);

        assertEquals("ACCEPTABLE",
                jdbc.queryForObject(
                        "SELECT performance_status FROM" + " clinlims.eqa_participant_result WHERE panel_sample_id = ?",
                        String.class, first));
        assertEquals("100 against a target of 60 is out of range", "UNACCEPTABLE",
                jdbc.queryForObject(
                        "SELECT performance_status FROM clinlims.eqa_participant_result WHERE panel_sample_id = ?",
                        String.class, second));
    }

    @Test
    public void verdict_numericTargetWithoutARangeComparesAsANumber() {
        EQAProgram scheme = inHouseScheme("IH Numeric Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(1));
        Long sample = insertPanelSample(panel, "IH-01", "IHBLIND-NUM", NUMERIC_ANALYTE, "100", null, null);

        blindingService.sealAndDistribute(panel.getId(), List.of(new BlindOrderSpec(sample, SEEDED_TEST_ID, 1L)), USER);
        enterResultViaPipeline("IHBLIND-NUM", "100.0");
        blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);

        assertEquals("100.0 equals a target of 100", "ACCEPTABLE",
                jdbc.queryForObject(
                        "SELECT performance_status FROM clinlims.eqa_participant_result WHERE panel_sample_id = ?",
                        String.class, sample));
    }

    @Test
    public void sealAndDistribute_refusesUnusableBlindCodesAndMissingPrepEvidence() {
        EQAProgram scheme = inHouseScheme("IH Validation Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));

        EQAPanel tooLong = panelWith(scheme, cycle, EQAPanelStatus.PREPARING, LocalDate.now().plusDays(1));
        Long longCode = insertPanelSample(tooLong, "IH-01", "IHBLIND-THIS-CODE-IS-FAR-TOO-LONG", NUMERIC_ANALYTE, "100",
                "95", "105");
        assertRefused(
                () -> blindingService.sealAndDistribute(tooLong.getId(),
                        List.of(new BlindOrderSpec(longCode, SEEDED_TEST_ID, 1L)), USER),
                "a blind code wider than an accession number");

        EQACycle secondCycle = readBack(insertCycle(scheme, 2));
        EQAPanel shortOnAliquots = insertPanel(scheme, p -> {
            p.setCycle(secondCycle);
            p.setStatus(EQAPanelStatus.PREPARING);
            p.setUnblindDate(Date.valueOf(LocalDate.now().plusDays(1)));
            p.setAliquotsProduced(0);
            p.setHomogeneityQcPassed(true);
        });
        Long sample = insertPanelSample(shortOnAliquots, "IH-01", "IHBLIND-V1", NUMERIC_ANALYTE, "100", "95", "105");
        assertRefused(() -> blindingService.sealAndDistribute(shortOnAliquots.getId(),
                List.of(new BlindOrderSpec(sample, SEEDED_TEST_ID, 1L)), USER), "fewer aliquots than samples");

        EQACycle thirdCycle = readBack(insertCycle(scheme, 3));
        EQAPanel failedQc = insertPanel(scheme, p -> {
            p.setCycle(thirdCycle);
            p.setStatus(EQAPanelStatus.PREPARING);
            p.setUnblindDate(Date.valueOf(LocalDate.now().plusDays(1)));
            p.setAliquotsProduced(4);
            p.setHomogeneityQcPassed(false);
        });
        Long qcSample = insertPanelSample(failedQc, "IH-01", "IHBLIND-V2", NUMERIC_ANALYTE, "100", "95", "105");
        assertRefused(
                () -> blindingService.sealAndDistribute(failedQc.getId(),
                        List.of(new BlindOrderSpec(qcSample, SEEDED_TEST_ID, 1L)), USER),
                "failed homogeneity QC with no justification");

        assertEquals("no orders were created by any refused seal", Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.sample WHERE accession_number LIKE 'IHBLIND-V%'"
                        + " OR accession_number LIKE 'IHBLIND-THIS%'", Integer.class));
    }

    private void assertRefused(Runnable action, String why) {
        try {
            action.run();
            fail("expected refusal: " + why);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // the service refuses before writing anything
        }
    }

    @Test
    public void unblind_secondRunConflictsAndNeverRescores() {
        seedEnrollment(9902, "IH Idempotency Scheme");
        EQAProgram scheme = inHouseScheme("IH Idempotency Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.DISTRIBUTED, LocalDate.now().minusDays(1));
        Long d1 = insertPanelSample(panel, "IH-01", "IHBLIND-D1", NUMERIC_ANALYTE, "100", "95", "105");
        insertResult(cycle, roundId, 9902, NUMERIC_ANALYTE, EQASubmissionStatus.SUBMITTED, "80", 1L, d1);

        blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);
        int eventsAfterFirstRun = jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_analyst_competency_event WHERE cycle_id = ?", Integer.class,
                cycle.getId());

        try {
            blindingService.unblindAndScore(panel.getId(), USER, EQAUnblindMethod.MANUAL);
            fail("a SCORED panel must refuse a second unblind (AC-V2.4-11)");
        } catch (IllegalStateException expected) {
        }
        assertEquals("no double-scoring on the re-run", Integer.valueOf(eventsAfterFirstRun),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.eqa_analyst_competency_event WHERE cycle_id = ?",
                        Integer.class, cycle.getId()));
    }

    // ---- scheduled unblind (FR-V2.4-06 automatic path) ----

    @Test
    public void scheduler_unblindsOnlyDueInHousePanels() {
        EQAProgram inHouse = inHouseScheme("IH Scheduler Scheme");
        EQAProgram external = insertScheme("External Scheduler Scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        EQACycle dueCycle = readBack(insertCycle(inHouse, 1));
        EQACycle futureCycle = readBack(insertCycle(inHouse, 2));
        EQACycle externalCycle = readBack(insertCycle(external, 1));

        EQAPanel due = panelWith(inHouse, dueCycle, EQAPanelStatus.DISTRIBUTED, LocalDate.now().minusDays(1));
        EQAPanel notDue = panelWith(inHouse, futureCycle, EQAPanelStatus.DISTRIBUTED, LocalDate.now().plusDays(3));
        EQAPanel externalPanel = panelWith(external, externalCycle, EQAPanelStatus.DISTRIBUTED,
                LocalDate.now().minusDays(1));

        scheduler.unblindDueInHousePanels();

        assertEquals("due in-house panel is unblinded and scored", EQAPanelStatus.SCORED,
                eqaPanelDAO.get(due.getId()).orElseThrow(AssertionError::new).getStatus());
        assertEquals("the audit records that the scheduler did it, not a person", "SCHEDULED",
                panelRow(due.getId()).get("unblind_method"));
        assertEquals("future panel untouched", EQAPanelStatus.DISTRIBUTED,
                eqaPanelDAO.get(notDue.getId()).orElseThrow(AssertionError::new).getStatus());
        assertEquals("external panels are not the scheduler's to unblind", EQAPanelStatus.DISTRIBUTED,
                eqaPanelDAO.get(externalPanel.getId()).orElseThrow(AssertionError::new).getStatus());
    }

    // ---- label sheet (FR-V2.4-13, AC-V2.4-14/-15) ----

    @Test
    public void labelSheet_showsBlindCodesNeverTargets_andRegeneratesByteIdentically() throws Exception {
        EQAProgram scheme = inHouseScheme("IH Label Scheme");
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        EQAPanel panel = panelWith(scheme, cycle, EQAPanelStatus.SEALED, LocalDate.now().plusDays(7));
        insertPanelSample(panel, "IH-01", "IHBLIND-P1", NUMERIC_ANALYTE, "43.7", "41.1", "46.3");
        insertPanelSample(panel, "IH-02", "IHBLIND-P2", CATEGORICAL_ANALYTE, "SecretPositive77", null, null);

        byte[] first = labelPDFService.generateLabelSheet(panel.getId());
        byte[] second = labelPDFService.generateLabelSheet(panel.getId());
        assertArrayEquals("regeneration is byte-identical (AC-V2.4-15)", first, second);

        PdfReader reader = new PdfReader(first);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
        }
        reader.close();
        String extracted = text.toString();

        assertTrue("every blind code prints", extracted.contains("IHBLIND-P1") && extracted.contains("IHBLIND-P2"));
        assertFalse("numeric target must not leak (AC-V2.4-14)", extracted.contains("43.7"));
        assertFalse("acceptance range must not leak", extracted.contains("41.1") || extracted.contains("46.3"));
        assertFalse("categorical target must not leak", extracted.contains("SecretPositive77"));
    }

    @Test
    public void labelSheet_refusedOutsideTheSealedWindow() {
        EQAProgram scheme = inHouseScheme("IH Early Label Scheme");
        EQAPanel panel = panelWith(scheme, null, EQAPanelStatus.PREPARING, null);
        insertPanelSample(panel, "IH-01", "IHBLIND-E1", NUMERIC_ANALYTE, "100", "95", "105");

        try {
            labelPDFService.generateLabelSheet(panel.getId());
            fail("labels before sealing would leak the panel's existence to the bench");
        } catch (IllegalStateException expected) {
        }
    }
}
