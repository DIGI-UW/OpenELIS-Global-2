package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.eqa.service.EQACycleSubmissionService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-611 (FR-V2.3-04) — per-analyst capture: what the Analyst column on
 * standard result entry writes, and the schemes it stays out of.
 *
 * <p>
 * The analyst is chosen at result entry, but the EQA participant result is
 * normally written later, by the cycle sweep that sees a finalized analysis.
 * These cases pin the seam between the two: the capture opens the draft row
 * early, and neither a re-save nor a scored verdict may be overwritten by it.
 *
 * <p>
 * The pipeline fixture matches {@code EQAAutoSubmissionIntegrationTest}'s — a
 * real sample, specimen, analysis and result row — because the capture resolves
 * its analyte exactly the way the bridge does.
 */
public class EQAPerAnalystCaptureIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT = 9931L;
    /**
     * The spine fixture's own catalog rows — inventing a test row means guessing at
     * columns this schema does not have.
     */
    private static final long ANALYTE = 9802L;
    private static final long TEST = 9702L;
    private static final long SAMPLE = 9931L;
    private static final long SAMPLE_ITEM = 9931L;
    private static final long SAMPLE_EQA = 9931L;
    private static final long OTHER_ANALYST = 2L;

    private long nextAnalysisId = 9941L;

    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Before
    public void seedPipelineCatalog() {
        ensureStatus(9931, "Finalized");
        ensureStatus(9932, "Not Tested");
        ensureStatus(9933, "Test Canceled");
        statusService.refreshCache();
        assertResolves(AnalysisStatus.Finalized);
        assertResolves(AnalysisStatus.NotStarted);

        jdbc.update("INSERT INTO clinlims.localization (id, description) SELECT 9931, 'EQA T19'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.localization WHERE id = 9931)");
        jdbc.update("INSERT INTO clinlims.test_section (id, name, description, is_external, sort_order,"
                + " name_localization_id) SELECT 9931, 'EQA T19', 'EQA T19 section', 'N', 9931, 9931"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.test_section WHERE id = 9931)");
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, name_localization_id, lastupdated)"
                + " SELECT 9931, 'EQA T19 specimen', 'H', 9931, now()"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.type_of_sample WHERE id = 9931)");
        jdbc.update(
                "INSERT INTO clinlims.system_user (id, external_id, login_name, last_name, first_name,"
                        + " initials, is_active, is_employee, lastupdated)"
                        + " SELECT ?, 'EQA_T19_ANALYST', 'eqa_t19_analyst', 'Analyst', 'Second', 'SA', 'Y', 'Y', now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.system_user WHERE id = ?)",
                OTHER_ANALYST, OTHER_ANALYST);
        cleanOrders();
    }

    @After
    public void cleanUpOrders() {
        cleanOrders();
    }

    // ---- the flag decides ----

    @Test
    public void aSchemeThatDoesNotCaptureAnalystsRecordsNothing() {
        EQAProgram scheme = insertScheme("Capture off", EQASchemeType.INTERNATIONAL_PT, "NHLS");
        assertFalse("per-analyst capture is opt-in", Boolean.TRUE.equals(scheme.getPerAnalyst()));
        Fixture fixture = order(scheme);

        assertFalse("a scheme that does not capture analysts refuses the write", cycleSubmissionService
                .assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)), ADMIN_USER_ID, USER));
        assertEquals("and writes no participant result at all", 0, allResults().size());
    }

    @Test
    public void aNonEqaAnalysisRecordsNothing() {
        Fixture fixture = order(perAnalystScheme("Capture on"));
        jdbc.update("UPDATE clinlims.sample_eqa SET is_eqa_sample = false WHERE sample_id = ?", SAMPLE);

        assertFalse("a patient sample never gets an EQA analyst", cycleSubmissionService
                .assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)), ADMIN_USER_ID, USER));
        assertEquals(0, allResults().size());
    }

    // ---- what the capture writes ----

    @Test
    public void theDraftRowIsOpenedWithTheAnalystWhenTheSweepHasNotRunYet() {
        Fixture fixture = order(perAnalystScheme("Capture on"));

        assertTrue(cycleSubmissionService.assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)),
                ADMIN_USER_ID, USER));

        EQAParticipantResult row = onlyResult();
        assertEquals("the capture opens the row rather than waiting for the sweep", Long.valueOf(ADMIN_USER_ID),
                row.getAssignedAnalystId());
        assertEquals(EQASubmissionStatus.DRAFT, row.getSubmissionStatus());
        assertEquals(Long.valueOf(fixture.analysisId), row.getAnalysisId());
        assertEquals(Long.valueOf(ANALYTE), row.getAnalyteId());
        assertEquals(fixture.cycle.getId(), row.getCycle().getId());
        assertEquals("the entered value rides along", "1200", row.getResultValue());
    }

    @Test
    public void anExistingDraftKeepsItsValueAndTakesTheNewAnalyst() {
        Fixture fixture = order(perAnalystScheme("Capture on"));
        Long resultId = insertParticipantResult(fixture.cycle, fixture.round, ENROLLMENT, ANALYTE,
                EQASubmissionStatus.DRAFT, "42");

        assertTrue(cycleSubmissionService.assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)),
                OTHER_ANALYST, USER));

        EQAParticipantResult row = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        assertEquals(Long.valueOf(OTHER_ANALYST), row.getAssignedAnalystId());
        assertEquals("re-attributing must not disturb the entered result", "42", row.getResultValue());
        assertEquals("one row per analyte per round, still", 1, allResults().size());
    }

    @Test
    public void aSubmittedResultIsNotReattributed() {
        Fixture fixture = order(perAnalystScheme("Capture on"));
        Long resultId = insertParticipantResult(fixture.cycle, fixture.round, ENROLLMENT, ANALYTE,
                EQASubmissionStatus.SUBMITTED, "42");

        assertFalse("the provider has already seen this result", cycleSubmissionService
                .assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)), OTHER_ANALYST, USER));

        EQAParticipantResult row = eqaParticipantResultDAO.get(resultId).orElseThrow(AssertionError::new);
        assertNull("a scored record is not moved after the fact", row.getAssignedAnalystId());
    }

    @Test
    public void recordingTheSameAnalystTwiceIsANoOp() {
        Fixture fixture = order(perAnalystScheme("Capture on"));
        assertTrue(cycleSubmissionService.assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)),
                ADMIN_USER_ID, USER));

        assertFalse("a re-save with the same analyst writes nothing", cycleSubmissionService
                .assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)), ADMIN_USER_ID, USER));
        assertEquals(1, allResults().size());
        assertEquals(Long.valueOf(ADMIN_USER_ID), onlyResult().getAssignedAnalystId());
    }

    @Test
    public void theCapturedAnalystSurvivesTheLaterCycleSweep() {
        Fixture fixture = order(perAnalystScheme("Capture on"));
        cycleSubmissionService.assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)), OTHER_ANALYST,
                USER);

        cycleSubmissionService.advanceCycle(fixture.cycle.getId());

        EQAParticipantResult row = onlyResult();
        assertEquals("the sweep updates the value and leaves the analyst alone", Long.valueOf(OTHER_ANALYST),
                row.getAssignedAnalystId());
        assertEquals("1200", row.getResultValue());
    }

    /**
     * The live shape this missed: real result rows carry no analyte of their own,
     * so the analyte comes from the enrollment's test map (qa/030). A fixture that
     * sets result.analyte_id never exercises that fallback.
     */
    @Test
    public void anAnalyteOnlyOnTheEnrollmentMapStillResolves() {
        Fixture fixture = order(perAnalystScheme("Capture on"), false);

        assertTrue(cycleSubmissionService.assignAnalyst(analysisService.get(String.valueOf(fixture.analysisId)),
                ADMIN_USER_ID, USER));

        EQAParticipantResult row = onlyResult();
        assertEquals(Long.valueOf(ADMIN_USER_ID), row.getAssignedAnalystId());
        assertEquals("the analyte came from the enrollment map, not the result row", Long.valueOf(ANALYTE),
                row.getAnalyteId());
    }

    // ---- fixtures ----

    private static final class Fixture {
        EQACycle cycle;
        EQARound round;
        long analysisId;
    }

    private EQAProgram perAnalystScheme(String name) {
        EQAProgram scheme = insertScheme(name, EQASchemeType.INTERNATIONAL_PT, "NHLS");
        jdbc.update("UPDATE clinlims.eqa_program SET per_analyst = true WHERE id = ?", scheme.getId());
        return eqaProgramService.get(scheme.getId());
    }

    /** An EQA order on a cycle of this scheme, with one finalized result. */
    private Fixture order(EQAProgram scheme) {
        return order(scheme, true);
    }

    private Fixture order(EQAProgram scheme, boolean resultCarriesAnalyte) {
        Fixture fixture = new Fixture();
        fixture.cycle = readBack(insertCycle(scheme, 1));
        fixture.cycle.setScheme(scheme);
        fixture.round = eqaRoundDAO.get(insertRound(fixture.cycle, 1, "OPEN")).orElseThrow(AssertionError::new);

        seedEnrollment(ENROLLMENT, "EQA Capture Programme");
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date,"
                + " collection_date, lastupdated) VALUES (?, 'EQAT19001', now(), now(), now(), now())", SAMPLE);
        jdbc.update(
                "INSERT INTO clinlims.sample_item (id, sort_order, status_id, samp_id, typeosamp_id,"
                        + " collection_date, lastupdated) VALUES (?, 1, ?::numeric, ?, 9931, now(), now())",
                SAMPLE_ITEM, statusService.getStatusID(AnalysisStatus.NotStarted), SAMPLE);
        jdbc.update(
                "INSERT INTO clinlims.sample_eqa (id, sample_id, is_eqa_sample, eqa_enrollment_id, cycle_id,"
                        + " round_id, sys_user_id, last_updated) VALUES (?, ?, true, ?, ?, ?, ?, now())",
                SAMPLE_EQA, SAMPLE, ENROLLMENT, fixture.cycle.getId(), fixture.round.getId(), USER);
        jdbc.update(
                "INSERT INTO clinlims.eqa_lab_enrollment_test_map (id, enrollment_id, test_id, analyte_id,"
                        + " sys_user_id, lastupdated) VALUES (?, ?, ?, ?, ?, now())",
                9900L + TEST, ENROLLMENT, TEST, ANALYTE, USER);

        fixture.analysisId = nextAnalysisId++;
        jdbc.update(
                "INSERT INTO clinlims.analysis (id, sampitem_id, test_sect_id, test_id, revision, analysis_type,"
                        + " entry_date, status_id, lastupdated, fhir_uuid)"
                        + " VALUES (?, ?, 9931, ?, 1, 'ROUTINE', now(), ?::numeric, now(), gen_random_uuid())",
                fixture.analysisId, SAMPLE_ITEM, TEST, statusService.getStatusID(AnalysisStatus.Finalized));
        jdbc.update(
                "INSERT INTO clinlims.result (id, analysis_id, analyte_id, result_type, value, sort_order,"
                        + " significant_digits, is_reportable, lastupdated, fhir_uuid)"
                        + " VALUES (?, ?, ?, 'N', '1200', 1, 0, 'Y', now(), gen_random_uuid())",
                fixture.analysisId + 500, fixture.analysisId, resultCarriesAnalyte ? ANALYTE : null);
        return fixture;
    }

    private void ensureStatus(long id, String name) {
        jdbc.update("INSERT INTO clinlims.status_of_sample (id, code, status_type, name, description)"
                + " SELECT ?, 1, 'ANALYSIS', ?, 'restored by EQAPerAnalystCaptureIntegrationTest'"
                + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.status_of_sample"
                + "   WHERE name = ? AND status_type = 'ANALYSIS')", id, name, name);
    }

    private void assertResolves(AnalysisStatus status) {
        String id = statusService.getStatusID(status);
        assertTrue(status + " did not resolve to a real status_of_sample row (got " + id + ")",
                id != null && !"-1".equals(id));
    }

    /**
     * Children before parents, and both EQA-side references before the pipeline
     * rows they point at: eqa_participant_result holds an FK to analysis, and
     * sample_eqa one to eqa_round, which the base fixture clears on the next setUp.
     * Leaving either behind fails the following test, not this one.
     */
    private void cleanOrders() {
        jdbc.update("DELETE FROM clinlims.eqa_analyst_competency_event");
        jdbc.update("DELETE FROM clinlims.eqa_participant_result");
        jdbc.update("DELETE FROM clinlims.sample_eqa WHERE sample_id = ?", SAMPLE);
        jdbc.update("DELETE FROM clinlims.eqa_lab_enrollment_test_map WHERE enrollment_id = ?", ENROLLMENT);
        jdbc.update("DELETE FROM clinlims.result WHERE analysis_id BETWEEN 9941 AND 9960");
        jdbc.update("DELETE FROM clinlims.analysis WHERE sampitem_id = ?", SAMPLE_ITEM);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE);
    }

    private List<EQAParticipantResult> allResults() {
        return eqaParticipantResultDAO.getAll();
    }

    private EQAParticipantResult onlyResult() {
        List<EQAParticipantResult> rows = allResults();
        assertEquals("exactly one participant result", 1, rows.size());
        return rows.get(0);
    }
}
