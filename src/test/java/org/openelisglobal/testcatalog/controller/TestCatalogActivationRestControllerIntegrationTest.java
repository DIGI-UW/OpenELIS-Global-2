package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController.ActivateRequest;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController.ActivationResult;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController.CompletenessReport;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService.CoverageReport;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

/**
 * OGC-949 M7 / OGC-973 — the activation safety gate end-to-end against a real
 * DB: complete coverage activates directly; coverage gaps without
 * acknowledgment return 409 and do NOT activate; gaps WITH acknowledgment
 * activate and write a (JSONB) audit row.
 */
public class TestCatalogActivationRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95401L;
    private static final String SECTION_NAME = "ActivateIT-Sec";

    @Autowired
    private TestService testService;

    @Autowired
    private LocalizationService localizationService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private RangeCoverageValidationService coverageService;

    @Autowired
    private TestActivationAcknowledgmentService ackService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogActivationRestController controller;
    private JdbcTemplate jdbc;
    private String resultTypeId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestCatalogActivationRestController(testService, resultLimitService, coverageService,
                ackService, componentService, testResultService);
        cleanup();
        // A lab unit is part of the completeness gate (OGC-1120), so the fixture
        // test carries one; the coverage tests below are then about coverage only.
        Localization sectionName = LocalizationServiceImpl.createNewLocalization(SECTION_NAME, SECTION_NAME,
                LocalizationServiceImpl.LocalizationType.TEST_NAME);
        sectionName.setSysUserId("1");
        String sectionLocId = localizationService.insert(sectionName);
        Long sectionId = jdbc.queryForObject("SELECT nextval('clinlims.test_section_seq')", Long.class);
        jdbc.update(
                "INSERT INTO clinlims.test_section (id, name, description, is_external, is_active,"
                        + " name_localization_id, lastupdated) VALUES (?, ?, ?, 'N', 'Y', ?, NOW())",
                sectionId, SECTION_NAME, "activate IT lab unit", Long.parseLong(sectionLocId));
        // Seed the test INACTIVE so a successful activation visibly flips it.
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated, test_section_id)"
                        + " VALUES (?, ?, ?, 'N', ?, NOW(), ?)",
                TEST_ID, "ActivateIT", "activate IT", UUID.randomUUID().toString(), sectionId);
        // Seed an active PRIMARY numeric component so the FR-57 completeness gate is
        // satisfied and these tests exercise the coverage gate, not completeness.
        jdbc.update(
                "INSERT INTO clinlims.test_result_component"
                        + " (id, test_id, code, label, display_order, result_type, allow_multiple_readings,"
                        + " is_primary, show_on_report, is_active, lastupdated)"
                        + " VALUES (?, ?, 'PRIMARY', 'Result', 0, 'N', false, true, true, 'Y', NOW())",
                UUID.randomUUID().toString(), TEST_ID);
        resultTypeId = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_test_result", String.class);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_activation_acknowledgment WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
        java.util.List<Long> sectionLocIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.test_section WHERE name = ?", Long.class, SECTION_NAME);
        jdbc.update("DELETE FROM clinlims.test_section WHERE name = ?", SECTION_NAME);
        for (Long locId : sectionLocIds) {
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", locId);
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

    private void seedRange(String gender, double minAge, double maxAge) {
        ResultLimit l = new ResultLimit();
        l.setTestId(String.valueOf(TEST_ID));
        l.setResultTypeId(resultTypeId);
        l.setGender(gender);
        l.setMinAge(minAge);
        l.setMaxAge(maxAge);
        l.setSysUserId("1");
        resultLimitService.insert(l);
    }

    private boolean testActive() {
        return testService.getTestById(String.valueOf(TEST_ID)).isActive();
    }

    private long ackCount() {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.test_activation_acknowledgment WHERE test_id = ?",
                Long.class, TEST_ID);
    }

    @org.junit.Test
    public void completeCoverage_activatesDirectly() {
        seedRange(null, 0d, Double.POSITIVE_INFINITY); // all-sex, full coverage from birth
        ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(testActive());
        assertEquals(0L, ackCount());
    }

    @org.junit.Test
    public void coverageGapsWithoutAck_returns409_andDoesNotActivate() {
        seedRange("M", 1d, Double.POSITIVE_INFINITY); // leading gap [0,1] for male
        ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(409, resp.getStatusCode().value());
        assertTrue(((CoverageReport) resp.getBody()).hasGaps());
        assertFalse("a 409 must not activate the test", testActive());
        assertEquals(0L, ackCount());
    }

    @org.junit.Test
    public void coverageGapsWithAck_activatesAndWritesAuditRow() {
        seedRange("M", 1d, Double.POSITIVE_INFINITY);
        ActivateRequest body = new ActivateRequest();
        body.gapsAcknowledged = "{\"male\":{\"status\":\"GAP\"}}";
        ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), body, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(testActive());
        assertEquals("an acknowledgment audit row (JSONB) is written", 1L, ackCount());
    }

    /**
     * OGC-1120 — a test without a lab unit could be activated, and once linked to a
     * sample type it broke the whole sample type's order-entry test list. The gate
     * now names the gap instead.
     */
    @org.junit.Test
    public void noLabUnit_returns422_namingTheGap_andDoesNotActivate() {
        seedRange(null, 0d, Double.POSITIVE_INFINITY);
        jdbc.update("UPDATE clinlims.test SET test_section_id = NULL WHERE id = ?", TEST_ID);
        ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(422, resp.getStatusCode().value());
        CompletenessReport report = (CompletenessReport) resp.getBody();
        assertFalse(report.complete);
        assertTrue("the checklist must name the lab unit", report.missing.contains("NO_LAB_UNIT"));
        assertTrue(report.messages.stream().anyMatch(m -> m.toLowerCase().contains("lab unit")));
        assertFalse("a 422 must not activate the test", testActive());
    }

    /**
     * FR-18 (OGC-1119) — activation re-surfaces the LOINC guardrails: the success
     * body names the other active test sharing this test's LOINC, and the
     * activation still goes through (warn, never block).
     */
    @org.junit.Test
    public void activation_reportsASharedLoinc_withoutBlocking() {
        long twinId = TEST_ID + 1;
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", twinId);
        jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", twinId);
        jdbc.update("UPDATE clinlims.test SET loinc = '4548-4' WHERE id = ?", TEST_ID);
        jdbc.update("INSERT INTO clinlims.localization (id, description, lastupdated) VALUES (?, ?, NOW())", twinId,
                "ActivateIT twin");
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, loinc, guid, lastupdated,"
                        + " name_localization_id) VALUES (?, ?, ?, 'Y', '4548-4', ?, NOW(), ?)",
                twinId, "ActivateIT twin", "activate IT twin", UUID.randomUUID().toString(), twinId);
        try {
            seedRange(null, 0d, Double.POSITIVE_INFINITY);
            ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
            assertEquals(200, resp.getStatusCode().value());
            assertTrue(testActive());
            ActivationResult result = (ActivationResult) resp.getBody();
            assertTrue("the success body must carry the LOINC warnings", result.loincIntegrity != null);
            assertEquals(1, result.loincIntegrity.duplicates.size());
            assertEquals(String.valueOf(twinId), result.loincIntegrity.duplicates.get(0).testId);
        } finally {
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", twinId);
            jdbc.update("DELETE FROM clinlims.localization WHERE id = ?", twinId);
        }
    }

    @org.junit.Test
    public void unknownTest_returns404() {
        assertEquals(404, controller.activateTest("99999999", null, authedRequest()).getStatusCode().value());
    }

    /**
     * OGC-1153 defect 5a — activation also flips {@code orderable} (FRS lifecycle:
     * Active ⇒ orderable), which the old bare-coverage-report body never mentioned,
     * so a client had to reload to find out. The success body must state the
     * resulting flags, and must stay a superset of the coverage report the 409 flow
     * consumes.
     */
    @org.junit.Test
    public void activateResponse_statesTheResultingActiveAndOrderableFlags() {
        seedRange(null, 0d, Double.POSITIVE_INFINITY);
        ResponseEntity<?> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(200, resp.getStatusCode().value());

        ActivationResult result = (ActivationResult) resp.getBody();
        assertEquals(String.valueOf(TEST_ID), result.testId);
        assertTrue("activation must report the test as active", result.active);
        assertTrue("activation sets orderable, so the response must say so", result.orderable);
        assertTrue("the success body must still carry the coverage report", result.male != null);

        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertTrue("the reported flags must match what was persisted", reloaded.isActive());
        assertTrue(Boolean.TRUE.equals(reloaded.getOrderable()));
    }
}
