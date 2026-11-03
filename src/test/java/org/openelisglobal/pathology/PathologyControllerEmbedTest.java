package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.persistence.OptimisticLockException;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The endpoint that embeds a cassette into a block.
 *
 * <p>
 * Embedding is a state change on a retained record, so it is reachable only
 * with a principal, it answers with the state the row is now in, an id nothing
 * carries is a plain not-found, and a retired cassette is refused with a
 * sentence rather than a server error.
 */
public class PathologyControllerEmbedTest extends PathologyControllerTestSupport {

    private static final String EMBED_ENDPOINT = "/rest/pathology/block/" + LEGACY_BLOCK_ID + "/embed";

    private static final String DEACTIVATE_ENDPOINT = "/rest/pathology/block/" + LEGACY_BLOCK_ID + "/deactivate";

    @Before
    public void makeTheLegacyBlockACassette() {
        // The fixture's block carries no state; recording it as a cassette lets each
        // test read the transition plainly. The row already carries last_updated, its
        // optimistic-lock version, so the application can still update it.
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET cassette_state = 'CASSETTE' WHERE id = ?",
                LEGACY_BLOCK_ID);
    }

    @Test
    public void embed_withoutAPrincipal_isRefused() throws Exception {
        // The base class runs every test as the daemon user (@WithDaemonUser), and the
        // security post-processor hands that principal to each request, so a request
        // has to say it is anonymous or it inherits one.
        int status = mockMvc.perform(post(EMBED_ENDPOINT).with(anonymous())).andReturn().getResponse().getStatus();

        assertEquals("embedding changes a retained record, so it is not reachable without a principal",
                HttpStatus.UNAUTHORIZED.value(), status);
        assertEquals("and the cassette is untouched", "CASSETTE", cassetteStateOf(LEGACY_BLOCK_ID));
    }

    @Test
    public void embed_answers200WithTheRowState() throws Exception {
        MockHttpServletResponse response = postAsTechnician(EMBED_ENDPOINT, null);

        assertEquals("the embed is accepted", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> body = bodyOf(response);
        assertEquals("the answer names the row that was embedded", Integer.valueOf(LEGACY_BLOCK_ID), body.get("id"));
        assertEquals("and the state it is now in, so the screen does not have to guess", "BLOCK",
                body.get("cassetteState"));
        assertEquals("which is what was stored", "BLOCK", cassetteStateOf(LEGACY_BLOCK_ID));
    }

    @Test
    public void embedUnknownBlock_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/block/424242/embed", null);

        assertEquals("an id no block carries is nothing to embed, not a failure", HttpStatus.NOT_FOUND.value(),
                response.getStatus());
        assertTrue("and the answer says which id that was",
                String.valueOf(bodyOf(response).get("error")).contains("424242"));
    }

    @Test
    public void embed_ofACaseOutsideTheUsersUnits_answers404() throws Throwable {
        asAUserOutsideTheCaseUnits(() -> assertEquals("a cassette of a case the user may not read is not found",
                HttpStatus.NOT_FOUND.value(), postAsTechnician(EMBED_ENDPOINT, null).getStatus()));
        assertEquals("and stays a cassette", "CASSETTE", cassetteStateOf(LEGACY_BLOCK_ID));
    }

    @Test
    public void embedARetiredCassette_answers400WithASentence() throws Exception {
        assertEquals("the cassette is retired first", HttpStatus.OK.value(),
                postAsTechnician(DEACTIVATE_ENDPOINT, null).getStatus());

        MockHttpServletResponse response = postAsTechnician(EMBED_ENDPOINT, null);

        assertEquals("a retired cassette cannot become a block", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        String error = String.valueOf(bodyOf(response).get("error"));
        assertTrue("and the answer is a sentence naming the block, not a stack trace: " + error,
                error.contains(String.valueOf(LEGACY_BLOCK_ID)) && error.contains("retired"));
        assertEquals("the row keeps its state", "CASSETTE", cassetteStateOf(LEGACY_BLOCK_ID));
    }

    /**
     * Two embeds of one cassette race on the row's version check; the one that
     * loses is a conflict the screen can explain, not a server error.
     */
    @Test
    public void embed_thatLosesTheVersionCheck_answers409() throws Exception {
        Object controller = AopTestUtils.getUltimateTargetObject(securityContext.getBean(PathologyController.class));
        PathologySampleService real = (PathologySampleService) ReflectionTestUtils.getField(controller,
                "pathologySampleService");
        PathologySampleService losing = Mockito.mock(PathologySampleService.class, AdditionalAnswers.delegatesTo(real));
        doThrow(new OptimisticLockException("version")).when(losing).embedBlock(any(), any());
        ReflectionTestUtils.setField(controller, "pathologySampleService", losing);
        try {
            MockHttpServletResponse response = postAsTechnician(EMBED_ENDPOINT, null);

            assertEquals("the embed that lost the race is a conflict", HttpStatus.CONFLICT.value(),
                    response.getStatus());
            assertEquals("and the answer says what kind", "version", bodyOf(response).get("conflict"));
        } finally {
            ReflectionTestUtils.setField(controller, "pathologySampleService", real);
        }
        assertEquals("the stored row is untouched", "CASSETTE", cassetteStateOf(LEGACY_BLOCK_ID));
    }

    // helpers

    private String cassetteStateOf(int blockId) {
        return jdbcTemplate.queryForObject("SELECT cassette_state FROM clinlims.pathology_block WHERE id = ?",
                String.class, blockId);
    }
}
