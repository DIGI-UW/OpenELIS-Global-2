package org.openelisglobal.testmethod.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController;
import org.openelisglobal.testmethod.controller.rest.TestMethodRestController.InlineCreateRequest;
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
 * real DB. Locks the link-API fixes that were previously 500s or unguarded:
 * <ul>
 * <li>PATCH/DELETE on an unknown link id, and on a link id belonging to a
 * different test, return 404.</li>
 * <li>A duplicate active (test, method) link returns 409.</li>
 * <li>Setting a new default clears the previous one (single-default
 * invariant).</li>
 * <li>The list returns the linked methods.</li>
 * </ul>
 * Methods 8001–8005 are seeded so each link satisfies the test_method -> method
 * foreign key.
 */
public class TestMethodRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95101L;
    private static final long OTHER_TEST_ID = 95102L;
    private static final long[] METHOD_IDS = { 8001L, 8002L, 8003L, 8004L, 8005L };

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
        for (long methodId : METHOD_IDS) {
            insertMethod(methodId, "M" + methodId);
        }
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
    public void listReturnsLinkedMethod() {
        link(TEST_ID, "8005", false);
        List<TestMethodDto> dtos = testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID));
        assertEquals(1, dtos.size());
        // Assert the toDto mapping actually populated the row, not just its count.
        TestMethodDto dto = dtos.get(0);
        assertEquals("8005", dto.methodId);
        assertEquals("C8005", dto.methodCode);
        assertFalse(dto.isDefault);
        assertEquals("2026-01-01", dto.effectiveDate);
    }

    @org.junit.Test
    public void linkNonNumericMethodId_returns422() {
        // methodId maps to a numeric(10) column; a non-numeric value must be
        // rejected up front (422), not blow up as a 500 at flush.
        ResponseEntity<?> resp = controller.linkMethod(String.valueOf(TEST_ID), linkReq("abc", false), authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void inlineCreateDuplicateCode_returns409() {
        ResponseEntity<?> first = controller.inlineCreateAndLink(String.valueOf(TEST_ID),
                inlineReq("First Method", "SAMECODE"), authedRequest());
        assertEquals(201, first.getStatusCode().value());
        // Distinct names but the SAME code: the name check passes, so this genuinely
        // exercises the duplicate-code path (uq_method_code) -> 409, not the name dup.
        ResponseEntity<?> dup = controller.inlineCreateAndLink(String.valueOf(OTHER_TEST_ID),
                inlineReq("Second Method", "SAMECODE"), authedRequest());
        assertEquals(409, dup.getStatusCode().value());
    }

    @org.junit.Test
    public void updateLinkViaPatch_movesDefaultAndPersistsDate() {
        link(TEST_ID, "8003", true);
        String linkId = link(TEST_ID, "8004", false);
        UpdateLinkRequest req = new UpdateLinkRequest();
        req.isDefault = true;
        req.effectiveDate = "2027-02-02";
        ResponseEntity<?> resp = controller.updateLink(String.valueOf(TEST_ID), linkId, req, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        TestMethodDto updated = (TestMethodDto) resp.getBody();
        assertTrue(updated.isDefault);
        assertEquals("2027-02-02", updated.effectiveDate);
        // The previous default (8003) is cleared: exactly one default remains, and it
        // is 8004.
        long defaults = testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID)).stream().filter(d -> d.isDefault)
                .count();
        assertEquals(1, defaults);
        assertEquals("8004", testMethodService.getDefaultMethodId(String.valueOf(TEST_ID)));
    }

    @org.junit.Test
    public void patchInvalidEffectiveDate_returns422() {
        String linkId = link(TEST_ID, "8001", false);
        UpdateLinkRequest req = new UpdateLinkRequest();
        req.effectiveDate = "not-a-date";
        ResponseEntity<?> resp = controller.updateLink(String.valueOf(TEST_ID), linkId, req, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void removeLink_softDeletesAndAllowsRelink() {
        String linkId = link(TEST_ID, "8001", false);
        ResponseEntity<?> del = controller.removeLink(String.valueOf(TEST_ID), linkId, authedRequest());
        assertEquals(204, del.getStatusCode().value());
        assertEquals(0, testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID)).size());
        // The (test, method) pair can be re-linked — the partial-unique active index
        // ignores the soft-deleted row.
        link(TEST_ID, "8001", false);
        assertEquals(1, testMethodService.getLinkedMethodDtos(String.valueOf(TEST_ID)).size());
    }

    @org.junit.Test
    public void copyMethodsFromTest_copiesMissingAsNonDefaultSkippingExisting() {
        link(TEST_ID, "8001", true); // source: 8001 is the default
        link(TEST_ID, "8002", false); // source: 8002
        link(OTHER_TEST_ID, "8001", false); // target already has 8001
        ResponseEntity<?> resp = controller.copyFromTest(String.valueOf(OTHER_TEST_ID), String.valueOf(TEST_ID),
                authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        // Target now has both 8001 (pre-existing, not duplicated) and 8002 (copied)...
        assertEquals(2, testMethodService.getLinkedMethodDtos(String.valueOf(OTHER_TEST_ID)).size());
        // ...and copies are forced non-default — the source's default flag is not
        // carried.
        assertNull(testMethodService.getDefaultMethodId(String.valueOf(OTHER_TEST_ID)));
    }

    @org.junit.Test
    public void getMethodDisplayList_emptyReturnsNull_thenListsLinked() {
        assertNull(testMethodService.getMethodDisplayListForTest(String.valueOf(TEST_ID)));
        link(TEST_ID, "8001", false);
        List<IdValuePair> display = testMethodService.getMethodDisplayListForTest(String.valueOf(TEST_ID));
        assertEquals(1, display.size());
        assertEquals("8001", display.get(0).getId());
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

    private static InlineCreateRequest inlineReq(String name, String code) {
        InlineCreateRequest req = new InlineCreateRequest();
        req.nameEnglish = name;
        req.nameFrench = name;
        req.code = code;
        req.isDefault = false;
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

    private void insertMethod(long id, String name) {
        // Seed a code too so DTO-content assertions can verify methodCode.
        jdbc.update("INSERT INTO clinlims.method (id, name, description, code, is_active, lastupdated)"
                + " VALUES (?, ?, ?, ?, 'Y', NOW())", id, name, name + " desc", "C" + id);
    }

    private void cleanup() {
        try {
            // test_method first — it now has FKs to both test and method.
            jdbc.update("DELETE FROM clinlims.test_method WHERE test_id IN (?, ?)", TEST_ID, OTHER_TEST_ID);
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", TEST_ID, OTHER_TEST_ID);
            jdbc.update("DELETE FROM clinlims.method WHERE id IN (8001, 8002, 8003, 8004, 8005)");
            jdbc.update("DELETE FROM clinlims.method WHERE code = 'SAMECODE'");
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
