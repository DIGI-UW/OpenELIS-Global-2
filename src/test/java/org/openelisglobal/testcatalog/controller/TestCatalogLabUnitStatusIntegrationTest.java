package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.management.controller.rest.LabUnitManagementRestController;
import org.openelisglobal.common.management.controller.rest.LabUnitManagementRestController.BulkTestAssignmentRequest;
import org.openelisglobal.common.management.controller.rest.LabUnitManagementRestController.DeactivationRequest;
import org.openelisglobal.common.management.controller.rest.LabUnitManagementRestController.LabUnitManagementDTO;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * OGC-1459: saving a test never changes its lab unit's status, and moving tests
 * never reactivates the unit they move to. A deliberate deactivation is undone
 * only by someone reactivating the unit.
 */
public class TestCatalogLabUnitStatusIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95491L;

    private static final long INACTIVE_A = 95492L;

    private static final long INACTIVE_B = 95493L;

    private static final long ACTIVE_C = 95494L;

    private static final long SAMPLE_TYPE = 95495L;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private org.openelisglobal.test.service.TestSectionService testSectionService;
    @Autowired
    private org.openelisglobal.localization.service.LocalizationService localizationService;
    @Autowired
    private org.openelisglobal.localization.service.SupportedLocaleService supportedLocaleService;
    @Autowired
    private org.openelisglobal.test.service.TestService testService;
    @Autowired
    private org.openelisglobal.testconfiguration.service.TestSectionCreateService testSectionCreateService;
    @Autowired
    private org.openelisglobal.testconfiguration.service.TestSectionTestAssignService testSectionTestAssignService;
    @Autowired
    private org.openelisglobal.role.service.RoleService roleService;
    @Autowired
    private org.openelisglobal.analysis.service.AnalysisService analysisService;

    private LabUnitManagementRestController labUnits;

    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        labUnits = new LabUnitManagementRestController();
        ReflectionTestUtils.setField(labUnits, "testSectionService", testSectionService);
        ReflectionTestUtils.setField(labUnits, "localizationService", localizationService);
        ReflectionTestUtils.setField(labUnits, "supportedLocaleService", supportedLocaleService);
        ReflectionTestUtils.setField(labUnits, "testService", testService);
        ReflectionTestUtils.setField(labUnits, "testSectionCreateService", testSectionCreateService);
        ReflectionTestUtils.setField(labUnits, "testSectionTestAssignService", testSectionTestAssignService);
        ReflectionTestUtils.setField(labUnits, "roleService", roleService);
        ReflectionTestUtils.setField(labUnits, "analysisService", analysisService);
        cleanup();
        seedLabUnit(INACTIVE_A, "LabUnitStatusIT A", false);
        seedLabUnit(INACTIVE_B, "LabUnitStatusIT B", false);
        seedLabUnit(ACTIVE_C, "LabUnitStatusIT C", true);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, antimicrobial_resistance,"
                        + " orderable, test_section_id, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', false, true, ?, NOW())",
                TEST_ID, "LabUnitStatusIT", "LabUnitStatusIT desc", UUID.randomUUID().toString(), INACTIVE_A);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void seedLabUnit(long id, String name, boolean active) {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", id, name);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, is_active, sort_order,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, ?, 'N', ?, ?, ?, NOW())",
                id, name, name, active ? "Y" : "N", id, id);
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        String created = "SELECT id FROM clinlims.test WHERE local_code = 'LUSIT1'";
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id IN (" + created + ")");
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id IN (" + created + ")");
        jdbc.update("DELETE FROM clinlims.test WHERE local_code = 'LUSIT1'");
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", SAMPLE_TYPE);
        for (long id : new long[] { INACTIVE_A, INACTIVE_B, ACTIVE_C }) {
            jdbc.update("DELETE FROM clinlims.test_section WHERE id = ?", id);
            jdbc.update("DELETE FROM clinlims.localization_value WHERE localization_id = ?", id);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", id);
        }
    }

    private static MockHttpSession session() {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(1);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        return session;
    }

    private static org.springframework.mock.web.MockHttpServletRequest request() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setSession(session());
        return request;
    }

    private MvcResult send(String method, String url, String body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method), url)
                .contentType(MediaType.APPLICATION_JSON).content(body).session(session())).andReturn();
    }

    private String status(long labUnitId) {
        return jdbc.queryForObject("SELECT is_active FROM clinlims.test_section WHERE id = ?", String.class, labUnitId);
    }

    private Long unitOfTest() {
        return jdbc.queryForObject("SELECT test_section_id FROM clinlims.test WHERE id = ?", Long.class, TEST_ID);
    }

    @org.junit.Test
    public void aBasicInfoSaveKeepsTheTestsInactiveUnitInactive() throws Exception {
        MvcResult saved = send("PUT", "/rest/test-catalog/tests/" + TEST_ID + "/basic-info",
                "{\"labUnitId\":\"" + INACTIVE_A + "\",\"description\":\"LabUnitStatusIT edited\"}");

        assertEquals(200, saved.getResponse().getStatus());
        assertEquals("N", status(INACTIVE_A));
        assertEquals(Long.valueOf(INACTIVE_A), unitOfTest());
    }

    @org.junit.Test
    public void aBasicInfoSaveCannotMoveATestIntoAnInactiveUnit() throws Exception {
        MvcResult refused = send("PUT", "/rest/test-catalog/tests/" + TEST_ID + "/basic-info",
                "{\"labUnitId\":\"" + INACTIVE_B + "\"}");

        assertEquals(422, refused.getResponse().getStatus());
        assertEquals("labUnitId", new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(refused.getResponse().getContentAsString()).path("invalidField").asText());
        assertEquals("N", status(INACTIVE_B));
        assertEquals(Long.valueOf(INACTIVE_A), unitOfTest());

        assertEquals(200, send("PUT", "/rest/test-catalog/tests/" + TEST_ID + "/basic-info",
                "{\"labUnitId\":\"" + ACTIVE_C + "\"}").getResponse().getStatus());
        assertEquals(Long.valueOf(ACTIVE_C), unitOfTest());
    }

    /**
     * A lab unit that is not an id, or does not exist, is refused on the field
     * instead of answering 500; a padded id still names its unit.
     */
    @org.junit.Test
    public void aBasicInfoSaveWithAnUnknownUnitIsRefusedOnTheField() throws Exception {
        for (String unit : new String[] { "abc", "999999" }) {
            MvcResult refused = send("PUT", "/rest/test-catalog/tests/" + TEST_ID + "/basic-info",
                    "{\"labUnitId\":\"" + unit + "\"}");
            assertEquals(unit, 422, refused.getResponse().getStatus());
            assertEquals("labUnitId", new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(refused.getResponse().getContentAsString()).path("invalidField").asText());
        }
        assertEquals(Long.valueOf(INACTIVE_A), unitOfTest());

        assertEquals(200, send("PUT", "/rest/test-catalog/tests/" + TEST_ID + "/basic-info",
                "{\"labUnitId\":\" " + ACTIVE_C + " \"}").getResponse().getStatus());
        assertEquals(Long.valueOf(ACTIVE_C), unitOfTest());
    }

    /**
     * Creating a test in an inactive unit is refused like moving one there, instead
     * of switching the unit back on.
     */
    @org.junit.Test
    public void aNewTestCannotBeCreatedInAnInactiveUnit() throws Exception {
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())",
                SAMPLE_TYPE, "LabUnitStatusIT type");
        jdbc.update("INSERT INTO clinlims.type_of_sample (id, description, domain, is_active, sort_order,"
                + " name_localization_id, lastupdated) VALUES (?, 'LabUnitStatusIT type', 'H', true, 1, ?, NOW())",
                SAMPLE_TYPE, SAMPLE_TYPE);

        MvcResult refused = send("POST", "/rest/test-catalog/tests",
                "{\"name\":\"LabUnitStatusIT new\",\"reportingName\":\"LabUnitStatusIT new\",\"code\":\"LUSIT1\","
                        + "\"domain\":\"CLINICAL\",\"labUnitId\":\"" + INACTIVE_B + "\",\"sampleTypeIds\":[\""
                        + SAMPLE_TYPE + "\"]}");

        assertEquals(422, refused.getResponse().getStatus());
        assertEquals("labUnitId", new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(refused.getResponse().getContentAsString()).path("invalidField").asText());
        assertEquals("N", status(INACTIVE_B));
        assertEquals(Integer.valueOf(0),
                jdbc.queryForObject("SELECT count(*) FROM clinlims.test WHERE local_code = 'LUSIT1'", Integer.class));
    }

    /**
     * A deactivation cannot hand its tests to an inactive unit. A bulk reassign may
     * (decision D4: emptying a unit into a switched-off one is a legitimate
     * reorganisation step), and the destination stays switched off.
     */
    @org.junit.Test
    public void deactivatingIntoAnInactiveUnitIsRefused_andAReassignNeverReactivates() throws Exception {
        jdbc.update("UPDATE clinlims.test SET test_section_id = ? WHERE id = ?", ACTIVE_C, TEST_ID);

        DeactivationRequest deactivation = new DeactivationRequest();
        deactivation.confirmation = "DEACTIVATE";
        deactivation.option = "reassign";
        deactivation.destinationLabUnitId = String.valueOf(INACTIVE_B);
        org.springframework.http.ResponseEntity<?> refusedDeactivation = labUnits.deactivateLabUnit(request(),
                String.valueOf(ACTIVE_C), deactivation);
        assertEquals(422, refusedDeactivation.getStatusCode().value());
        assertEquals("the screen can name the refusal in the user's language", "destination.inactive",
                ((org.openelisglobal.common.management.controller.rest.LabUnitManagementRestController.ApiResponse<?>) refusedDeactivation
                        .getBody()).getRefusal());
        assertEquals("N", status(INACTIVE_B));
        assertEquals("the refused deactivation leaves the source alone", "Y", status(ACTIVE_C));
        assertEquals(Long.valueOf(ACTIVE_C), unitOfTest());

        BulkTestAssignmentRequest reassign = new BulkTestAssignmentRequest();
        reassign.testIds = java.util.List.of(String.valueOf(TEST_ID));
        reassign.destinationLabUnitId = String.valueOf(INACTIVE_B);
        assertEquals(200,
                labUnits.reassignTests(request(), String.valueOf(ACTIVE_C), reassign).getStatusCode().value());
        assertEquals(Long.valueOf(INACTIVE_B), unitOfTest());
        assertEquals("N", status(INACTIVE_B));
    }

    /**
     * OGC-1463: a save from a page loaded before another admin deactivated the unit
     * is refused as stale, so the unit stays off; so is a deactivation asked from a
     * page older than the unit's last change.
     */
    @org.junit.Test
    public void aSaveFromAPageOlderThanTheDeactivationIsRefusedAsStale() throws Exception {
        LabUnitManagementDTO pageA = labUnits.getLabUnit(String.valueOf(ACTIVE_C)).getBody().getData();
        DeactivationRequest deactivation = new DeactivationRequest();
        deactivation.confirmation = "DEACTIVATE";
        deactivation.option = "keep";
        deactivation.lastupdated = pageA.getLastupdated();
        assertEquals(200,
                labUnits.deactivateLabUnit(request(), String.valueOf(ACTIVE_C), deactivation).getStatusCode().value());

        pageA.setDescription("Changed on an old page");
        assertEquals(409, labUnits.updateLabUnit(request(), String.valueOf(ACTIVE_C), pageA).getStatusCode().value());
        assertEquals("N", status(ACTIVE_C));

        assertEquals("a second deactivation from the same old page is stale too", 409,
                labUnits.deactivateLabUnit(request(), String.valueOf(ACTIVE_C), deactivation).getStatusCode().value());
    }

    /**
     * OGC-1465: a rename onto another unit's name is refused, as a create is; a
     * name the unit already holds is not a rename.
     */
    @org.junit.Test
    public void aRenameOntoAnotherUnitsNameIsRefused() throws Exception {
        long valueId = 95499L;
        for (long id : new long[] { INACTIVE_A, ACTIVE_C }) {
            jdbc.update(
                    "INSERT INTO clinlims.localization_value (id, localization_id, locale, value, last_updated)"
                            + " VALUES (?, ?, 'en', ?, NOW())",
                    valueId--, id, id == ACTIVE_C ? "LabUnitStatusIT C" : "LabUnitStatusIT A");
        }
        LabUnitManagementDTO unitC = labUnits.getLabUnit(String.valueOf(ACTIVE_C)).getBody().getData();

        unitC.setNames(java.util.Map.of("en", " labunitstatusit a "));
        assertEquals(409, labUnits.updateLabUnit(request(), String.valueOf(ACTIVE_C), unitC).getStatusCode().value());

        LabUnitManagementDTO unchanged = labUnits.getLabUnit(String.valueOf(ACTIVE_C)).getBody().getData();
        unchanged.setNames(java.util.Map.of("en", "LabUnitStatusIT C"));
        unchanged.setDescription("only the description changes");
        assertEquals(200,
                labUnits.updateLabUnit(request(), String.valueOf(ACTIVE_C), unchanged).getStatusCode().value());
    }
}
