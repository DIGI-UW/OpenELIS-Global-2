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
        for (PanelOption o : controller.listPanels(false)) {
            if (panelAId.equals(o.id)) {
                found = true;
            }
        }
        assertTrue("the seeded panel should appear in the typeahead list", found);
    }

    /**
     * OGC-224 — the list endpoint carries the management fields the Panels list
     * renders: domain (backfilled CLINICAL by liquibase 074), test count, derived
     * sample types (from member tests — panels store none), and the active flag.
     * The picker contract ({id, name}) stays untouched.
     */
    @org.junit.Test
    public void listPanels_managementFields_domainTestCountAndDerivedSampleTypes() {
        Long sampleTypeId = jdbc.queryForObject("SELECT id FROM clinlims.type_of_sample ORDER BY id LIMIT 1",
                Long.class);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (952224, ?, ?)",
                sampleTypeId, TEST_ID);
        // the test->sample-type map is a lazy singleton cache; the jdbc seed
        // above is invisible to it until cleared
        typeOfSampleService.clearCache();
        try {
            put(membership(panelAId, 1));

            PanelOption row = controller.listPanels(true).stream().filter(o -> panelAId.equals(o.id)).findFirst()
                    .orElse(null);
            assertTrue("the panel must appear in the management list", row != null);
            assertEquals("existing panels backfill to CLINICAL", "CLINICAL", row.domain);
            assertTrue("the member test must be counted", row.testCount >= 1);
            assertTrue("sample types derive from the member tests", !row.sampleTypes.isEmpty());
            assertTrue("the panel is active", row.active);
        } finally {
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id = 952224");
            typeOfSampleService.clearCache();
        }
    }

    /**
     * OGC-224 — the management list includes inactive panels only when asked
     * (includeInactive=true); the default stays active-only so the test editor's
     * add-to-panel typeahead never offers an inactive panel.
     */
    @org.junit.Test
    public void listPanels_includeInactiveGatesInactivePanels() {
        jdbc.update("UPDATE clinlims.panel SET is_active = 'N' WHERE id = ?", Long.parseLong(panelBId));
        try {
            boolean inDefault = controller.listPanels(false).stream().anyMatch(o -> panelBId.equals(o.id));
            boolean inManagement = controller.listPanels(true).stream().anyMatch(o -> panelBId.equals(o.id));
            assertTrue("default list must exclude the inactive panel", !inDefault);
            assertTrue("management list must include the inactive panel", inManagement);
        } finally {
            jdbc.update("UPDATE clinlims.panel SET is_active = 'Y' WHERE id = ?", Long.parseLong(panelBId));
        }
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

    @org.junit.Test
    public void saveTestPanels_unknownPanelReturns422() {
        PanelMembershipUpdate body = new PanelMembershipUpdate();
        body.memberships.add(membership(panelAId, 1));
        body.memberships.add(membership("99999999", 2));
        ResponseEntity<TestPanelsResponse> resp = controller.saveTestPanels(testId(), body, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
        // The whole request is rejected — no partial write for the valid panel.
        assertEquals(Long.valueOf(0L), membershipRowCount(panelAId));
    }

    /**
     * Free-text panel creation (OGC-1112 FR-43) — create-if-not-exists, over HTTP
     * so the real bean (with its field-injected localization service) and Jackson
     * binding are exercised. A new name creates the panel (201); the SAME name
     * again returns the existing panel (200, same id) instead of a duplicate or the
     * previous blank 500 (uncaught LIMSDuplicateRecordException). The created panel
     * is immediately assignable and the membership round-trips.
     */
    @org.junit.Test
    public void createPanel_isCreateIfNotExists_andAssignable() throws Exception {
        org.openelisglobal.login.valueholder.UserSessionData usd = new org.openelisglobal.login.valueholder.UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            org.springframework.test.web.servlet.MvcResult created = mockMvc
                    .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/rest/test-catalog/panels")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"PanelsIT Freetext\"}").session(session))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                    .andReturn();
            String panelId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

            // same name again — the existing panel, not a duplicate, not a 500
            org.springframework.test.web.servlet.MvcResult again = mockMvc
                    .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/rest/test-catalog/panels")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"PanelsIT Freetext\"}").session(session))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andReturn();
            org.junit.Assert.assertEquals(panelId,
                    json.readTree(again.getResponse().getContentAsString()).get("id").asText());
            Integer rows = jdbc.queryForObject("SELECT count(*) FROM clinlims.panel WHERE name = 'PanelsIT Freetext'",
                    Integer.class);
            org.junit.Assert.assertEquals(Integer.valueOf(1), rows);

            // the created panel is assignable and the membership persists
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/tests/" + testId() + "/panels")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"memberships\":[{\"panelId\":\"" + panelId + "\",\"position\":4}]}").session(session))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.memberships[0].panelId").value(panelId));
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .get("/rest/test-catalog/tests/" + testId() + "/panels").session(session))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.memberships[0].panelId").value(panelId));
        } finally {
            jdbc.update("DELETE FROM clinlims.panel_item WHERE test_id = ?", TEST_ID);
            jdbc.update("DELETE FROM clinlims.panel WHERE name = 'PanelsIT Freetext'");
        }
    }

    private static MockHttpSession authedSession() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }

    /**
     * OGC-224 C2 — the panel envelope reads back the management fields; unknown and
     * non-numeric ids are 404s.
     */
    @org.junit.Test
    public void getPanel_envelope_and404Guards() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/test-catalog/panels/" + panelAId).session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id")
                        .value(panelAId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.domain")
                        .value("CLINICAL"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name")
                        .isNotEmpty());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/test-catalog/panels/99999999").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/rest/test-catalog/panels/notanumber").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * OGC-224 C2 — Basic Info rename updates BOTH name sources (PANEL.NAME and the
     * display localization order entry reads), and the response carries the saved
     * row.
     */
    @org.junit.Test
    public void savePanelBasicInfo_renameUpdatesNameAndLocalization() throws Exception {
        String originalName = jdbc.queryForObject("SELECT name FROM clinlims.panel WHERE id = ?", String.class,
                Long.parseLong(panelAId));
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelAId + "/basic-info")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"PanelsIT Renamed\",\"description\":\"renamed by IT\"}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name")
                            .value("PanelsIT Renamed"));
            assertEquals("PanelsIT Renamed", jdbc.queryForObject("SELECT name FROM clinlims.panel WHERE id = ?",
                    String.class, Long.parseLong(panelAId)));
            assertEquals("PanelsIT Renamed",
                    jdbc.queryForObject("SELECT lv.value FROM clinlims.localization_value lv"
                            + " JOIN clinlims.panel p ON p.name_localization_id = lv.localization_id"
                            + " WHERE p.id = ? AND lv.locale = 'en'", String.class, Long.parseLong(panelAId)));
            assertEquals("renamed by IT", jdbc.queryForObject("SELECT description FROM clinlims.panel WHERE id = ?",
                    String.class, Long.parseLong(panelAId)));
        } finally {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelAId + "/basic-info")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + originalName + "\",\"description\":\"" + originalName + "\"}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        }
    }

    /**
     * OGC-224 C2 — the FRS activation rules: a zero-test panel can never be
     * activated (422); once it has a member it can; deactivating is always allowed;
     * a create with active=false starts inactive.
     */
    @org.junit.Test
    public void savePanelBasicInfo_activationRules() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        org.springframework.test.web.servlet.MvcResult created = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/rest/test-catalog/panels")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PanelsIT Act\",\"active\":false}").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        String panelId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        try {
            assertEquals("N", jdbc.queryForObject("SELECT is_active FROM clinlims.panel WHERE id = ?", String.class,
                    Long.parseLong(panelId)));

            // zero tests → activation is rejected
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/basic-info")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"active\":true}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                            .isUnprocessableEntity());

            // give it a member test → activation succeeds
            put(membership(panelId, 1));
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/basic-info")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"active\":true}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.active")
                            .value(true));

            // deactivating is always allowed
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/basic-info")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"active\":false}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.active")
                            .value(false));
        } finally {
            jdbc.update("DELETE FROM clinlims.panel_item WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", Long.parseLong(panelId));
        }
    }

    /**
     * OGC-224 C3 — the panel-side Tests write: ordered membership round-trip (add,
     * reorder, remove), SAMPLETYPE_PANEL kept in sync with the derived sample-type
     * set on every write, and auto-activate honored only for the create flow's
     * first test.
     */
    @org.junit.Test
    public void savePanelTests_roundTrip_syncsSampleTypePanel_andAutoActivates() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        Long sampleTypeId = jdbc.queryForObject("SELECT id FROM clinlims.type_of_sample ORDER BY id LIMIT 1",
                Long.class);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (952225, ?, ?)",
                sampleTypeId, TEST_ID);
        org.springframework.test.web.servlet.MvcResult created = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/rest/test-catalog/panels")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PanelsIT Tests\",\"active\":false}").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        String panelId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        try {
            // create flow: first test + autoActivate → membership, sync, ACTIVE
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/tests")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"tests\":[{\"testId\":\"" + testId() + "\",\"position\":1}],\"autoActivate\":true}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.tests[0].testId").value(testId()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.panel.active").value(true));
            assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                    "SELECT count(*) FROM clinlims.sampletype_panel WHERE panel_id = ?" + " AND sample_type_id = ?",
                    Integer.class, Long.parseLong(panelId), sampleTypeId));

            // removing every test empties membership AND the junction; the
            // active state is untouched (editing never auto-flips)
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/tests")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"tests\":[]}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.panel.testCount").value(0))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.panel.active").value(true));
            assertEquals(Integer.valueOf(0),
                    jdbc.queryForObject("SELECT count(*) FROM clinlims.sampletype_panel WHERE panel_id = ?",
                            Integer.class, Long.parseLong(panelId)));
        } finally {
            jdbc.update("DELETE FROM clinlims.sampletype_panel WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.panel_item WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id = 952225");
            jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", Long.parseLong(panelId));
        }
    }

    /**
     * OGC-224 C3 — the domain guard on BOTH sides of the one model: a test whose
     * domain differs from the panel's is rejected (422) by the panel-side Tests
     * write and by the test-side memberships write.
     */
    @org.junit.Test
    public void membershipWrites_rejectCrossDomainTests() throws Exception {
        jdbc.update("UPDATE clinlims.test SET domain = 'ENVIRONMENTAL' WHERE id = ?", TEST_ID);
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelAId + "/tests")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"tests\":[{\"testId\":\"" + testId() + "\",\"position\":1}]}").session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                            .isUnprocessableEntity());
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/tests/" + testId() + "/panels")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"memberships\":[{\"panelId\":\"" + panelAId + "\",\"position\":1}]}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                            .isUnprocessableEntity());
            assertEquals(Long.valueOf(0L), membershipRowCount(panelAId));
        } finally {
            jdbc.update("UPDATE clinlims.test SET domain = 'CLINICAL' WHERE id = ?", TEST_ID);
        }
    }

    /**
     * OGC-224 C3 — the test-side memberships write (the other view of the one
     * model) also keeps SAMPLETYPE_PANEL in sync, including on removal.
     */
    @org.junit.Test
    public void saveTestPanels_syncsSampleTypePanel() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        Long sampleTypeId = jdbc.queryForObject("SELECT id FROM clinlims.type_of_sample ORDER BY id LIMIT 1",
                Long.class);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (952226, ?, ?)",
                sampleTypeId, TEST_ID);
        org.springframework.test.web.servlet.MvcResult created = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/rest/test-catalog/panels")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PanelsIT SyncTS\",\"active\":false}").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        String panelId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/tests/" + testId() + "/panels")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"memberships\":[{\"panelId\":\"" + panelId + "\",\"position\":1}]}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            assertEquals(Integer.valueOf(1), jdbc.queryForObject(
                    "SELECT count(*) FROM clinlims.sampletype_panel WHERE panel_id = ?" + " AND sample_type_id = ?",
                    Integer.class, Long.parseLong(panelId), sampleTypeId));

            // dropping the membership from the test side clears the junction too
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/tests/" + testId() + "/panels")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"memberships\":[]}")
                    .session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            assertEquals(Integer.valueOf(0),
                    jdbc.queryForObject("SELECT count(*) FROM clinlims.sampletype_panel WHERE panel_id = ?",
                            Integer.class, Long.parseLong(panelId)));
        } finally {
            jdbc.update("DELETE FROM clinlims.sampletype_panel WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.panel_item WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id = 952226");
            jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", Long.parseLong(panelId));
        }
    }

    /**
     * OGC-224 C3 — without the create-flow flag a first test never auto-flips the
     * active state (editing an existing panel preserves it as-is).
     */
    @org.junit.Test
    public void savePanelTests_noAutoActivateOutsideCreateFlow() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
        org.springframework.test.web.servlet.MvcResult created = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/rest/test-catalog/panels")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"PanelsIT NoAuto\",\"active\":false}").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        String panelId = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .put("/rest/test-catalog/panels/" + panelId + "/tests")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .content("{\"tests\":[{\"testId\":\"" + testId() + "\",\"position\":1}]}").session(authedSession()))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.panel.active").value(false));
        } finally {
            jdbc.update("DELETE FROM clinlims.panel_item WHERE panel_id = ?", Long.parseLong(panelId));
            jdbc.update("DELETE FROM clinlims.panel WHERE id = ?", Long.parseLong(panelId));
        }
    }

    /**
     * OGC-224 C2 — domain writes are validated: an unknown domain is 422, and the
     * domain can never move away from existing member tests (a panel never mixes
     * domains).
     */
    @org.junit.Test
    public void savePanelBasicInfo_domainValidationAndMemberGuard() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/rest/test-catalog/panels/" + panelAId + "/basic-info")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"domain\":\"BOGUS\"}")
                .session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isUnprocessableEntity());

        // TEST_ID's test.domain defaults to CLINICAL — with it as a member,
        // moving the panel to ENVIRONMENTAL would mix domains → 422
        put(membership(panelAId, 1));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/rest/test-catalog/panels/" + panelAId + "/basic-info")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"domain\":\"ENVIRONMENTAL\"}").session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isUnprocessableEntity());

        // the members' own domain is always acceptable
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/rest/test-catalog/panels/" + panelAId + "/basic-info")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"domain\":\"CLINICAL\"}")
                .session(authedSession()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.domain")
                        .value("CLINICAL"));
    }
}
