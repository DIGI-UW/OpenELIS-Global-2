package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import jakarta.persistence.OptimisticLockException;
import java.util.Map;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.openelisglobal.program.controller.pathology.PathologyRequestRestController;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The endpoints that raise a bench request on a case and complete, cancel or
 * reopen one (FR-10). That anonymous calls are refused, and that the user's lab
 * units are checked before anything is written, is pinned by the security slice
 * test beside the controller; the rules themselves by the service tests.
 *
 * <p>
 * A change answers with the request's id and status only, since the screen
 * reads the case again; a refusal carries the message id the screen translates;
 * a case the user may not read is a plain not-found; a page that showed a
 * status the request has since left is a request conflict, and a change that
 * lost the case's version check a version conflict.
 */
public class PathologyRequestRestControllerTest extends PathologyControllerTestSupport {

    private static final String RAISE = "/rest/pathology/caseView/" + CASE_ID + "/requests";

    private static final String BLOCK_REQUEST = "{\"kind\":\"BLOCK\",\"targetBlockId\":null,\"targetSlideId\":null,"
            + "\"stainId\":null,\"priority\":\"STAT\",\"instruction\":\"Another block from part A\"}";

    @Test
    public void aRaise_answers200WithTheNewRequestsIdAndStatus() throws Exception {
        MockHttpServletResponse response = postAsTechnician(RAISE, BLOCK_REQUEST);

        assertEquals("the request is raised", HttpStatus.OK.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the answer names the request and its status, nothing more", 2, answer.size());
        assertEquals("a request just raised is open", "OPENED", answer.get("status"));
        int id = ((Number) answer.get("id")).intValue();
        Map<String, Object> row = requestRow(id);
        assertEquals("the id answered is the row written on this case", CASE_ID,
                ((Number) row.get("pathology_sample_id")).intValue());
        assertEquals("BLOCK", row.get("request_kind"));
        assertEquals("STAT", row.get("priority"));
        assertEquals("raised by the signed-in user", 1001, ((Number) row.get("raised_by")).intValue());
    }

    /** A slide request that names no block is refused, and nothing is written. */
    @Test
    public void aRaiseRefusal_answers400WithTheMessageId() throws Exception {
        MockHttpServletResponse response = postAsTechnician(RAISE,
                "{\"kind\":\"SLIDE\",\"priority\":\"NORMAL\",\"instruction\":\"Deeper levels\"}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the screen translates the refusal from its message id", "pathology.error.requestTargetRequired",
                answer.get("messageKey"));
        assertTrue("with its values, empty or not", answer.get("messageArgs") instanceof Map);
        assertEquals("the case still holds only the fixture's request", 1, requestCountOnTheCase());
    }

    /** A post with no body is a request that names no kind. */
    @Test
    public void aRaiseWithNoBody_answers400KindRequired() throws Exception {
        MockHttpServletResponse response = postAsTechnician(RAISE, null);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("pathology.error.requestKindRequired", bodyOf(response).get("messageKey"));
        assertEquals(1, requestCountOnTheCase());
    }

    @Test
    public void aRaiseOnAnUnknownCase_answers404() throws Exception {
        MockHttpServletResponse response = postAsTechnician("/rest/pathology/caseView/424242/requests", BLOCK_REQUEST);

        assertEquals("a case id nothing carries is not found", HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    /** A case outside the user's lab units is not found, and nothing is written. */
    @Test
    public void aCaseOutsideTheUsersUnits_answers404AndIsLeftAsItWas() throws Throwable {
        asAUserOutsideTheCaseUnits(() -> {
            assertEquals("a request is not raised on a case the user may not read", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(RAISE, BLOCK_REQUEST).getStatus());
            assertEquals("nor is one of its requests changed", HttpStatus.NOT_FOUND.value(),
                    postAsTechnician(statusOf(FIXTURE_REQUEST_ID), completeFrom("OPENED")).getStatus());
        });
        assertEquals("no request was added", 1, requestCountOnTheCase());
        assertEquals("and the fixture's request is still open", "OPENED", storedStatus(FIXTURE_REQUEST_ID));
    }

    @Test
    public void aStatusChange_answers200WithTheIdAndTheNewStatus() throws Exception {
        MockHttpServletResponse response = postAsTechnician(statusOf(FIXTURE_REQUEST_ID), completeFrom("OPENED"));

        assertEquals("the request is completed", HttpStatus.OK.value(), response.getStatus());
        assertEquals("the answer names the request and its status, nothing more",
                Map.of("id", FIXTURE_REQUEST_ID, "status", "COMPLETED"), bodyOf(response));
        assertEquals("as stored", "COMPLETED", storedStatus(FIXTURE_REQUEST_ID));
    }

    /**
     * A page still showing a status the request has left is told so, and nothing is
     * written: another user changed the request first.
     */
    @Test
    public void aStatusChangeFromAStatusTheRequestHasLeft_answers409Request() throws Exception {
        MockHttpServletResponse response = postAsTechnician(statusOf(FIXTURE_REQUEST_ID),
                "{\"from\":\"CANCELLED\",\"status\":\"OPENED\"}");

        assertEquals("a stale page is a conflict, not a second change", HttpStatus.CONFLICT.value(),
                response.getStatus());
        Map<String, Object> answer = bodyOf(response);
        assertEquals("the answer says what kind", "request", answer.get("conflict"));
        assertEquals("and where the request really stands", "OPENED", answer.get("status"));
        assertEquals("which is unchanged", "OPENED", storedStatus(FIXTURE_REQUEST_ID));
        assertEquals("and nothing was audited", 0, requestAuditCount());
    }

    /** A request of another case is not this case's to change. */
    @Test
    public void aRequestOfAnotherCase_answers400NotOnCase() throws Exception {
        MockHttpServletResponse response = postAsTechnician(statusOf(OTHER_CASES_REQUEST_ID),
                "{\"from\":\"COMPLETED\",\"status\":\"OPENED\"}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("pathology.error.requestNotOnCase", bodyOf(response).get("messageKey"));
        assertEquals("the other case's request is left as it was", "COMPLETED", storedStatus(OTHER_CASES_REQUEST_ID));
    }

    /** A cancellation without a reason is refused by name. */
    @Test
    public void aCancellationWithoutAReason_answers400() throws Exception {
        MockHttpServletResponse response = postAsTechnician(statusOf(FIXTURE_REQUEST_ID),
                "{\"from\":\"OPENED\",\"status\":\"CANCELLED\",\"cancelReason\":\"  \"}");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertEquals("pathology.error.requestCancelReasonRequired", bodyOf(response).get("messageKey"));
        assertEquals("OPENED", storedStatus(FIXTURE_REQUEST_ID));
    }

    /**
     * A raise that loses the case's version check to a move or a save landing in
     * the same moment is a conflict the screen can explain, not a server error.
     */
    @Test
    public void aRaiseThatLosesTheVersionCheck_answers409Version() throws Exception {
        Object controller = AopTestUtils
                .getUltimateTargetObject(securityContext.getBean(PathologyRequestRestController.class));
        PathologySampleService real = (PathologySampleService) ReflectionTestUtils.getField(controller,
                "pathologySampleService");
        PathologySampleService losing = Mockito.mock(PathologySampleService.class, AdditionalAnswers.delegatesTo(real));
        doThrow(new OptimisticLockException("version")).when(losing).raiseRequest(any(), any(), any());
        ReflectionTestUtils.setField(controller, "pathologySampleService", losing);
        try {
            MockHttpServletResponse response = postAsTechnician(RAISE, BLOCK_REQUEST);

            assertEquals("the raise that lost the race is a conflict", HttpStatus.CONFLICT.value(),
                    response.getStatus());
            assertEquals("of the version kind", "version", bodyOf(response).get("conflict"));
        } finally {
            ReflectionTestUtils.setField(controller, "pathologySampleService", real);
        }
        assertEquals("nothing was raised", 1, requestCountOnTheCase());
    }

    // helpers

    private static String statusOf(int requestId) {
        return "/rest/pathology/caseView/" + CASE_ID + "/requests/" + requestId + "/status";
    }

    private static String completeFrom(String from) {
        return "{\"from\":\"" + from + "\",\"status\":\"COMPLETED\",\"cancelReason\":null,"
                + "\"resultBlockId\":null,\"resultSlideId\":null}";
    }

    private Map<String, Object> requestRow(int id) {
        return jdbcTemplate.queryForMap("SELECT * FROM clinlims.pathology_request WHERE id = ?", id);
    }

    private String storedStatus(int id) {
        return jdbcTemplate.queryForObject("SELECT status FROM clinlims.pathology_request WHERE id = ?", String.class,
                id);
    }

    private int requestCountOnTheCase() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.pathology_request WHERE pathology_sample_id = ?", Integer.class,
                CASE_ID);
    }

    private int requestAuditCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.history WHERE reference_table ="
                        + " (SELECT id FROM clinlims.reference_tables WHERE name = 'PATHOLOGY_REQUEST')",
                Integer.class);
    }
}
