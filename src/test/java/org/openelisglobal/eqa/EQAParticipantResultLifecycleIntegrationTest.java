package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAParticipantResultService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-609 [EQA V2.1 / T-11] — participant-result lifecycle enforcement
 * (FR-V2.1-05) and the competency events it emits (FR-V2.1-22).
 */
public class EQAParticipantResultLifecycleIntegrationTest extends EQASpineTestBase {

    private static final long ENROLLMENT = 9901L;
    private static final long ANALYTE = 9801L;
    private static final long ANALYST = 1L;

    @Autowired
    private EQAParticipantResultService resultService;

    private EQACycle cycle;

    private Long draft(Long analystId) {
        return draftFor(insertScheme("Lifecycle scheme", EQASchemeType.INTERNATIONAL_PT, "NHLS"), analystId);
    }

    private Long draftInHouse(Long analystId) {
        return draftFor(insertScheme("In-house lifecycle", EQASchemeType.IN_HOUSE, null), analystId);
    }

    private Long draftFor(EQAProgram scheme, Long analystId) {
        seedEnrollment(ENROLLMENT, "Lifecycle program");
        cycle = readBack(insertCycle(scheme, 1));
        Long roundId = insertRound(cycle, 1, "OPEN");

        EQAParticipantResult result = new EQAParticipantResult();
        result.setCycle(cycle);
        EQARound round = new EQARound();
        round.setId(roundId);
        result.setRound(round);
        result.setLabEnrollmentId(ENROLLMENT);
        result.setAnalyteId(ANALYTE);
        result.setResultValue("4.7");
        result.setResultUnit("log10 c/mL");
        result.setAssignedAnalystId(analystId);
        result.setSysUserId(USER);
        return resultService.saveDraft(result).getId();
    }

    private String statusInDb(Long id) {
        return jdbc.queryForObject("SELECT submission_status FROM clinlims.eqa_participant_result WHERE id = ?",
                String.class, id);
    }

    @Test
    public void happyPath_draftToValidatedToSubmittedToScored() {
        Long id = draft(null);
        assertEquals("DRAFT", statusInDb(id));

        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);
        assertEquals("VALIDATED_PARTIAL", statusInDb(id));

        resultService.transitionStatus(id, EQASubmissionStatus.SUBMITTED, USER);
        assertEquals("SUBMITTED", statusInDb(id));
        assertNotNull("submittedAt stamps on submit", jdbc.queryForObject(
                "SELECT submitted_at FROM clinlims.eqa_participant_result WHERE id = ?", java.sql.Timestamp.class, id));

        resultService.recordScore(id, EQAPerformanceStatus.ACCEPTABLE, null, USER);
        assertEquals("SCORED", statusInDb(id));
        assertNotNull(jdbc.queryForObject("SELECT score_received_at FROM clinlims.eqa_participant_result WHERE id = ?",
                java.sql.Timestamp.class, id));
    }

    @Test
    public void illegalJump_draftStraightToSubmitted_isRefused() {
        Long id = draft(null);
        try {
            resultService.transitionStatus(id, EQASubmissionStatus.SUBMITTED, USER);
            fail("DRAFT cannot jump straight to SUBMITTED");
        } catch (IllegalStateException expected) {
            assertEquals("DRAFT", statusInDb(id));
        }
    }

    @Test
    public void scoredAndMissedDeadline_areRefusedOnTheGenericPath() {
        Long id = draft(null);
        try {
            resultService.transitionStatus(id, EQASubmissionStatus.SCORED, USER);
            fail("SCORED must go through recordScore");
        } catch (IllegalStateException expected) {
        }
        try {
            resultService.transitionStatus(id, EQASubmissionStatus.MISSED_DEADLINE, USER);
            fail("MISSED_DEADLINE must go through markMissedDeadline");
        } catch (IllegalStateException expected) {
        }
        assertEquals("DRAFT", statusInDb(id));
    }

    @Test
    public void editingPastDraft_isRefused() {
        Long id = draft(null);
        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);

        EQAParticipantResult edit = new EQAParticipantResult();
        edit.setId(id);
        edit.setResultValue("9.9");
        edit.setSysUserId(USER);
        try {
            resultService.saveDraft(edit);
            fail("a VALIDATED_PARTIAL result must not be editable as a draft");
        } catch (IllegalStateException expected) {
            assertEquals("4.7", jdbc.queryForObject(
                    "SELECT result_value FROM clinlims.eqa_participant_result WHERE id = ?", String.class, id));
        }
    }

    @Test
    public void unacceptableScore_writesCompetencyEventForAssignedAnalyst() {
        Long id = draft(ANALYST);
        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);
        resultService.transitionStatus(id, EQASubmissionStatus.SUBMITTED, USER);

        resultService.recordScore(id, EQAPerformanceStatus.UNACCEPTABLE, null, USER);

        Map<String, Object> event = jdbc
                .queryForMap("SELECT analyst_id, event_type, cycle_id, participant_result_id, analyte_id"
                        + " FROM clinlims.eqa_analyst_competency_event WHERE participant_result_id = ?", id);
        assertEquals(ANALYST, ((Number) event.get("analyst_id")).longValue());
        assertEquals("UNACCEPTABLE_SCORE", event.get("event_type"));
        assertEquals(cycle.getId().longValue(), ((Number) event.get("cycle_id")).longValue());
        assertEquals(ANALYTE, ((Number) event.get("analyte_id")).longValue());
    }

    @Test
    public void acceptableScore_writesNoCompetencyEvent() {
        Long id = draft(ANALYST);
        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);
        resultService.transitionStatus(id, EQASubmissionStatus.SUBMITTED, USER);

        resultService.recordScore(id, EQAPerformanceStatus.ACCEPTABLE, null, USER);

        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_analyst_competency_event" + " WHERE participant_result_id = ?",
                Integer.class, id));
    }

    @Test
    public void questionableScoreWithNoAnalyst_writesNoCompetencyEvent() {
        Long id = draft(null);
        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);
        resultService.transitionStatus(id, EQASubmissionStatus.SUBMITTED, USER);

        resultService.recordScore(id, EQAPerformanceStatus.QUESTIONABLE, null, USER);

        assertEquals("SCORED", statusInDb(id));
        assertEquals(Integer.valueOf(0), jdbc.queryForObject(
                "SELECT count(*) FROM clinlims.eqa_analyst_competency_event" + " WHERE participant_result_id = ?",
                Integer.class, id));
    }

    @Test
    public void missedDeadline_externalScheme_writesExternalEvent() {
        Long id = draft(ANALYST);

        resultService.markMissedDeadline(id, USER);

        assertEquals("MISSED_DEADLINE", statusInDb(id));
        assertEquals("EXTERNAL_MISSED_DEADLINE", jdbc.queryForObject(
                "SELECT event_type FROM clinlims.eqa_analyst_competency_event" + " WHERE participant_result_id = ?",
                String.class, id));
    }

    @Test
    public void missedDeadline_inHouseScheme_writesInHouseEvent() {
        Long id = draftInHouse(ANALYST);

        resultService.markMissedDeadline(id, USER);

        assertEquals("IN_HOUSE_MISSED_DEADLINE", jdbc.queryForObject(
                "SELECT event_type FROM clinlims.eqa_analyst_competency_event" + " WHERE participant_result_id = ?",
                String.class, id));
    }

    @Test
    public void resultDtos_narrowByEnrollmentAndExposeLifecycleFields() {
        Long id = draft(ANALYST);
        resultService.transitionStatus(id, EQASubmissionStatus.VALIDATED_PARTIAL, USER);

        List<Map<String, Object>> mine = resultService.getResultDtos(cycle.getId(), ENROLLMENT);
        assertEquals(1, mine.size());
        assertEquals(id, mine.get(0).get("id"));
        assertEquals("VALIDATED_PARTIAL", mine.get(0).get("submissionStatus"));
        assertEquals("4.7", mine.get(0).get("resultValue"));
        assertNull(mine.get(0).get("submittedAt"));

        assertEquals(0, resultService.getResultDtos(cycle.getId(), 424242L).size());
    }
}
