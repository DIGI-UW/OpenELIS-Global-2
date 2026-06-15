package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.MembershipItem;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.PanelMembership;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.PanelMembershipUpdate;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.PanelOption;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.TestPanelsResponse;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M9 / OGC-980..982 — Panels section API, round-tripped against a real
 * DB. Verifies a test's panel memberships read, the add/reposition/remove
 * reconcile (persisted to panel_item), the panel list + preview, and the 404
 * guards. Uses existing (Liquibase-seeded) panels — seeding a new panel needs
 * the full orderable-panel scaffolding (localization + modules + roles).
 */
public class TestCatalogEditorPanelsIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95451L;

    @Autowired
    private TestService testService;
    @Autowired
    private TestResultComponentService componentService;
    @Autowired
    private TestResultInterpretationService interpretationService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;
    @Autowired
    private org.openelisglobal.testcatalog.service.RangeCoverageValidationService coverageService;
    @Autowired
    private org.openelisglobal.testsamplehandling.service.TestSampleHandlingService handlingService;
    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerService analyzerService;
    @Autowired
    private org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService analyzerTestMappingService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleService typeOfSampleService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private org.openelisglobal.testterminology.service.TestTerminologyMappingService terminologyService;
    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;
    @Autowired
    private org.openelisglobal.panelitem.service.PanelItemService panelItemService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;
    private String panelAId;
    private String panelBId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogEditorRestController(testService, componentService, interpretationService,
                testResultService, resultLimitService, coverageService, handlingService, analyzerService,
                analyzerTestMappingService, typeOfSampleService, typeOfSampleTestService, terminologyService,
                panelService, panelItemService);
        List<Panel> panels = panelService.getAllActivePanels();
        Assume.assumeTrue("needs at least two Liquibase-seeded panels", panels.size() >= 2);
        panelAId = panels.get(0).getId();
        panelBId = panels.get(1).getId();
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "PanelsIT", "PanelsIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.panel_item WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    private String testId() {
        return String.valueOf(TEST_ID);
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

    private TestPanelsResponse put(MembershipItem... items) {
        PanelMembershipUpdate body = new PanelMembershipUpdate();
        for (MembershipItem i : items) {
            body.memberships.add(i);
        }
        return controller.saveTestPanels(testId(), body, authedRequest()).getBody();
    }

    private static MembershipItem membership(String panelId, Integer position) {
        MembershipItem item = new MembershipItem();
        item.panelId = panelId;
        item.position = position;
        return item;
    }

    private PanelMembership find(TestPanelsResponse resp, String panelId) {
        return resp.memberships.stream().filter(m -> panelId.equals(m.panelId)).findFirst().orElse(null);
    }

    private Long membershipRowCount(String panelId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.panel_item WHERE test_id = ? AND panel_id = ?",
                Long.class, TEST_ID, Long.parseLong(panelId));
    }

    @org.junit.Test
    public void getTestPanels_emptyWhenNoMemberships() {
        ResponseEntity<TestPanelsResponse> resp = controller.getTestPanels(testId());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().memberships.isEmpty());
    }

    @org.junit.Test
    public void saveAndGet_addsTestToPanelsWithPositions() {
        put(membership(panelAId, 5), membership(panelBId, 3));
        TestPanelsResponse loaded = controller.getTestPanels(testId()).getBody();
        assertEquals(2, loaded.memberships.size());
        assertEquals(Integer.valueOf(5), find(loaded, panelAId).position);
        assertEquals(Integer.valueOf(3), find(loaded, panelBId).position);
    }

    @org.junit.Test
    public void reposition_andRemove_persists() {
        put(membership(panelAId, 5), membership(panelBId, 3));
        // Keep only panel A, at a new position → B is removed.
        put(membership(panelAId, 9));
        TestPanelsResponse loaded = controller.getTestPanels(testId()).getBody();
        assertEquals(1, loaded.memberships.size());
        assertEquals(Integer.valueOf(9), find(loaded, panelAId).position);
        assertEquals(Long.valueOf(1L), membershipRowCount(panelAId));
        assertEquals(Long.valueOf(0L), membershipRowCount(panelBId));
    }

    @org.junit.Test
    public void listPanels_includesSeededPanels() {
        boolean found = false;
        for (PanelOption o : controller.listPanels()) {
            if (panelAId.equals(o.id)) {
                found = true;
            }
        }
        assertTrue("the seeded panel should appear in the typeahead list", found);
    }

    @org.junit.Test
    public void getPanelTestOrder_includesTheAddedTest() {
        put(membership(panelAId, 7));
        ResponseEntity<TestCatalogEditorRestController.PanelTestOrderResponse> resp = controller
                .getPanelTestOrder(panelAId);
        assertEquals(200, resp.getStatusCode().value());
        boolean found = resp.getBody().tests.stream().anyMatch(r -> testId().equals(r.testId));
        assertTrue("the added test should appear in the panel's test order", found);
    }

    @org.junit.Test
    public void panels_unknownTestOrPanelReturns404() {
        assertEquals(404, controller.getTestPanels("99999999").getStatusCode().value());
        assertEquals(404, controller.getPanelTestOrder("99999999").getStatusCode().value());
    }
}
