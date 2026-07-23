package org.openelisglobal.qaevent.criticalcallback.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
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
 * OGC-714 [QA-C.4] — critical-callback capture endpoint, round-tripped against
 * a real DB. Covers create (201 + server-stamped loggedBy/loggedAt + derived
 * analysisId + resultValue snapshot), the repeat-POST-is-a-new-attempt-row
 * contract, and the 400 validation guards (unknown/blank result, NON-CRITICAL
 * saved value, blank/oversized recipient, bad status).
 *
 * <p>
 * The seed builds the full chain the criticality check resolves through: test +
 * result_limits (critical band 10–90, default demographic row) + sample +
 * sample_item + analysis + a saved result. A callback can only be logged
 * against a persisted, actually-critical result (C.4 outline §5).
 *
 * <p>
 * Gated by {@code qa.view.qi}; the 403 path is enforced by Spring Security's
 * proxy, which is bypassed under direct controller invocation, so it is not
 * asserted here (matches the sibling editor ITs) — the Playwright E2E covers
 * the gate.
 */
public class CriticalCallbackRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95431L;
    private static final long LIMIT_ID = 95431L;
    private static final long SAMPLE_ID = 95431L;
    private static final long SAMPLE_ITEM_ID = 95431L;
    private static final long ANALYSIS_ID = 95431L;
    private static final long ANALYSIS_ID_NORMAL = 95432L;
    private static final long RESULT_ID_CRITICAL = 95431L;
    private static final long RESULT_ID_NORMAL = 95432L;

    @Autowired
    private CriticalCallbackService callbackService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private CriticalCallbackRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new CriticalCallbackRestController(callbackService, resultService, resultLimitService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "CallbackIT", "CallbackIT desc", UUID.randomUUID().toString());
        // Default demographic row (blank gender, full age span) with a 10–90
        // critical band — mirrors the shipped result_limits shape.
        jdbc.update(
                "INSERT INTO clinlims.result_limits (id, test_id, test_result_type_id, min_age, max_age,"
                        + " low_critical, high_critical, lastupdated) VALUES (?, ?, 4, 0, ?, 10, 90, NOW())",
                LIMIT_ID, TEST_ID, Double.POSITIVE_INFINITY);
        jdbc.update("INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                + " lastupdated) VALUES (?, ?, NOW(), NOW(), false, NOW())", SAMPLE_ID, "CBIT" + SAMPLE_ID);
        jdbc.update("INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)"
                + " VALUES (?, ?, 1, 1, NOW())", SAMPLE_ITEM_ID, SAMPLE_ID);
        jdbc.update("INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, lastupdated)"
                + " VALUES (?, 'MANUAL', ?, ?, NOW())", ANALYSIS_ID, TEST_ID, SAMPLE_ITEM_ID);
        jdbc.update("INSERT INTO clinlims.analysis (id, analysis_type, test_id, sampitem_id, lastupdated)"
                + " VALUES (?, 'MANUAL', ?, ?, NOW())", ANALYSIS_ID_NORMAL, TEST_ID, SAMPLE_ITEM_ID);
        // 95 is at/beyond the high critical bound (>= 90); 50 is inside normal.
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (?, ?, '95', 'N', NOW())", RESULT_ID_CRITICAL, ANALYSIS_ID);
        jdbc.update("INSERT INTO clinlims.result (id, analysis_id, value, result_type, lastupdated)"
                + " VALUES (?, ?, '50', 'N', NOW())", RESULT_ID_NORMAL, ANALYSIS_ID_NORMAL);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.critical_callback WHERE analysis_id IN (?, ?)", ANALYSIS_ID,
                ANALYSIS_ID_NORMAL);
        jdbc.update("DELETE FROM clinlims.result WHERE id IN (?, ?)", RESULT_ID_CRITICAL, RESULT_ID_NORMAL);
        jdbc.update("DELETE FROM clinlims.analysis WHERE id IN (?, ?)", ANALYSIS_ID, ANALYSIS_ID_NORMAL);
        jdbc.update("DELETE FROM clinlims.sample_item WHERE id = ?", SAMPLE_ITEM_ID);
        jdbc.update("DELETE FROM clinlims.sample WHERE id = ?", SAMPLE_ID);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE id = ?", LIMIT_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
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

    private String criticalResultId() {
        return String.valueOf(RESULT_ID_CRITICAL);
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

        List<CriticalCallback> persisted = callbackService.getByAnalysisId(String.valueOf(ANALYSIS_ID));
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

        List<CriticalCallback> attempts = callbackService.getByAnalysisId(String.valueOf(ANALYSIS_ID));
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
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req(String.valueOf(RESULT_ID_NORMAL), "Dr. X", "CONFIRMED"), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(callbackService.getByAnalysisId(String.valueOf(ANALYSIS_ID_NORMAL)).isEmpty());
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
        assertTrue(callbackService.getByAnalysisId(String.valueOf(ANALYSIS_ID)).isEmpty());
    }
}
