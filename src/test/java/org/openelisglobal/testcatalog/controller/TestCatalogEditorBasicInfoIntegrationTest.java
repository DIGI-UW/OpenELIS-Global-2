package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-748 Basic Info — round-trip against a real DB: load a test's basic-info,
 * change Domain + AMR + status, save, reload, and confirm the M1 columns
 * (TEST.DOMAIN, TEST.ANTIMICROBIAL_RESISTANCE) and status persisted.
 *
 * <p>
 * Also pins the /tests list endpoint's filters (domain, status, amr, search),
 * case-insensitive name sort, and pagination — the behavior contract that a
 * later move to a DB-side projection query must preserve.
 */
public class TestCatalogEditorBasicInfoIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95001L;

    @Autowired
    private TestService testService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        // Controllers live in the servlet context, not the test's root context; build
        // one with the real (autowired) service so the save logic hits a real DB.
        controller = new TestCatalogEditorRestController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "testService", testService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, antimicrobial_resistance,"
                        + " orderable, lastupdated) VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', false, true, NOW())",
                TEST_ID, "BasicInfoIT", "BasicInfoIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    /**
     * A request carrying the audit user (id 1, seeded by the test base) for
     * saveBasicInfo.
     */
    private static MockHttpServletRequest authedRequest() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    @org.junit.Test
    public void basicInfo_roundTrips_domainAmrAndStatus() {
        ResponseEntity<BasicInfo> loaded = controller.getBasicInfo(String.valueOf(TEST_ID));
        assertEquals(200, loaded.getStatusCode().value());
        assertEquals("CLINICAL", loaded.getBody().domain);
        assertTrue(!loaded.getBody().antimicrobialResistance);

        BasicInfo update = new BasicInfo();
        update.domain = "VECTOR";
        update.antimicrobialResistance = true;
        update.active = true;
        update.orderable = false;
        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), update, authedRequest());
        assertEquals(200, saved.getStatusCode().value());
        assertEquals("VECTOR", saved.getBody().domain);
        assertTrue(saved.getBody().antimicrobialResistance);

        // Reload from DB through the service to confirm persistence.
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("VECTOR", reloaded.getDomain());
        assertTrue(Boolean.TRUE.equals(reloaded.getAntimicrobialResistance()));
        assertTrue(!Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_rejectsInvalidDomain() {
        BasicInfo bad = new BasicInfo();
        bad.domain = "NONSENSE";
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_rejectsImmutableFieldChange() {
        // Name/code/description are read-only in v1 (OGC-950); submitting a
        // changed value for one must be rejected (422), not silently ignored.
        BasicInfo bad = new BasicInfo();
        bad.name = "Renamed-" + TEST_ID;
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_unknownTestReturns404() {
        ResponseEntity<BasicInfo> resp = controller.getBasicInfo("99999999");
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void getEditorEnvelope_returns200WithApplicableSections() {
        ResponseEntity<TestCatalogEditorRestController.EditorEnvelope> resp = controller
                .getEditorEnvelope(String.valueOf(TEST_ID));
        assertEquals(200, resp.getStatusCode().value());
        TestCatalogEditorRestController.EditorEnvelope env = resp.getBody();
        assertEquals(String.valueOf(TEST_ID), env.testId);
        assertEquals("CLINICAL", env.domain);
        // The full v1 section set, in order, is the whole point of the envelope (M2).
        assertEquals(java.util.List.of("basic-info", "sample-results", "methods", "ranges", "storage", "panels",
                "terminology", "analyzers", "display-order"), env.applicableSections);
    }

    @org.junit.Test
    public void basicInfo_partialPut_preservesUnsentFlags() {
        // Arrange: AMR + active + orderable all true.
        BasicInfo setup = new BasicInfo();
        setup.antimicrobialResistance = true;
        setup.active = true;
        setup.orderable = true;
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), setup, authedRequest()).getStatusCode().value());

        // Act: PUT only the domain — every flag field is null.
        BasicInfo partial = new BasicInfo();
        partial.domain = "VECTOR";
        assertEquals(200,
                controller.saveBasicInfo(String.valueOf(TEST_ID), partial, authedRequest()).getStatusCode().value());

        // Assert: the unsent flags are preserved, NOT silently reset to false
        // (the boxed-Boolean apply-if-present contract).
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("VECTOR", reloaded.getDomain());
        assertTrue(Boolean.TRUE.equals(reloaded.getAntimicrobialResistance()));
        assertTrue(reloaded.isActive());
        assertTrue(Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_rejectsImmutableCodeChange() {
        BasicInfo bad = new BasicInfo();
        bad.code = "NEWCODE" + TEST_ID;
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_rejectsImmutableDescriptionChange() {
        BasicInfo bad = new BasicInfo();
        bad.description = "Changed description";
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void listTests_filtersByDomainAndPaginates() {
        // Seed three catalog rows: 2 CLINICAL, 1 VECTOR (ids in a cleaned range).
        for (long id = 95010L; id <= 95012L; id++) {
            String dom = id == 95012L ? "VECTOR" : "CLINICAL";
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                            + " antimicrobial_resistance, orderable, lastupdated)"
                            + " VALUES (?, ?, ?, 'Y', ?, ?, false, true, NOW())",
                    id, "ListIT-" + id, "ListIT-" + id, UUID.randomUUID().toString(), dom);
        }

        TestCatalogEditorRestController.TestListPage vector = controller.listTests("VECTOR", "all", null, "ListIT-", 1,
                25);
        assertEquals(1, vector.total);
        assertEquals("VECTOR", vector.rows.get(0).domain);

        TestCatalogEditorRestController.TestListPage all = controller.listTests(null, "all", null, "ListIT-", 1, 2);
        assertEquals(3, all.total);
        assertEquals(2, all.rows.size()); // page size 2 of 3 total

        TestCatalogEditorRestController.TestListPage page2 = controller.listTests(null, "all", null, "ListIT-", 2, 2);
        assertEquals(1, page2.rows.size());
    }

    @org.junit.Test
    public void listTests_filtersByStatus() {
        seedTest(95020L, "StatusIT-active1", "CLINICAL", true, false);
        seedTest(95021L, "StatusIT-active2", "CLINICAL", true, false);
        seedTest(95022L, "StatusIT-inactive", "CLINICAL", false, false);

        assertEquals(3, controller.listTests(null, "all", null, "StatusIT-", 1, 25).total);
        assertEquals(2, controller.listTests(null, "active", null, "StatusIT-", 1, 25).total);
        assertEquals(1, controller.listTests(null, "inactive", null, "StatusIT-", 1, 25).total);
    }

    @org.junit.Test
    public void listTests_filtersByAmr() {
        seedTest(95023L, "AmrIT-yes", "CLINICAL", true, true);
        seedTest(95024L, "AmrIT-no1", "CLINICAL", true, false);
        seedTest(95025L, "AmrIT-no2", "CLINICAL", true, false);

        assertEquals(3, controller.listTests(null, "all", null, "AmrIT-", 1, 25).total);
        assertEquals(1, controller.listTests(null, "all", true, "AmrIT-", 1, 25).total);
        assertEquals(2, controller.listTests(null, "all", false, "AmrIT-", 1, 25).total);
    }

    @org.junit.Test
    public void listTests_searchIsCaseInsensitiveSubstring() {
        seedTest(95026L, "ZebraSrchIT", "CLINICAL", true, false);
        seedTest(95027L, "alphaSrchIT", "CLINICAL", true, false);

        // a lowercase query matches both mixed-case names (case-insensitive)
        assertEquals(2, controller.listTests(null, "all", null, "srchit", 1, 25).total);
        // a distinct fragment narrows to one
        assertEquals(1, controller.listTests(null, "all", null, "ZEBRA", 1, 25).total);
    }

    @org.junit.Test
    public void listTests_sortsByNameCaseInsensitive() {
        seedTest(95028L, "banana SortIT", "CLINICAL", true, false);
        seedTest(95029L, "Apple SortIT", "CLINICAL", true, false);
        seedTest(95030L, "cherry SortIT", "CLINICAL", true, false);

        java.util.List<TestCatalogEditorRestController.TestListRow> rows = controller.listTests(null, "all", null,
                "SortIT", 1, 25).rows;
        assertEquals(3, rows.size());
        assertEquals("Apple SortIT", rows.get(0).name);
        assertEquals("banana SortIT", rows.get(1).name);
        assertEquals("cherry SortIT", rows.get(2).name);
    }

    private void seedTest(long id, String name, String domain, boolean active, boolean amr) {
        jdbc.update("INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                + " antimicrobial_resistance, orderable, lastupdated)" + " VALUES (?, ?, ?, ?, ?, ?, ?, true, NOW())",
                id, name, name, active ? "Y" : "N", UUID.randomUUID().toString(), domain, amr);
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test WHERE id = ? OR (id >= 95010 AND id <= 95099)", TEST_ID);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
