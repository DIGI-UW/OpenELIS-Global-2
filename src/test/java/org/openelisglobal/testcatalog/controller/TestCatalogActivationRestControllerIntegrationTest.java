package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testactivation.service.TestActivationAcknowledgmentService;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogActivationRestController.ActivateRequest;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService;
import org.openelisglobal.testcatalog.service.RangeCoverageValidationService.CoverageReport;
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

    @Autowired
    private TestService testService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private RangeCoverageValidationService coverageService;

    @Autowired
    private TestActivationAcknowledgmentService ackService;

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
                ackService);
        cleanup();
        // Seed the test INACTIVE so a successful activation visibly flips it.
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'N', ?, NOW())",
                TEST_ID, "ActivateIT", "activate IT", UUID.randomUUID().toString());
        resultTypeId = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_test_result", String.class);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_activation_acknowledgment WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.result_limits WHERE test_id = ?", TEST_ID);
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
        ResponseEntity<CoverageReport> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(testActive());
        assertEquals(0L, ackCount());
    }

    @org.junit.Test
    public void coverageGapsWithoutAck_returns409_andDoesNotActivate() {
        seedRange("M", 1d, Double.POSITIVE_INFINITY); // leading gap [0,1] for male
        ResponseEntity<CoverageReport> resp = controller.activateTest(String.valueOf(TEST_ID), null, authedRequest());
        assertEquals(409, resp.getStatusCode().value());
        assertTrue(resp.getBody().hasGaps());
        assertFalse("a 409 must not activate the test", testActive());
        assertEquals(0L, ackCount());
    }

    @org.junit.Test
    public void coverageGapsWithAck_activatesAndWritesAuditRow() {
        seedRange("M", 1d, Double.POSITIVE_INFINITY);
        ActivateRequest body = new ActivateRequest();
        body.gapsAcknowledged = "{\"male\":{\"status\":\"GAP\"}}";
        ResponseEntity<CoverageReport> resp = controller.activateTest(String.valueOf(TEST_ID), body, authedRequest());
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(testActive());
        assertEquals("an acknowledgment audit row (JSONB) is written", 1L, ackCount());
    }

    @org.junit.Test
    public void unknownTest_returns404() {
        assertEquals(404, controller.activateTest("99999999", null, authedRequest()).getStatusCode().value());
    }
}
