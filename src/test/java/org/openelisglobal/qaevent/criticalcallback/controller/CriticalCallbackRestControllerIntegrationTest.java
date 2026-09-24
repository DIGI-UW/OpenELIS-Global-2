package org.openelisglobal.qaevent.criticalcallback.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.qaevent.criticalcallback.CriticalResultFixture;
import org.openelisglobal.qaevent.criticalcallback.controller.rest.CriticalCallbackRestController;
import org.openelisglobal.qaevent.criticalcallback.controller.rest.CriticalCallbackRestController.CallbackRequest;
import org.openelisglobal.qaevent.criticalcallback.service.CriticalCallbackService;
import org.openelisglobal.qaevent.criticalcallback.valueholder.CriticalCallback;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-714 — critical-callback capture endpoint, round-tripped against a real
 * DB. Covers create (201 + server-stamped loggedBy/loggedAt + derived
 * analysisId + resultValue snapshot), the repeat-POST-is-a-new-attempt-row
 * contract, and the 400 validation guards (unknown/blank result, NON-CRITICAL
 * saved value, blank/oversized recipient, bad status).
 *
 * <p>
 * The seed builds the full chain the criticality check resolves through: test +
 * result_limits (critical band 10–90, default demographic row) + sample +
 * sample_item + analysis + a saved result. A callback can only be logged
 * against a persisted, actually-critical result.
 *
 * <p>
 * Gated by {@code qa.view.qi}; the 403 path is enforced by Spring Security's
 * proxy, which is bypassed under direct controller invocation, so it is not
 * asserted here (matches the sibling editor ITs) — the Playwright E2E covers
 * the gate.
 */
public class CriticalCallbackRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    // The test, its critical band, the sample and the sample item all share this
    // id; each analysis carries a result of the same id as itself.
    private static final long TEST_ID = 95431L;
    private static final long ANALYSIS_ID = 95431L;
    private static final long ANALYSIS_ID_NORMAL = 95432L;

    @Autowired
    private CriticalCallbackService callbackService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private CriticalCallbackRestController controller;
    private CriticalResultFixture fixture;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        controller = new CriticalCallbackRestController(callbackService, resultService, resultLimitService);
        fixture = new CriticalResultFixture(new JdbcTemplate(dataSource), TEST_ID, ANALYSIS_ID, ANALYSIS_ID_NORMAL);
        fixture.clean();
        fixture.seedTestWithCriticalBand("CallbackIT", "CBIT");
        // 95 is at/beyond the high critical bound (>= 90); 50 is inside normal.
        // Neither is released: the capture endpoint reads the saved result, not the
        // release window.
        fixture.seedResult(ANALYSIS_ID, null, "95");
        fixture.seedResult(ANALYSIS_ID_NORMAL, null, "50");
    }

    @After
    public void tearDown() {
        fixture.clean();
    }

    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    private static CallbackRequest req(String resultId, String recipientName, String status) {
        CallbackRequest body = new CallbackRequest();
        body.resultId = resultId;
        body.recipientName = recipientName;
        body.status = status;
        return body;
    }

    /** Each analysis carries a result of the same id. */
    private String criticalResultId() {
        return String.valueOf(ANALYSIS_ID);
    }

    /** Every attempt logged against one analysis, newest first. */
    private List<CriticalCallback> attemptsFor(long analysisId) {
        return callbackService.getAllMatchingOrdered("analysisId", String.valueOf(analysisId), "loggedAt", true);
    }

    @Test
    public void create_persistsRow_withServerStampedIdentityTimeAndSnapshot() {
        Timestamp before = Timestamp.from(Instant.now().minusSeconds(5));

        CriticalCallback created = controller
                .create(req(criticalResultId(), "  Dr. Achieng (Ward 4)  ", "CONFIRMED"), authedRequest()).getBody();

        assertNotNull(created);
        assertNotNull(created.getId());
        // resultId echoed; analysisId + communicated value derived server-side
        assertEquals(criticalResultId(), created.getResultId());
        assertEquals(String.valueOf(ANALYSIS_ID), created.getAnalysisId());
        assertEquals("95", created.getResultValue());
        // recipient is trimmed; identity and time are stamped server-side
        assertEquals("Dr. Achieng (Ward 4)", created.getRecipientName());
        assertEquals("CONFIRMED", created.getStatus());
        assertEquals("1", created.getLoggedBy());
        assertNotNull(created.getLoggedAt());
        assertTrue("loggedAt should be stamped at insert time", created.getLoggedAt().after(before));

        List<CriticalCallback> persisted = attemptsFor(ANALYSIS_ID);
        assertEquals(1, persisted.size());
        assertEquals(created.getId(), persisted.get(0).getId());
        assertEquals("95", persisted.get(0).getResultValue());
        assertEquals("Dr. Achieng (Ward 4)", persisted.get(0).getRecipientName());
        assertEquals("CONFIRMED", persisted.get(0).getStatus());
        assertEquals("1", persisted.get(0).getLoggedBy());
    }

    @Test
    public void repeatPost_sameResult_isANewAttemptRow() {
        controller.create(req(criticalResultId(), "Ward clerk", "UNABLE_TO_REACH"), authedRequest());
        controller.create(req(criticalResultId(), "Dr. Okello", "CONFIRMED"), authedRequest());

        List<CriticalCallback> attempts = attemptsFor(ANALYSIS_ID);
        assertEquals(2, attempts.size());
        // newest first (order by loggedAt desc); both outcomes retained
        assertEquals("CONFIRMED", attempts.get(0).getStatus());
        assertEquals("Dr. Okello", attempts.get(0).getRecipientName());
        assertEquals("UNABLE_TO_REACH", attempts.get(1).getStatus());
        assertEquals("Ward clerk", attempts.get(1).getRecipientName());
    }

    @Test
    public void create_nonCriticalSavedValue_throwsBadRequest() {
        // value 50 sits inside the 10–90 critical band: the record does not
        // support a critical callback, regardless of what the UI showed.
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> controller
                .create(req(String.valueOf(ANALYSIS_ID_NORMAL), "Dr. X", "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(attemptsFor(ANALYSIS_ID_NORMAL).isEmpty());
    }

    @Test
    public void create_unknownResult_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req("99999999", "Dr. X", "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_missingResultId_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req(" ", "Dr. X", "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_blankRecipient_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req(criticalResultId(), "   ", "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_oversizedRecipient_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req(criticalResultId(), "x".repeat(256), "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_invalidStatus_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req(criticalResultId(), "Dr. X", "LEFT_VOICEMAIL"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        // nothing persisted on a rejected request
        assertTrue(attemptsFor(ANALYSIS_ID).isEmpty());
    }
}
