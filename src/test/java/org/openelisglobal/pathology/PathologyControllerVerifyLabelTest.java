package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.persistence.OptimisticLockException;
import java.sql.Timestamp;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openelisglobal.program.controller.pathology.PathologyController;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The endpoint that checks a slide's printed label against its block.
 *
 * <p>
 * The check stamps a retained record, so it is reachable only with a principal;
 * a match answers with who verified the slide and when, a mismatch is a
 * conflict naming both identifiers as the screen received them, a blank field
 * is a refusal the screen translates, and an id nothing carries is a plain
 * not-found.
 */
public class PathologyControllerVerifyLabelTest extends PathologyControllerTestSupport {

    private static final String VERIFY_ENDPOINT = "/rest/pathology/slide/" + LEGACY_SLIDE_ID + "/verify-label";

    @Autowired
    private SystemUserService systemUserService;

    private String blockBarcode;
    private String slideBarcode;

    /**
     * The fixture's slide is given a block and the barcodes a cut would have
     * written, so the endpoint has a label to check; the case stands at Microtomy.
     */
    @Before
    public void giveTheSlideABlock() {
        String accessionNumber = jdbcTemplate.queryForObject(
                "SELECT s.accession_number FROM clinlims.sample s"
                        + " JOIN clinlims.pathology_sample ps ON ps.sample_id = s.id WHERE ps.id = ?",
                String.class, CASE_ID);
        blockBarcode = accessionNumber + ".A1";
        slideBarcode = accessionNumber + ".A1.1";
        jdbcTemplate.update("UPDATE clinlims.pathology_block SET designation = 'A1', barcode = ?,"
                + " cassette_state = 'BLOCK' WHERE id = ?", blockBarcode, LEGACY_BLOCK_ID);
        jdbcTemplate.update(
                "UPDATE clinlims.pathology_slide SET block_id = ?, designation = '1', barcode = ?" + " WHERE id = ?",
                LEGACY_BLOCK_ID, slideBarcode, LEGACY_SLIDE_ID);
        PathologyCaseRows.clearLabelStamp(jdbcTemplate, LEGACY_SLIDE_ID);
        storeAt(PathologyStatus.MICROTOMY);
    }

    @Test
    public void verify_withoutAPrincipal_isRefused() throws Exception {
        int status = mockMvc.perform(post(VERIFY_ENDPOINT).with(anonymous()).contentType(MediaType.APPLICATION_JSON)
                .content(body(blockBarcode, slideBarcode))).andReturn().getResponse().getStatus();

        assertEquals("verifying stamps a retained record, so it is not reachable without a principal",
                HttpStatus.UNAUTHORIZED.value(), status);
        assertNull("and the slide stays unverified", verifiedAt());
    }

    @Test
    public void verify_withMatchingBarcodes_answers200WithWhoVerifiedIt() throws Exception {
        MockHttpServletResponse response = postAsTechnician(VERIFY_ENDPOINT, body(blockBarcode, slideBarcode));

        assertEquals("the label matches its block", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the answer names the slide", Integer.valueOf(LEGACY_SLIDE_ID), answer.get("id"));
        assertNotNull("the slide is stamped", verifiedAt());
        @SuppressWarnings("unchecked")
        Map<String, Object> verification = (Map<String, Object>) answer.get("labelVerification");
        assertNotNull("the answer says who verified it and when, so the screen need not reload", verification);
        assertEquals("by the signed-in user's name", systemUserService.get("1001").getDisplayName(),
                verification.get("verifiedBy"));
        assertNotNull("at a time the server read", verification.get("verifiedAt"));
    }

    @Test
    public void verify_withAnotherBlocksBarcode_answers409NamingBoth() throws Exception {
        MockHttpServletResponse response = postAsTechnician(VERIFY_ENDPOINT, body("OTHER.A2", slideBarcode));

        assertEquals("a mismatch is a conflict", HttpStatus.CONFLICT.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("of the label kind", "label", answer.get("conflict"));
        assertEquals("with the message the screen translates", "pathology.banner.labelMismatch",
                answer.get("messageKey"));
        assertEquals("naming the block and the slide as scanned",
                Map.of("slideBarcode", slideBarcode, "blockBarcode", "OTHER.A2"), answer.get("messageArgs"));
        assertFalse("the two identifiers are given once, as the message's values",
                answer.containsKey("block") || answer.containsKey("slide"));
        assertFalse("the body carries no status, which the screen's request helper overwrites",
                answer.containsKey("status"));
        assertNull("the slide stays unverified", verifiedAt());
        assertEquals("but the mismatch is recorded", 1, auditEntriesOnThisSlide());
    }

    @Test
    public void verifyUnknownSlide_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/slide/424242/verify-label",
                body(blockBarcode, slideBarcode));

        assertEquals("an id no slide carries is not found", HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertTrue(String.valueOf(bodyOf(response).get("error")).contains("424242"));
    }

    @Test
    public void verify_ofACaseOutsideTheUsersUnits_answers404() throws Throwable {
        asAUserOutsideTheCaseUnits(
                () -> assertEquals("a slide of a case the user may not read is not found", HttpStatus.NOT_FOUND.value(),
                        postAsTechnician(VERIFY_ENDPOINT, body(blockBarcode, slideBarcode)).getStatus()));
        assertNull("and stays unverified", verifiedAt());
    }

    @Test
    public void verify_withABlankField_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(VERIFY_ENDPOINT, body(" ", slideBarcode));

        assertEquals("both fields are needed", HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("and the answer carries the message the screen translates", "pathology.error.scanBothFields",
                bodyOf(response).get("messageKey"));
        assertNull(verifiedAt());
    }

    /**
     * Two checks of one slide race on its version check; the one that loses is a
     * conflict the screen can explain, not a server error.
     */
    @Test
    public void verify_thatLosesTheVersionCheck_answers409() throws Exception {
        Object controller = AopTestUtils.getUltimateTargetObject(securityContext.getBean(PathologyController.class));
        PathologySampleService real = (PathologySampleService) ReflectionTestUtils.getField(controller,
                "pathologySampleService");
        PathologySampleService losing = Mockito.mock(PathologySampleService.class, AdditionalAnswers.delegatesTo(real));
        doThrow(new OptimisticLockException("version")).when(losing).verifySlideLabel(any(), any(), any(), any());
        ReflectionTestUtils.setField(controller, "pathologySampleService", losing);
        try {
            MockHttpServletResponse response = postAsTechnician(VERIFY_ENDPOINT, body(blockBarcode, slideBarcode));

            assertEquals("the check that lost the race is a conflict", HttpStatus.CONFLICT.value(),
                    response.getStatus());
            assertEquals("and the answer says what kind", "version", bodyOf(response).get("conflict"));
        } finally {
            ReflectionTestUtils.setField(controller, "pathologySampleService", real);
        }
        assertNull("the stored row is untouched", verifiedAt());
    }

    // helpers

    private static String body(String block, String slide) {
        return "{\"block\":\"" + block + "\",\"slide\":\"" + slide + "\"}";
    }

    private Timestamp verifiedAt() {
        return PathologyCaseRows.labelVerifiedAt(jdbcTemplate, LEGACY_SLIDE_ID);
    }

    /** The audit entries on the fixture's slide alone. */
    private int auditEntriesOnThisSlide() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM clinlims.history WHERE reference_id = ?"
                + " AND reference_table = (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_SLIDE')",
                Integer.class, LEGACY_SLIDE_ID);
    }
}
