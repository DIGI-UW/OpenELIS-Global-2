package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The two endpoints that take a block or a slide out of use, and the case save
 * that refuses a row the case cannot hold.
 *
 * <p>
 * A retirement is a state change on a retained record, so it is reachable only
 * with a principal, it answers with the state the row is now in, and an id
 * nothing carries is a plain not-found rather than a failure. A slide that
 * names a block belonging to no case is the client asking for something
 * untraceable, and is answered with the sentence that says so rather than as a
 * server error.
 */
public class PathologyControllerDeactivateTest extends PathologyControllerTestSupport {

    private static final int LEGACY_SLIDE_ID = 201;

    private static final String BLOCK_ENDPOINT = "/rest/pathology/block/" + LEGACY_BLOCK_ID + "/deactivate";

    private static final String SLIDE_ENDPOINT = "/rest/pathology/slide/" + LEGACY_SLIDE_ID + "/deactivate";

    private static final String CASE_VIEW_ENDPOINT = "/rest/pathology/caseView/1";

    @Test
    public void deactivate_withoutAPrincipal_isRefused() throws Exception {
        // The base class runs every test as the daemon user (@WithDaemonUser), and the
        // security post-processor hands that principal to each request, so a request
        // has to say it is anonymous or it inherits one.
        int status = mockMvc.perform(post(BLOCK_ENDPOINT).with(anonymous())).andReturn().getResponse().getStatus();

        assertEquals("taking a block out of use is a change to a retained record, so it is not reachable"
                + " without a principal", HttpStatus.UNAUTHORIZED.value(), status);
        assertTrue("and the block is untouched", isActive("pathology_block", LEGACY_BLOCK_ID));
    }

    @Test
    public void deactivateBlock_answers200WithTheRowState() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BLOCK_ENDPOINT,
                "{\"reason\":\"cross-contamination suspected\"}");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer names the block that was retired", Integer.valueOf(LEGACY_BLOCK_ID), body.get("id"));
        assertEquals("and the state it is now in, so the screen does not have to guess", Boolean.FALSE,
                body.get("active"));
    }

    @Test
    public void deactivateBlock_withoutABody_stillRetiresTheRow() throws Exception {
        MockHttpServletResponse response = postAsTechnician(BLOCK_ENDPOINT, null);

        assertEquals("the bench is not obliged to give a reason", HttpStatus.OK.value(), response.getStatus());
        assertEquals(Boolean.FALSE, bodyOf(response).get("active"));
    }

    @Test
    public void deactivateUnknownBlock_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/block/424242/deactivate", null);

        assertEquals("an id no block carries is nothing to retire, not a failure", HttpStatus.NOT_FOUND.value(),
                response.getStatus());
        assertTrue("and the answer says which id that was",
                String.valueOf(bodyOf(response).get("error")).contains("424242"));
    }

    /**
     * The pathology dashboard lists a user only the cases whose order they may read
     * by lab unit; a block or slide of any other case is not found and stays in
     * use.
     */
    @Test
    public void deactivate_ofACaseOutsideTheUsersUnits_answers404() throws Throwable {
        asAUserOutsideTheCaseUnits(() -> {
            assertEquals(HttpStatus.NOT_FOUND.value(), postAsTechnician(BLOCK_ENDPOINT, null).getStatus());
            assertEquals(HttpStatus.NOT_FOUND.value(), postAsTechnician(SLIDE_ENDPOINT, null).getStatus());
        });
        assertTrue("the block is still in use", isActive("pathology_block", LEGACY_BLOCK_ID));
        assertTrue("the slide is still in use", isActive("pathology_slide", LEGACY_SLIDE_ID));
    }

    @Test
    public void deactivateSlide_answers200WithTheRowState() throws Exception {
        MockHttpServletResponse response = postAsTechnician(SLIDE_ENDPOINT, "{\"reason\":\"section folded\"}");

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer names the slide that was retired", Integer.valueOf(LEGACY_SLIDE_ID), body.get("id"));
        assertEquals("and the state it is now in", Boolean.FALSE, body.get("active"));
    }

    @Test
    public void saveWithAForeignSlideBlock_answers400WithASentence() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW_ENDPOINT,
                "{\"status\":\"GROSSING\"," + "\"blocks\":[],\"reports\":[],\"slides\":[{\"blockId\":424242}]}");

        assertEquals("a slide that cannot be traced to a block on this case is the client asking for"
                + " something the case cannot hold", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        String error = String.valueOf(bodyOf(response).get("error"));
        assertTrue("and the answer is a sentence naming the block, not a stack trace",
                error.contains("block") && error.contains("424242"));
        assertEquals("nothing was stored", 1, slideCountOnCase(1));
    }

    @Test
    public void aTissueTypeOutsideTheCategory_isA400NamingTheRule() throws Exception {
        MockHttpServletResponse response = postAsTechnician(CASE_VIEW_ENDPOINT,
                "{\"status\":\"GROSSING\","
                        + "\"blocks\":[{\"id\":101,\"location\":\"Lab 1\",\"tissueTypeId\":\"424242\"}],"
                        + "\"reports\":[],\"slides\":[]}");

        assertEquals("a tissue type the laboratory never configured is the client asking for something the"
                + " category does not hold", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        String error = String.valueOf(bodyOf(response).get("error"));
        assertTrue("and the answer names the id and the category it is not an entry of: " + error,
                error.contains("424242") && error.contains("pathology_tissue_types"));
        assertEquals("nothing was stored on the block", 0,
                jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM clinlims.pathology_block WHERE id = ? AND tissue_type_id IS NOT NULL",
                        Integer.class, LEGACY_BLOCK_ID).intValue());
    }

    // helpers

    private int slideCountOnCase(int pathologySampleId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_slide WHERE" + " pathology_sample_id = ?", Integer.class,
                pathologySampleId);
    }
}
