package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The two endpoints that move a pathology case between stages.
 *
 * <p>
 * A move answers with the stage the case now stands at, and the screen reads
 * the case again for the rest; every refusal is a status the screen can
 * explain: a message id for an unfinished stage or a missing reason, a conflict
 * for a page another user has overtaken, and not-found for a case the user may
 * not read. Who may reach the endpoints at all, and the conflict a lost version
 * check answers, are pinned in
 * {@code PathologyStageRestControllerSecurityTest}.
 */
public class PathologyStageRestControllerTest extends PathologyControllerTestSupport {

    private static final String ADVANCE = "/rest/pathology/caseView/" + CASE_ID + "/advance";

    private static final String RETURN = "/rest/pathology/caseView/" + CASE_ID + "/return";

    private StageSwitches switches;

    /**
     * Every optional stage is switched on, so the fixture's unflagged case at
     * Grossing moves to Processing; the switches are put back afterwards.
     */
    @Before
    public void switchEveryStageOn() {
        switches = StageSwitches.allOn();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    /** The answer is the stage the case now stands at, and the move is recorded. */
    @Test
    public void advance_answers200WithTheNewStage() throws Exception {
        MockHttpServletResponse response = postAsTechnician(ADVANCE, "{\"from\":\"GROSSING\"}");

        assertEquals("the move is accepted", HttpStatus.OK.value(), response.getStatus());
        assertEquals("an unflagged specimen goes from grossing to processing, and that is all the answer says",
                Map.of("status", "PROCESSING"), bodyOf(response));
        assertEquals("which is the stage stored on the case", "PROCESSING", storedStage());
        Map<String, Object> entry = latestEntry();
        assertEquals("the move is recorded onto the new stage", "PROCESSING", entry.get("stage"));
        assertEquals("going forward", "FORWARD", entry.get("direction"));
        assertEquals("by the signed-in user", 1001, ((Number) entry.get("entered_by")).intValue());
    }

    /** A return names the stage the case went back to and why. */
    @Test
    public void return_answers200WithTheReason() throws Exception {
        assertEquals("setup: the case is sent on first", HttpStatus.OK.value(),
                postAsTechnician(ADVANCE, "{\"from\":\"GROSSING\"}").getStatus());

        MockHttpServletResponse response = postAsTechnician(RETURN,
                "{\"from\":\"PROCESSING\",\"reason\":\"cassette A1 under-fixed\"}");

        assertEquals("the return is accepted", HttpStatus.OK.value(), response.getStatus());
        assertEquals("the case is back at grossing", Map.of("status", "GROSSING"), bodyOf(response));
        assertEquals("as stored", "GROSSING", storedStage());
        Map<String, Object> entry = latestEntry();
        assertEquals("the entry records a return", "RETURN", entry.get("direction"));
        assertEquals("with the reason given", "cassette A1 under-fixed", entry.get("reason"));
    }

    @Test
    public void advanceAnUnknownCase_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/caseView/424242/advance",
                "{\"from\":\"GROSSING\"}");

        assertEquals("an id no case carries is nothing to move, not a server error", HttpStatus.NOT_FOUND.value(),
                response.getStatus());
        assertTrue("and the answer names the id", String.valueOf(bodyOf(response).get("error")).contains("424242"));
    }

    /** A case outside the user's lab units is not found, and does not move. */
    @Test
    public void advance_ofACaseOutsideTheUsersUnits_answers404() throws Throwable {
        asAUserOutsideTheCaseUnits(() -> {
            assertEquals("a case the user may not read cannot be sent on", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(ADVANCE, "{\"from\":\"GROSSING\"}").getStatus());
            assertEquals("nor sent back", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(RETURN, "{\"from\":\"GROSSING\",\"reason\":\"re-gross\"}").getStatus());
        });
        assertEquals("and it stays where it was", "GROSSING", storedStage());
        assertEquals("with no entry written", 0, stageEventCount());
    }

    /** A page still showing a stage the case has left is told where the case is. */
    @Test
    public void advanceFromAStageTheCaseHasLeft_answers409NamingTheStoredStage() throws Exception {
        MockHttpServletResponse response = postAsTechnician(ADVANCE, "{\"from\":\"EMBEDDING\"}");

        assertEquals("a stale page is a conflict, not a second move", HttpStatus.CONFLICT.value(),
                response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer says what kind", "stage", body.get("conflict"));
        assertEquals("and where the case really stands", "GROSSING", body.get("status"));
        assertEquals("which is unchanged", "GROSSING", storedStage());
        assertEquals("and nothing was recorded", 0, stageEventCount());
    }

    /** Grossing is finished once the specimen is described (FR-3.4). */
    @Test
    public void advanceFromAnUnfinishedGrossing_answers400WithTheMessageId() throws Exception {
        jdbcTemplate.update("UPDATE clinlims.pathology_sample SET gross_exam = NULL WHERE id = ?", CASE_ID);

        MockHttpServletResponse response = postAsTechnician(ADVANCE, "{\"from\":\"GROSSING\"}");

        assertEquals("an undescribed specimen is not sent on", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer carries the message the screen translates", "pathology.error.grossingIncomplete",
                body.get("messageKey"));
        assertNotNull("with its values, empty or not", body.get("messageArgs"));
        assertEquals("the case stays at grossing", "GROSSING", storedStage());
    }

    /** A return without a reason is refused (FR-2.5). */
    @Test
    public void returnWithABlankReason_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(RETURN, "{\"from\":\"GROSSING\",\"reason\":\"   \"}");

        assertEquals("a case is not sent back without saying why", HttpStatus.BAD_REQUEST.value(),
                response.getStatus());
        assertEquals("and the answer names the rule", "pathology.error.returnReasonRequired",
                bodyOf(response).get("messageKey"));
        assertEquals("the case has not moved", "GROSSING", storedStage());
    }

    /** Without the stage the page showed, the move cannot be checked against it. */
    @Test
    public void advanceWithoutAStageOrWithAnUnknownOne_answers400FromRequired() throws Exception {
        MockHttpServletResponse absent = postAsTechnician(ADVANCE, "{}");
        MockHttpServletResponse unknown = postAsTechnician(ADVANCE, "{\"from\":\"SLICING\"}");

        assertEquals("a move that does not say where the case is is refused", HttpStatus.BAD_REQUEST.value(),
                absent.getStatus());
        assertEquals("with the message the screen translates", "pathology.error.fromRequired",
                bodyOf(absent).get("messageKey"));
        assertEquals("a stage name that does not exist is the same mistake", HttpStatus.BAD_REQUEST.value(),
                unknown.getStatus());
        assertEquals("and is answered the same way", "pathology.error.fromRequired", bodyOf(unknown).get("messageKey"));
        assertEquals("the case has not moved", "GROSSING", storedStage());
    }

    // helpers

    private int stageEventCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_stage_event WHERE pathology_sample_id = ?", Integer.class,
                CASE_ID);
    }

    private Map<String, Object> latestEntry() {
        return jdbcTemplate
                .queryForMap("SELECT stage, direction, entered_by, reason FROM clinlims.pathology_stage_event"
                        + " WHERE pathology_sample_id = ? ORDER BY id DESC LIMIT 1", CASE_ID);
    }
}
