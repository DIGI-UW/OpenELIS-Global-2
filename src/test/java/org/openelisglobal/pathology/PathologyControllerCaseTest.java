package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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

    /** What the case view posts, with the fields a test adds. */
    private static String saveBody(String extraFields) {
        return "{\"grossExam\":\"Normal tissues observed\",\"blocks\":[],\"reports\":[],\"slides\":[]" + extraFields
                + "}";
    }
}
