package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED;
import static org.openelisglobal.pathology.PathologyConsultationFixture.CONSULTED_NAME;
import static org.openelisglobal.pathology.PathologyConsultationFixture.OUTSIDER;
import static org.openelisglobal.pathology.PathologyConsultationFixture.REQUESTER;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.openelisglobal.audittrail.valueholder.History;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LocalizedValidationException;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.service.ConsultationAnswer;
import org.openelisglobal.program.service.ConsultationDraft;
import org.openelisglobal.program.service.PathologyConsultationConflictException;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.valueholder.pathology.ConsultationStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Answering and withdrawing a second opinion (FR-11.4, FR-11.5): only the
 * pathologist asked records the outcome, anyone withdraws, only an awaited one
 * takes either, wherever the case stands, and the note never reaches the audit
 * trail.
 */
public class PathologyConsultationAnswerTest extends PathologyCaseTestSupport {

    private static final String NOTE = "Reactive atypia rather than dysplasia";

    @Autowired
    private PathologyConsultationService consultationService;

    private PathologyConsultationFixture fixture;
    private StageSwitches switches;
    private String consultationReferenceTableId;

    @Before
    public void seedThePeopleAndReasons() {
        switches = StageSwitches.allOn();
        fixture = PathologyConsultationFixture.seed(jdbcTemplate);
        consultationReferenceTableId = referenceTablesService
                .getReferenceTableByName(PathologyConsultationService.REFERENCE_TABLE).getId();
        storeAt(PathologyStatus.UNDER_REVIEW);
    }

    @After
    public void removeThePeopleAndReasons() {
        fixture.close();
        switches.close();
    }

    @Test
    public void theConsultedPathologist_recordsEachOutcome_withANote_andTheAnswerTime() {
        for (String outcome : List.of("AGREE", "DISAGREE", "MODIFIED")) {
            int id = awaited();
            Map<String, Object> asked = row(id);
            long before = System.currentTimeMillis();

            consultationService.answer(CASE_ID, id, new ConsultationAnswer(outcome, "  " + NOTE + "  "), CONSULTED);

            long after = System.currentTimeMillis();
            Map<String, Object> answered = row(id);
            assertEquals("answered", "ANSWERED", answered.get("status"));
            assertEquals("with the outcome", outcome, answered.get("outcome"));
            assertEquals("and the note, trimmed", NOTE, answered.get("outcome_note"));
            long answeredAt = ((Timestamp) answered.get("answered_at")).getTime();
            assertTrue("at the server clock", answeredAt >= before && answeredAt <= after);
            assertEquals("asked as before", asked.get("requested_at"), answered.get("requested_at"));
            assertEquals(asked.get("requested_by"), answered.get("requested_by"));
        }
    }

    @Test
    public void theOutcomeIsAColumnThatCanBeCounted() {
        for (String outcome : List.of("DISAGREE", "DISAGREE", "AGREE")) {
            consultationService.answer(CASE_ID, awaited(), new ConsultationAnswer(outcome, null), CONSULTED);
        }

        assertEquals(2,
                (int) jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_consultation WHERE outcome = 'DISAGREE'",
                        Integer.class));
        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_consultation WHERE outcome = 'AGREE'", Integer.class));
    }

    @Test
    public void anAnswerIsAudited_withTheOutcomeOnly_neverTheNote() {
        int id = awaited();

        consultationService.answer(CASE_ID, id, new ConsultationAnswer("DISAGREE", NOTE), CONSULTED);

        List<History> answers = entries(id, PathologyConsultationService.PATHOLOGY_CONSULTATION_ANSWERED);
        assertEquals("one entry", 1, answers.size());
        assertEquals("as an update", IActionConstants.AUDIT_TRAIL_UPDATE, answers.get(0).getActivity());
        assertEquals("by the consulted pathologist", CONSULTED, answers.get(0).getSysUserId());
        assertEquals("the outcome only",
                Map.of("verb", PathologyConsultationService.PATHOLOGY_CONSULTATION_ANSWERED, "outcome", "DISAGREE"),
                payloadOf(answers.get(0)));
        for (History entry : allEntries(id)) {
            assertFalse("no entry carries the note", changesOf(entry).contains("Reactive atypia"));
        }
    }

    @Test
    public void onlyTheConsultedPathologistAnswers() {
        int id = awaited();
        Map<String, Object> before = row(id);

        for (String actor : List.of(REQUESTER, OUTSIDER)) {
            LocalizedValidationException refusal = assertRefusedWith("pathology.error.consultationNotYours",
                    () -> consultationService.answer(CASE_ID, id, new ConsultationAnswer("AGREE", NOTE), actor));
            assertEquals("naming the consulted pathologist", Map.of("name", CONSULTED_NAME), refusal.getParams());
        }

        assertEquals("the row unchanged", before, row(id));
        assertTrue("no answer audited",
                entries(id, PathologyConsultationService.PATHOLOGY_CONSULTATION_ANSWERED).isEmpty());
    }

    @Test
    public void anAnswerWithoutAnOutcome_orWithAnUnknownOne_isRefused() {
        int id = awaited();
        Map<String, Object> before = row(id);

        assertRefusedWith("pathology.error.consultationOutcomeRequired",
                () -> consultationService.answer(CASE_ID, id, null, CONSULTED));
        for (String outcome : new String[] { null, " ", "MAYBE", "agree" }) {
            assertRefusedWith("pathology.error.consultationOutcomeRequired",
                    () -> consultationService.answer(CASE_ID, id, new ConsultationAnswer(outcome, NOTE), CONSULTED));
        }
        assertEquals("the row unchanged", before, row(id));
    }

    @Test
    public void aNoteOverTheLimit_isRefused_andABlankOneIsStoredAsNone() {
        int id = awaited();
        Map<String, Object> before = row(id);

        assertRefusedWith("pathology.error.consultationNoteTooLong", () -> consultationService.answer(CASE_ID, id,
                new ConsultationAnswer("AGREE", StringUtils.repeat('n', 1001)), CONSULTED));
        assertEquals("the row unchanged", before, row(id));

        consultationService.answer(CASE_ID, id, new ConsultationAnswer("AGREE", StringUtils.repeat('n', 1000)),
                CONSULTED);
        assertEquals("1,000 characters are taken", 1000, ((String) row(id).get("outcome_note")).length());

        int blank = awaited();
        consultationService.answer(CASE_ID, blank, new ConsultationAnswer("AGREE", "   "), CONSULTED);
        assertNull("a blank note is none", row(blank).get("outcome_note"));
    }

    @Test
    public void anAnsweredOrWithdrawnRow_takesNoAnswerAndNoWithdrawal() {
        int answered = awaited();
        consultationService.answer(CASE_ID, answered, new ConsultationAnswer("AGREE", NOTE), CONSULTED);
        int withdrawn = awaited();
        consultationService.withdraw(CASE_ID, withdrawn, REQUESTER);
        Map<String, Object> answeredRow = row(answered);
        Map<String, Object> withdrawnRow = row(withdrawn);
        int entriesBefore = consultationAuditCount();

        assertConflict(ConsultationStatus.ANSWERED, () -> consultationService.answer(CASE_ID, answered,
                new ConsultationAnswer("DISAGREE", "Changed my mind"), CONSULTED));
        assertConflict(ConsultationStatus.ANSWERED, () -> consultationService.withdraw(CASE_ID, answered, REQUESTER));
        assertConflict(ConsultationStatus.WITHDRAWN,
                () -> consultationService.answer(CASE_ID, withdrawn, new ConsultationAnswer("AGREE", null), CONSULTED));
        assertConflict(ConsultationStatus.WITHDRAWN, () -> consultationService.withdraw(CASE_ID, withdrawn, REQUESTER));

        assertEquals("the answered row unchanged", answeredRow, row(answered));
        assertEquals("the withdrawn row unchanged", withdrawnRow, row(withdrawn));
        assertEquals("no verb", entriesBefore, consultationAuditCount());
    }

    @Test
    public void aWithdrawal_marksTheRowWithdrawn_byWhoeverAsks_keepsTheRow_andIsAudited() {
        for (String actor : List.of(REQUESTER, OUTSIDER)) {
            int id = awaited();

            consultationService.withdraw(CASE_ID, id, actor);

            Map<String, Object> withdrawn = row(id);
            assertEquals("withdrawn by " + actor, "WITHDRAWN", withdrawn.get("status"));
            assertNull("no outcome", withdrawn.get("outcome"));
            assertNull("no answer time", withdrawn.get("answered_at"));
            List<History> entries = entries(id, PathologyConsultationService.PATHOLOGY_CONSULTATION_WITHDRAWN);
            assertEquals("one entry", 1, entries.size());
            assertEquals("as an update", IActionConstants.AUDIT_TRAIL_UPDATE, entries.get(0).getActivity());
            assertEquals("by the actor", actor, entries.get(0).getSysUserId());
            assertEquals("the verb only", Map.of("verb", PathologyConsultationService.PATHOLOGY_CONSULTATION_WITHDRAWN),
                    payloadOf(entries.get(0)));
        }
        assertEquals("both rows kept", 2, consultationCount());
    }

    @Test
    public void aRowOfAnotherCase_orAnIdThatNamesNoRow_isNotOnThisCase() {
        jdbcTemplate.update("INSERT INTO clinlims.pathology_consultation (id, pathology_sample_id, requested_by,"
                + " consulted_user_id, pre_treatment, status, requested_at, last_updated)"
                + " VALUES (nextval('clinlims.pathology_consultation_seq'), 2, 1002, 1003, true, 'REQUESTED', now(),"
                + " now())");
        int otherCases = jdbcTemplate.queryForObject(
                "SELECT id FROM clinlims.pathology_consultation WHERE pathology_sample_id = 2", Integer.class);
        Map<String, Object> before = row(otherCases);

        for (Integer id : new Integer[] { otherCases, 999999, null }) {
            assertRefusedWith("pathology.error.consultationNotOnCase",
                    () -> consultationService.answer(CASE_ID, id, new ConsultationAnswer("AGREE", null), CONSULTED));
            assertRefusedWith("pathology.error.consultationNotOnCase",
                    () -> consultationService.withdraw(CASE_ID, id, REQUESTER));
        }
        assertEquals("the other case's row as it was", before, row(otherCases));
    }

    @Test
    public void anAnswerOrAWithdrawal_isTakenWhereverTheCaseStands() {
        for (PathologyStatus stage : List.of(PathologyStatus.GROSSING, PathologyStatus.READY_PATHOLOGIST,
                PathologyStatus.UNDER_REVIEW, PathologyStatus.COMPLETED)) {
            storeAt(PathologyStatus.UNDER_REVIEW);
            int toAnswer = awaited();
            storeAt(stage);
            consultationService.answer(CASE_ID, toAnswer, new ConsultationAnswer("AGREE", null), CONSULTED);
            assertEquals("answered at " + stage, "ANSWERED", row(toAnswer).get("status"));

            storeAt(PathologyStatus.UNDER_REVIEW);
            int toWithdraw = awaited();
            storeAt(stage);
            consultationService.withdraw(CASE_ID, toWithdraw, REQUESTER);
            assertEquals("withdrawn at " + stage, "WITHDRAWN", row(toWithdraw).get("status"));
        }

        storeAt(PathologyStatus.UNDER_REVIEW);
        int toAnswer = awaited();
        int toWithdraw = consultationService.request(CASE_ID, new ConsultationDraft(REQUESTER, null, true), CONSULTED)
                .getId();
        signOut();
        consultationService.answer(CASE_ID, toAnswer, new ConsultationAnswer("DISAGREE", null), CONSULTED);
        consultationService.withdraw(CASE_ID, toWithdraw, CONSULTED);
        assertEquals("answered once signed out", "ANSWERED", row(toAnswer).get("status"));
        assertEquals("withdrawn once signed out", "WITHDRAWN", row(toWithdraw).get("status"));
    }

    // helpers

    /** A second opinion asked of the consulted pathologist by the requester. */
    private int awaited() {
        return consultationService.request(CASE_ID, new ConsultationDraft(CONSULTED, null, true), REQUESTER).getId();
    }

    private void assertConflict(ConsultationStatus stored, ThrowingRunnable call) {
        PathologyConsultationConflictException conflict = assertThrows(PathologyConsultationConflictException.class,
                call);
        assertEquals("the conflict carries the stored status", stored, conflict.getStoredStatus());
    }

    private void signOut() {
        closeTheFixtureRequest();
        PathologySampleForm form = caseViewForm();
        form.setRelease(true);
        save(form);
        assertEquals("setup: the case is signed out", "COMPLETED", storedStage());
    }

    private Map<String, Object> row(int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_consultation WHERE id = ?", id);
    }

    private int consultationCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.pathology_consultation", Integer.class);
    }

    private int consultationAuditCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_table = ?",
                Integer.class, Integer.parseInt(consultationReferenceTableId));
    }

    private List<History> allEntries(int id) {
        return historyService.getHistoryByRefIdAndRefTableId(String.valueOf(id), consultationReferenceTableId);
    }

    private List<History> entries(int id, String verb) {
        return allEntries(id).stream().filter(entry -> verb.equals(payloadOf(entry).get("verb"))).toList();
    }
}
