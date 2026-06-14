package org.openelisglobal.testmethod.controller;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.LinkMethodRequest;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.UpdateLinkRequest;
import org.openelisglobal.testmethod.service.TestMethodService;
import org.openelisglobal.testmethod.service.TestMethodService.TestMethodDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-949 review follow-ups (#3714) — TestMethod link API behavior against a
 * real DB. Locks the B1–B4 fixes that were previously 500s or unguarded:
 * <ul>
 * <li>B1 — PATCH/DELETE on an unknown link id, and a cross-test link id, return
 * 404.</li>
 * <li>B2 — a link whose method_id has no matching method row does not 500 the
 * list.</li>
 * <li>duplicate active link returns 409; the single-default invariant
 * holds.</li>
 * </ul>
 * method_ids are intentionally arbitrary (no method rows) so the list path also
 * exercises the orphaned-method_id case (B2).
 */
public class TestMethodRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95101L;
    private static final long OTHER_TEST_ID = 95102L;

    @Autowired
    private TestMethodService testMethodService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestMethodRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestMethodRestController();
        ReflectionTestUtils.setField(controller, "testMethodService", testMethodService);
        cleanup();
        insertTest(TEST_ID, "TMLinkIT");
        insertTest(OTHER_TEST_ID, "TMLinkIT-other");
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @org.junit.Test
    public void patchUnknownLinkId_returns404() {
        UpdateLinkRequest req = new UpdateLinkRequest();
        req.isDefault = true;
        ResponseEntity<?> resp = controller.updateLink(String.valueOf(TEST_ID), "no-such-link", req, authedRequest());
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void deleteUnknownLinkId_returns404() {
        ResponseEntity<?> resp = controller.removeLink(String.valueOf(TEST_ID), "no-such-link", authedRequest());
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void crossTestLinkId_returns404() {
        String linkId = link(TEST_ID, "8002", false);
        UpdateLinkRequest req = new UpdateLinkRequest();
        req.isDefault = true;
        // The link belongs to TEST_ID; patching it via OTHER_TEST_ID's path must 404.
        ResponseEntity<?> resp = controller.updateLink(String.valueOf(OTHER_TEST_ID), linkId, req, authedRequest());
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void duplicateActiveLink_returns409() {
        link(TEST_ID, "8001", false);
        ResponseEntity<?> dup = controller.linkMethod(String.valueOf(TEST_ID), linkReq("8001", false), authedRequest());
        assertEquals(409, dup.getStatusCode().value());
    }

    @org.junit.Test
    public void settingDefault_clearsPreviousDefault() {
        link(TEST_ID, "8003", true);
        link(TEST_ID, "8004", true);
        long defaults = testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID)).stream().filter(d -> d.isDefault)
                .count();
        assertEquals(1, defaults);
        assertEquals("8004", testMethodService.getDefaultMethodId(String.valueOf(TEST_ID)));
    }

    @org.junit.Test
    public void listWithOrphanedMethodId_doesNotError() {
        link(TEST_ID, "8005", false);
        // method 8005 has no method row; the list must still return the link (B2).
        assertEquals(1, testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID)).size());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String link(long testId, String methodId, boolean isDefault) {
        ResponseEntity<?> resp = controller.linkMethod(String.valueOf(testId), linkReq(methodId, isDefault),
                authedRequest());
        assertEquals(201, resp.getStatusCode().value());
        return ((TestMethodDto) resp.getBody()).id;
    }

    private static LinkMethodRequest linkReq(String methodId, boolean isDefault) {
        LinkMethodRequest req = new LinkMethodRequest();
        req.methodId = methodId;
        req.isDefault = isDefault;
        req.effectiveDate = "2026-01-01";
        return req;
    }

    private void insertTest(long id, String name) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                        + " antimicrobial_resistance, orderable, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', false, true, NOW())",
                id, name, name + " desc", UUID.randomUUID().toString());
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test_method WHERE test_id IN (?, ?)", String.valueOf(TEST_ID),
                    String.valueOf(OTHER_TEST_ID));
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_ID, OTHER_TEST_ID);
        } catch (Exception ignored) {
            // best-effort
        }
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
}
