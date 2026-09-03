package org.openelisglobal.eqa.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.eqa.EQASpineTestBase;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQACycleStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-610 (EQA V2.2) — automatic submission end to end against the real schema:
 * the bridge from the standard result pipeline into
 * {@code eqa_participant_result}, the participant state walk, the FR-V2.2-05
 * window and retry cap, and the two fallbacks.
 *
 * <p>
 * The fixture enters results the way the pipeline does — a {@code result} row
 * carrying {@code analyte_id}, which {@code ResultSaveService} fills from
 * {@code test_analyte} — rather than writing
 * {@code eqa_participant_result.result_value} directly. Writing that column in
 * a test is what previously hid the fact that nothing in production filled it.
 * {@link #analysisWithNoAnalyte_neverSubmitsAPartialSet()} pins the other half:
 * a result the bridge cannot map must stop the submission, not shrink it.
 *
 * <p>
 * The FHIR post itself is stubbed. What matters here is what the workflow does
 * with success and failure; the bundle is asserted by the FHIR submission
 * tests.
 */
public class EQAAutoSubmissionIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT = 9901L;
    private static final long VL_ANALYTE = 9802L;
    private static final long SEROLOGY_ANALYTE = 9801L;
    private static final long VL_TEST = 9702L;
    private static final long SEROLOGY_TEST = 9701L;
    private static final long SAMPLE = 9901L;
    private static final long SAMPLE_ITEM = 9901L;
    private static final long SAMPLE_EQA = 9901L;

    /**
     * Fixture ids are assigned here rather than from a sequence: the sample_eqa and
     * analysis sequences are not in the clinlims schema, so nextval() on a
     * schema-qualified name fails.
     */
    private long nextAnalysisId = 9911L;

    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private FhirConfig fhirConfig;

    private EQACycleSubmissionServiceImpl target;
    private EQAFhirSubmissionService fhirStub;
    private EQAFhirSubmissionService realFhirService;

    @Before
    public void stubTransportAndSeedCatalog() {
        target = AopTestUtils.getTargetObject(cycleSubmissionService);
        fhirStub = mock(EQAFhirSubmissionService.class);
        realFhirService = (EQAFhirSubmissionService) ReflectionTestUtils.getField(target, "fhirSubmissionService");
        ReflectionTestUtils.setField(target, "fhirSubmissionService", fhirStub);

        // submitIfDue refuses to spend a retry when no store is configured, which
        // is exactly the manual-fallback deployment — so the automatic tests need
        // one present. AppTestConfig supplies FhirConfig as a mock, so this is a
        // stub rather than a field write.
        when(fhirConfig.getLocalFhirStorePath()).thenReturn("http://fhir.example/stub");

        ensureAnalysisStatuses();
        ensureOrderCatalog();
        nextAnalysisId = 9911L;
    }

    /**
     * Null-safe on purpose: when a fixture insert fails in setUp the collaborators
     * were never swapped, and a second exception here would bury the first.
     */
    @After
    public void restoreCollaborators() {
        if (target != null) {
            ReflectionTestUtils.setField(target, "fhirSubmissionService", realFhirService);
            target.setClock(Clock.systemDefaultZone());
        }
        // Back to the shared mock's own default: unstubbed, it reports no store.
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(null);
        jdbc.update("DELETE FROM clinlims.alert WHERE alert_entity_type = 'EQACycle'");
    }

    /**
     * sample_eqa carries FKs to eqa_cycle and eqa_round, so the order rows have to
     * go before the base clears those tables — in the base's own @Before as well as
     * its @After, which is why this is an override rather than another @After.
     */
    @Override
    protected void cleanEqaTables() {
        // eqa_participant_result FKs to analysis and sample_eqa FKs to eqa_round, so
        // the EQA rows go first, then the base clears the cycle spine, then the
        // orders themselves.
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.eqa_lab_enrollment_test_map WHERE enrollment_id = ?", ENROLLMENT);
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id = ?", SAMPLE);
        super.cleanEqaTables();
        cleanOrders();
    }

    /**
     * Other fixtures truncate status_of_sample, and a later cache refresh rebuilds
     * StatusService from whatever is left, so the ANALYSIS rows this class writes
     * are restored by the exact names StatusService maps.
     */
    private void ensureAnalysisStatuses() {
        // The names are the ones StatusService maps, not the enum constants:
        // AnalysisStatus.NotStarted is "Not Tested" and Canceled is "Test Canceled".
        // A row under any other name is never mapped, so getStatusID returns "-1"
        // and the analysis insert fails its FK instead of saying why.
        ensureStatus(9901, "ANALYSIS", "Finalized");
        ensureStatus(9902, "ANALYSIS", "Not Tested");
        ensureStatus(9903, "ANALYSIS", "Test Canceled");
        statusService.refreshCache();
        assertResolves(AnalysisStatus.Finalized);
        assertResolves(AnalysisStatus.NotStarted);
        assertResolves(AnalysisStatus.Canceled);
    }

    /** Fail on the cause, not on the foreign key three lines later. */
    private void assertResolves(AnalysisStatus status) {
        String id = statusService.getStatusID(status);
        assertTrue(status + " did not resolve to a real status_of_sample row (got " + id
                + "); check the name StatusService maps it by", id != null && !"-1".equals(id));
    }

    private void ensureStatus(long id, String type, String name) {
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT ?, 1, ?, ?, 'restored by EQAAutoSubmissionIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample WHERE name = ? AND status_type = ?)", id,
                type, name, name, type);
    }

    private void ensureOrderCatalog() {
        jdbc.update("INSERT INTO clinlims.localization (id, description) SELECT 9901, 'EQA T14'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.localization WHERE id = 9901)");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                + " name_localization_id) SELECT 9901, 'EQA T14', 'EQA T14 section', 'N',"
                + " 9901, 9901 WHERE NOT EXISTS (SELECT 1 FROM clinlims.test_section WHERE id = 9901)");
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, name_localization_id, lastupdated)"
                + " SELECT 9901, 'EQA T14 specimen', 'H', 9901, now()"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE id = 9901)");
    }

    private void cleanOrders() {
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id = ?", SAMPLE);
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id BETWEEN 9911 AND 9930");
        jdbc.update("DELETE FROM clinlims.analysis WHERE sampitem_id = ?", SAMPLE_ITEM);
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id = ?", SAMPLE);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE);
    }

    // ---- fixture builders ----

    private EQAProgram externalScheme(boolean requiresReview) {
        EQAProgram scheme = insertScheme("NHLS VL Scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        if (requiresReview) {
            jdbc.update("UPDATE clinlims.eqa_program SET requires_cycle_review = true WHERE id = ?", scheme.getId());
            scheme = eqaProgramService.get(scheme.getId());
        }
        return scheme;
    }

    /**
     * An EQA order on this cycle: sample, specimen, and the linking sample_eqa row.
     */
    private void eqaOrder(EQACycle cycle, Long roundId) {
        seedEnrollment(ENROLLMENT, "NHLS VL");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date,"
                + " collection_date, lastupdated) VALUES (?, 'EQAT14001', now(), now(), now(), now())", SAMPLE);
        jdbc.update(
                "INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id,"
                        + " collection_date, lastupdated) VALUES (?, 1, ?::numeric, ?, 9901, now(), now())",
                SAMPLE_ITEM, statusService.getStatusID(AnalysisStatus.NotStarted), SAMPLE);
        jdbc.update(
                "INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, eqa_enrollment_id, cycle_id,"
                        + " round_id, sys_user_id, last_updated) VALUES (?, ?, true, ?, ?, ?, ?, now())",
                SAMPLE_EQA, SAMPLE, ENROLLMENT, cycle.getId(), roundId, USER);
    }

    /**
     * What the lab declares when enrolling: this local test reports this scheme
     * analyte (qa/030). Without it an external order has no resolvable analyte,
     * because real result rows carry none.
     */
    private void mapTestToSchemeAnalyte(long testId, long analyteId) {
        jdbc.update(
                "INSERT INTO clinlims.eqa_lab_enrollment_test_map (id, enrollment_id, test_id, analyte_id,"
                        + " sys_user_id, lastupdated) VALUES (?, ?, ?, ?, ?, now())",
                9900L + testId, ENROLLMENT, testId, analyteId, USER);
    }

    /** A finalized analysis carrying one reported value, mapped to its analyte. */
    private long finalizedAnalysis(long testId, long analyteId, String value) {
        return analysis(testId, analyteId, value, AnalysisStatus.Finalized, true);
    }

    private long analysis(long testId, long analyteId, String value, AnalysisStatus status, boolean withAnalyte) {
        long analysisId = nextAnalysisId++;
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision, analysis_type,"
                        + " entry_date, status_id, lastupdated, fhir_uuid)"
                        + " VALUES (?, ?, 9901, ?, 1, 'ROUTINE', now(), ?::numeric, now(), gen_random_uuid())",
                analysisId, SAMPLE_ITEM, testId, statusService.getStatusID(status));
        if (value != null) {
            // Result type and significant_digits are set the way a configured test
            // sets them. On a numeric result left at 0 digits,
            // ResultService.getResultValue truncates to the integer part (4.75 ->
            // "4") — real shipped reporting behaviour, not an EQA rule. The bridge
            // deliberately submits that same formatted value, because it is what the
            // in-house scoring path compares against its sealed target.
            boolean numeric = value.matches("-?\\d+(\\.\\d+)?");
            jdbc.update(
                    "INSERT INTO clinlims.result (id, analysis_id, analyte_id, result_type, value, sort_order,"
                            + " significant_digits, is_reportable, lastupdated, fhir_uuid)"
                            + " VALUES (?, ?, ?, ?, ?, 1, ?, 'Y', now(), gen_random_uuid())",
                    analysisId + 500, analysisId, withAnalyte ? analyteId : null, numeric ? "N" : "A", value,
                    numeric ? 2 : -1);
        }
        return analysisId;
    }

    private void windowElapsed() {
        target.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(2)));
    }

    private List<Map<String, Object>> participantResults(Long cycleId) {
        return jdbc.queryForList("SELECT analyte_id, submission_status, result_value, submission_channel,"
                + " manual_submission_reference, submitted_at, performance_status, z_score, analysis_id"
                + " FROM clinlims.eqa_participant_result WHERE cycle_id = ? ORDER BY analyte_id", cycleId);
    }

    private List<String> auditTriggers(Long cycleId) {
        return jdbc.queryForList(
                "SELECT prior_state || '->' || new_state || ':' || trigger_event"
                        + " FROM clinlims.eqa_cycle_state_transition WHERE cycle_id = ? ORDER BY id",
                String.class, cycleId);
    }

    private int attempts(Long cycleId) {
        return jdbc.queryForObject("SELECT submission_attempts FROM clinlims.eqa_cycle WHERE id = ?", Integer.class,
                cycleId);
    }

    private int failureAlerts(Long cycleId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.alert WHERE alert_type = 'EQA_SUBMISSION_FAILED'"
                + " AND alert_entity_type = 'EQACycle' AND alert_entity_id = ?", Integer.class, cycleId);
    }

    // ---- the happy path ----

    @Test
    public void validatedOrder_bridgesResultWalksTheMachineAndSubmits() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        long analysisId = finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(true);
        windowElapsed();

        assertTrue("the sweep must report work done", cycleSubmissionService.advanceCycle(cycle.getId()));

        List<Map<String, Object>> results = participantResults(cycle.getId());
        assertEquals("one participant result, per reported analyte", 1, results.size());
        Map<String, Object> row = results.get(0);
        assertEquals(VL_ANALYTE, ((Number) row.get("analyte_id")).longValue());
        assertEquals("4.75", row.get("result_value"));
        assertEquals("SUBMITTED", row.get("submission_status"));
        assertEquals("FHIR", row.get("submission_channel"));
        assertEquals(analysisId, ((Number) row.get("analysis_id")).longValue());
        assertTrue("a submitted result is stamped", row.get("submitted_at") != null);

        assertEquals(EQACycleStatus.SUBMITTED, readBack(cycle.getId()).getStatus());
        assertEquals(List.of("PLANNED->PANEL_RECEIVED:LAST_VALIDATED_RESULT",
                "PANEL_RECEIVED->TESTING:LAST_VALIDATED_RESULT", "TESTING->READY_TO_SUBMIT:LAST_VALIDATED_RESULT",
                "READY_TO_SUBMIT->SUBMITTED:FHIR_SUBMIT_SUCCESS"), auditTriggers(cycle.getId()));
        verify(fhirStub).submitCycleViaFhir(cycle.getId(), ENROLLMENT);
    }

    @Test
    public void rerunOverAnUnchangedCycle_writesNothingFurther() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 2));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(true);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());
        int auditRows = auditTriggers(cycle.getId()).size();

        assertEquals("a SUBMITTED cycle is no longer a candidate", false,
                cycleSubmissionService.advanceCycle(cycle.getId()));
        assertEquals(auditRows, auditTriggers(cycle.getId()).size());
        assertEquals(1, participantResults(cycle.getId()).size());
    }

    // ---- the window, the cap, and the gates ----

    @Test
    public void beforeTheWindowElapses_theCycleWaitsInReadyToSubmit() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 3));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals(EQACycleStatus.READY_TO_SUBMIT, readBack(cycle.getId()).getStatus());
        assertEquals("VALIDATED_PARTIAL", participantResults(cycle.getId()).get(0).get("submission_status"));
        assertEquals(0, attempts(cycle.getId()));
        verify(fhirStub, never()).submitCycleViaFhir(anyLong(), anyLong());
    }

    @Test
    public void aSchemeThatRequiresReview_isNeverSubmittedAutomatically() {
        EQAProgram scheme = externalScheme(true);
        EQACycle cycle = readBack(insertCycle(scheme, 4));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals(EQACycleStatus.READY_TO_SUBMIT, readBack(cycle.getId()).getStatus());
        verify(fhirStub, never()).submitCycleViaFhir(anyLong(), anyLong());
    }

    @Test
    public void fiveFailures_stopRetryingAndRaiseExactlyOneAlert() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 5));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(false);

        // Each pass jumps the clock past that attempt's backoff (15, 30, 60, 120
        // minutes), so five attempts are genuinely spent rather than five calls
        // being made in one backoff window.
        long[] hoursOut = { 2, 3, 5, 8, 12 };
        for (long hours : hoursOut) {
            target.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(hours)));
            cycleSubmissionService.advanceCycle(cycle.getId());
        }

        assertEquals("the retry budget is spent", 5, attempts(cycle.getId()));
        assertEquals("the cycle must not claim to have been submitted", EQACycleStatus.READY_TO_SUBMIT,
                readBack(cycle.getId()).getStatus());
        assertEquals("one alert, not one per attempt", 1, failureAlerts(cycle.getId()));

        // A sixth pass long after the last backoff must not spend a sixth attempt.
        target.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(48)));
        cycleSubmissionService.advanceCycle(cycle.getId());
        assertEquals(5, attempts(cycle.getId()));
        assertEquals(1, failureAlerts(cycle.getId()));
    }

    @Test
    public void withinTheBackoffWindow_noSecondAttemptIsSpent() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 6));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(false);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());
        assertEquals(1, attempts(cycle.getId()));

        // Two minutes later: inside the 15-minute backoff.
        target.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofHours(2).plusMinutes(2)));
        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals("still one attempt", 1, attempts(cycle.getId()));
    }

    // ---- completeness and ownership ----

    @Test
    public void anUnfinishedAnalysis_holdsTheCycleInTesting() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 7));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        analysis(SEROLOGY_TEST, SEROLOGY_ANALYTE, null, AnalysisStatus.NotStarted, true);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals(EQACycleStatus.TESTING, readBack(cycle.getId()).getStatus());
        assertEquals("the finished analysis is still bridged", 1, participantResults(cycle.getId()).size());
        verify(fhirStub, never()).submitCycleViaFhir(anyLong(), anyLong());
    }

    @Test
    public void aCanceledAnalysis_doesNotCountAgainstCompleteness() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 8));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        analysis(SEROLOGY_TEST, SEROLOGY_ANALYTE, null, AnalysisStatus.Canceled, true);
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(true);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals(EQACycleStatus.SUBMITTED, readBack(cycle.getId()).getStatus());
    }

    /**
     * The inversion of the happy path: {@code analyte_id} is a NOT NULL FK, so a
     * result the bridge cannot map produces no row — and the cycle must then stop
     * short rather than submit a set that is missing an analyte the provider
     * expects.
     */
    @Test
    public void analysisWithNoAnalyte_neverSubmitsAPartialSet() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 9));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        analysis(SEROLOGY_TEST, SEROLOGY_ANALYTE, "Positive", AnalysisStatus.Finalized, false);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals("only the mappable analyte is bridged", 1, participantResults(cycle.getId()).size());
        assertEquals(EQACycleStatus.TESTING, readBack(cycle.getId()).getStatus());
        verify(fhirStub, never()).submitCycleViaFhir(anyLong(), anyLong());
    }

    /**
     * The shape production actually produces. Real result rows carry no analyte —
     * ResultSaveService fills analyte_id from test_analyte, which most test
     * configurations do not have — so the enrollment's scheme mapping is what makes
     * an external order submittable.
     */
    @Test
    public void resultWithoutItsOwnAnalyte_resolvesThroughTheEnrollmentMapping() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 16));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        mapTestToSchemeAnalyte(VL_TEST, VL_ANALYTE);
        analysis(VL_TEST, VL_ANALYTE, "4.75", AnalysisStatus.Finalized, false);
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(true);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        List<Map<String, Object>> results = participantResults(cycle.getId());
        assertEquals(1, results.size());
        assertEquals("the analyte comes from the enrollment mapping, not the result", VL_ANALYTE,
                ((Number) results.get(0).get("analyte_id")).longValue());
        assertEquals("4.75", results.get(0).get("result_value"));
        assertEquals("SUBMITTED", results.get(0).get("submission_status"));
        assertEquals(EQACycleStatus.SUBMITTED, readBack(cycle.getId()).getStatus());
    }

    /**
     * An unmapped test stays unbridgeable: the mapping is the only source once the
     * result carries no analyte, so the cycle holds rather than submitting a set
     * the provider cannot reconcile.
     */
    @Test
    public void aTestWithNoSchemeMapping_holdsTheCycle() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 17));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        mapTestToSchemeAnalyte(VL_TEST, VL_ANALYTE);
        analysis(VL_TEST, VL_ANALYTE, "4.75", AnalysisStatus.Finalized, false);
        analysis(SEROLOGY_TEST, SEROLOGY_ANALYTE, "Positive", AnalysisStatus.Finalized, false);
        windowElapsed();

        cycleSubmissionService.advanceCycle(cycle.getId());

        assertEquals("only the mapped test is bridged", 1, participantResults(cycle.getId()).size());
        assertEquals(EQACycleStatus.TESTING, readBack(cycle.getId()).getStatus());
        verify(fhirStub, never()).submitCycleViaFhir(anyLong(), anyLong());
    }

    @Test
    public void anInHouseCycle_isLeftToTheBlindingService() {
        EQAProgram scheme = insertScheme("In-house panel", EQASchemeType.IN_HOUSE, null);
        EQACycle cycle = readBack(insertCycle(scheme, 10));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        windowElapsed();

        assertEquals(false, cycleSubmissionService.advanceCycle(cycle.getId()));

        assertEquals(EQACycleStatus.PLANNED, readBack(cycle.getId()).getStatus());
        assertTrue("in-house results belong to the blinding path", participantResults(cycle.getId()).isEmpty());
    }

    /**
     * The sweep's entry point, which the scheduler calls before anything else: a
     * cycle in a state where results can still arrive is a candidate, one already
     * submitted or scored is not. Untested, this is a silent no-op — the sweep
     * would simply never visit anything.
     */
    @Test
    public void advanceCandidates_areTheStatesWhereWorkCanStillArrive() {
        EQAProgram scheme = externalScheme(false);
        Long planned = insertCycle(scheme, 20);
        Long testing = insertCycle(scheme, 21);
        Long submitted = insertCycle(scheme, 22);
        Long closed = insertCycle(scheme, 23);
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'TESTING' WHERE id = ?", testing);
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'SUBMITTED' WHERE id = ?", submitted);
        jdbc.update("UPDATE clinlims.eqa_cycle SET status = 'CLOSED' WHERE id = ?", closed);

        List<Long> candidates = cycleSubmissionService.findAdvanceCandidates();

        assertTrue("a planned cycle is a candidate", candidates.contains(planned));
        assertTrue("a testing cycle is a candidate", candidates.contains(testing));
        assertFalse("a submitted cycle is not", candidates.contains(submitted));
        assertFalse("a closed cycle is not", candidates.contains(closed));
    }

    // ---- fallbacks ----

    @Test
    public void manualSubmission_demandsTheProvidersReference() {
        EQAProgram scheme = externalScheme(true);
        EQACycle cycle = readBack(insertCycle(scheme, 11));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        cycleSubmissionService.advanceCycle(cycle.getId());

        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> cycleSubmissionService.submitManually(cycle.getId(), ENROLLMENT, "  ", USER));
        assertTrue(blank.getMessage().contains("reference"));
        assertEquals("nothing may move without one", "VALIDATED_PARTIAL",
                participantResults(cycle.getId()).get(0).get("submission_status"));

        EQACycle submitted = cycleSubmissionService.submitManually(cycle.getId(), ENROLLMENT, "NHLS-2026-0042", USER);

        assertEquals(EQACycleStatus.SUBMITTED, submitted.getStatus());
        Map<String, Object> row = participantResults(cycle.getId()).get(0);
        assertEquals("SUBMITTED", row.get("submission_status"));
        assertEquals("MANUAL", row.get("submission_channel"));
        assertEquals("NHLS-2026-0042", row.get("manual_submission_reference"));
        assertTrue("the manual move is audited as manual",
                auditTriggers(cycle.getId()).contains("READY_TO_SUBMIT->SUBMITTED:MANUAL_OVERRIDE"));
    }

    @Test
    public void exportBundle_carriesTheValidatedRowsAndQuotesSeparators() {
        EQAProgram scheme = externalScheme(true);
        EQACycle cycle = readBack(insertCycle(scheme, 12));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "Positive, weak");
        cycleSubmissionService.advanceCycle(cycle.getId());

        String csv = cycleSubmissionService.exportBundleCsv(cycle.getId(), ENROLLMENT);

        String[] lines = csv.split("\n");
        assertEquals("header plus one row", 2, lines.length);
        assertEquals("cycle_id,cycle_name,round_number,analyte_id,analyte_name,result_value,result_unit,"
                + "submission_status,entered_at", lines[0]);
        assertTrue("a value with a comma must be quoted, or the column count shifts: " + lines[1],
                lines[1].contains("\"Positive, weak\""));
        assertTrue(lines[1].startsWith(cycle.getId() + ",,1," + VL_ANALYTE + ","));
    }

    @Test
    public void scoreIntake_writesTheVerdictAndZScoreAndScoresTheCycle() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 13));
        Long roundId = insertRound(cycle, 1, "OPEN");
        eqaOrder(cycle, roundId);
        finalizedAnalysis(VL_TEST, VL_ANALYTE, "4.75");
        when(fhirStub.submitCycleViaFhir(anyLong(), anyLong())).thenReturn(true);
        windowElapsed();
        cycleSubmissionService.advanceCycle(cycle.getId());

        int scored = cycleSubmissionService.intakeScores(cycle.getId(), ENROLLMENT,
                List.of(Map.of("analyteId", VL_ANALYTE, "performance", "acceptable", "zScore", "1.2")), USER);

        assertEquals(1, scored);
        Map<String, Object> row = participantResults(cycle.getId()).get(0);
        assertEquals("SCORED", row.get("submission_status"));
        assertEquals("ACCEPTABLE", row.get("performance_status"));
        assertEquals(0, new BigDecimal("1.2").compareTo((BigDecimal) row.get("z_score")));
        assertEquals(EQACycleStatus.SCORED, readBack(cycle.getId()).getStatus());
        assertTrue(auditTriggers(cycle.getId()).contains("SUBMITTED->SCORED:SCORE_INTAKE"));
    }

    @Test
    public void scoreIntake_refusesAResultFromAnotherCycle() {
        EQAProgram scheme = externalScheme(false);
        EQACycle cycle = readBack(insertCycle(scheme, 14));
        EQACycle other = readBack(insertCycle(scheme, 15));
        insertRound(cycle, 1, "OPEN");
        Long otherRound = insertRound(other, 1, "OPEN");
        seedEnrollment(ENROLLMENT, "NHLS VL");
        Long strayResult = insertParticipantResult(other, eqaRoundDAO.get(otherRound).orElseThrow(), ENROLLMENT,
                VL_ANALYTE, null, "9.9");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> cycleSubmissionService.intakeScores(cycle.getId(), ENROLLMENT,
                        List.of(Map.of("resultId", strayResult, "performance", "ACCEPTABLE")), USER));

        assertTrue(e.getMessage().contains("does not belong to cycle"));
        assertNull("the stray result must be untouched",
                participantResults(other.getId()).get(0).get("performance_status"));
    }
}
