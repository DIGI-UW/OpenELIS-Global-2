package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The case view's own read and save, as the screen calls them.
 *
 * <p>
 * A case id nothing carries is not found, but a missing row the save only
 * refers to is not the case being gone; and a save the case's stage refuses is
 * answered with the message the screen translates.
 */
public class PathologyControllerCaseTest extends PathologyControllerTestSupport {

    private static final String CASE_VIEW = "/rest/pathology/caseView/" + CASE_ID;

    private StageSwitches switches;

    @Before
    public void switchEveryStageOn() {
        switches = StageSwitches.allOn();
    }

    @After
    public void restoreTheSwitches() {
        switches.close();
    }

    /**
     * A case id nothing carries is not found, whether the case view is read or
     * saved, rather than a server error.
     */
    @Test
    public void anUnknownCase_answers404OnReadAndSave() throws Exception {
        MockHttpServletResponse read = mockMvc.perform(get("/rest/pathology/caseView/424242").with(user("technician1")))
                .andReturn().getResponse();
        MockHttpServletResponse save = postAsTechnician("/rest/pathology/caseView/424242", saveBody(""));

        assertEquals("reading a case nobody has is not found", HttpStatus.NOT_FOUND.value(), read.getStatus());
        assertTrue("and the answer names the id", String.valueOf(bodyOf(read).get("error")).contains("424242"));
        assertEquals("saving one is not found either", HttpStatus.NOT_FOUND.value(), save.getStatus());
    }

    /**
     * A pathologist id no user carries is a failure of the save, not a missing
     * case, so the screen is never told the case is gone.
     */
    @Test
    public void aSaveNamingAnUnknownPathologist_isNotAnsweredAsAMissingCase() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW,
                saveBody(",\"assignedPathologistId\":\"424242\""));

        assertNotEquals("the case exists, so the answer is not not-found", HttpStatus.NOT_FOUND.value(),
                response.getStatus());
        assertEquals("it stays the server error any unexpected failure is", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                response.getStatus());
    }

    /** The pathologist signs a case out from the review queue only. */
    @Test
    public void aReleaseFromABenchStage_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW, saveBody(",\"release\":true"));

        assertEquals("a case at grossing is not signed out", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("and the answer carries the message the screen translates", "pathology.error.releaseBeforeReview",
                bodyOf(response).get("messageKey"));
        assertEquals("the case stays at grossing", "GROSSING", storedStage());
    }

    /** The processor run fits its column. */
    @Test
    public void aRunReferenceLongerThanTheColumn_answers400WithTheMessageId() throws Exception {
        assertEquals("setup: the case is sent on to processing", HttpStatus.OK.value(),
                postAsTechnician(CASE_VIEW + "/advance", "{\"from\":\"GROSSING\"}").getStatus());

        MockHttpServletResponse response = postAsTechnician(CASE_VIEW,
                saveBody(",\"runReference\":\"" + StringUtils.repeat("R", 65) + "\""));

        assertEquals("a run longer than its column is refused", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("with the message the screen translates", "pathology.error.runReferenceTooLong",
                bodyOf(response).get("messageKey"));
        assertEquals("and nothing is stored", Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_stage_event"
                                + " WHERE pathology_sample_id = ? AND run_reference IS NOT NULL",
                        Integer.class, CASE_ID));
    }

    /** A slide's level fits its column. */
    @Test
    public void aLevelLongerThanTheColumn_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW,
                "{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],\"slides\":[{\"id\":"
                        + LEGACY_SLIDE_ID + ",\"level\":\"" + StringUtils.repeat("L", 17) + "\"}]}");

        assertEquals("a level longer than its column is refused", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("with the message the screen translates", "pathology.error.levelTooLong",
                bodyOf(response).get("messageKey"));
        assertEquals("and the slide keeps no level", Integer.valueOf(0),
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_slide WHERE id = ? AND level IS NOT NULL",
                        Integer.class, LEGACY_SLIDE_ID));
    }

    /**
     * A slide is cut from an embedded block where Embedding is tracked; the refusal
     * names the cassette so the screen can say which one to embed.
     */
    @Test
    public void aSaveCuttingASlideFromACassette_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW,
                "{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],\"slides\":[{\"blockId\":"
                        + LEGACY_BLOCK_ID + "}]}");

        assertEquals("a slide from a cassette nobody embedded is refused", HttpStatus.BAD_REQUEST.value(),
                response.getStatus());
        assertEquals("with the message the screen translates", "pathology.error.slideFromCassette",
                bodyOf(response).get("messageKey"));
        assertEquals("naming the cassette by its legacy number", Map.of("designation", "11"),
                bodyOf(response).get("messageArgs"));
        assertEquals("and no slide is cut from it", Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_slide WHERE block_id = ?", Integer.class, LEGACY_BLOCK_ID));
    }

    /**
     * Each new refusal of the case save reaches the screen as its message id, with
     * the retired diagnosis named.
     */
    @Test
    public void aRefusedFinding_isAnsweredWithItsMessageKey() throws Exception {
        try (PathologyFindingsDictionary dictionary = PathologyFindingsDictionary.seed(jdbcTemplate)) {
            assertRefused("a reading written on the bench", saveBody(",\"microscopyExam\":\"Changed\""),
                    "pathology.error.findingsLocked");

            storeAt(PathologyStatus.UNDER_REVIEW);
            assertRefused("a narrative longer than its column",
                    saveBody(",\"conclusionText\":\"" + StringUtils.repeat("n", 256) + "\""),
                    "pathology.error.conclusionTooLong");
            Map<String, Object> retired = assertRefused("a retired diagnosis",
                    saveBody(",\"conclusions\":[\"" + PathologyFindingsDictionary.OLD_TERM + "\"]"),
                    "pathology.error.conclusionNotInUse");
            assertEquals("naming it", Map.of("name", "Old term"), retired.get("messageArgs"));
            assertRefused("a release with a blank microscopic description",
                    saveBody(",\"microscopyExam\":\" \",\"release\":true"),
                    "pathology.error.releaseWithoutDescriptions");

            assertEquals("setup: the case is signed out", HttpStatus.OK.value(),
                    postAsTechnician(CASE_VIEW, saveBody(",\"release\":true")).getStatus());
            assertRefused("an edit after the sign-out", saveBody(",\"microscopyExam\":\"Changed\""),
                    "pathology.error.caseCompleted");
        }
    }

    /**
     * A case a pathologist is reading is bench work still: the dashboard counts it
     * as in progress and no longer as awaiting review.
     */
    @Test
    public void aCaseUnderReview_countsAsInProgress_notAsAwaitingReview() throws Exception {
        storeAt(PathologyStatus.READY_PATHOLOGIST);
        Map<String, Object> inTheQueue = counts();

        storeAt(PathologyStatus.UNDER_REVIEW);
        Map<String, Object> underReview = counts();

        assertEquals("the case leaves the queue's tile", number(inTheQueue, "awaitingReview") - 1,
                number(underReview, "awaitingReview"));
        assertEquals("and joins work in progress", number(inTheQueue, "inProgress") + 1,
                number(underReview, "inProgress"));
    }

    private Map<String, Object> counts() throws Exception {
        MockHttpServletResponse response = mockMvc
                .perform(get("/rest/pathology/dashboard/count").with(user("technician1"))).andReturn().getResponse();
        assertEquals("the tiles are served", HttpStatus.OK.value(), response.getStatus());
        return bodyOf(response);
    }

    private static long number(Map<String, Object> counts, String tile) {
        return ((Number) counts.get(tile)).longValue();
    }

    private Map<String, Object> assertRefused(String what, String body, String messageKey) throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW, body);
        assertEquals(what + " is refused", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals(what + ", with the message the screen translates", messageKey, answer.get("messageKey"));
        return answer;
    }

    /** What the case view posts, with the fields a test adds. */
    private static String saveBody(String extraFields) {
        return "{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],\"slides\":[]" + extraFields
                + "}";
    }
}
