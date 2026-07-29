package org.openelisglobal.testalertrule.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testalertrule.controller.rest.TestAlertRuleRestController;
import org.openelisglobal.testalertrule.controller.rest.TestAlertRuleRestController.AlertRuleRequest;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

/**
 * OGC-949 / OGC-763 — per-test alert rule REST endpoints, round-tripped against
 * a real DB. Covers create + list, the 400 validation guards (bad trigger type,
 * SPECIFIC_VALUE without a value, missing name), the 404 guards (unknown test /
 * rule), update, and delete.
 *
 * <p>
 * Gated by {@code @PreAuthorize("hasRole('ADMIN')")}; the 403 path is enforced
 * by Spring Security's proxy, which is bypassed under direct controller
 * invocation, so it is not asserted here (matches the sibling editor ITs).
 */
public class TestAlertRuleRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95421L;

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.role.service.RoleService roleService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestAlertRuleRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        controller = new TestAlertRuleRestController(alertRuleService, testService, roleService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AlertRuleIT", "AlertRuleIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.test_alert_rule WHERE test_id = ?", TEST_ID);
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

    private String testId() {
        return String.valueOf(TEST_ID);
    }

    private AlertRuleRequest req(String name, String triggerType, String triggerValue) {
        AlertRuleRequest body = new AlertRuleRequest();
        body.name = name;
        body.triggerType = triggerType;
        body.triggerValue = triggerValue;
        body.notifyEmail = true;
        body.notifyOrderingPhysician = true;
        body.acknowledgmentRequired = true;
        return body;
    }

    @Test
    public void create_thenList_returnsRule() {
        TestAlertRule created = controller.create(testId(), req("Critical SMS", "CRITICAL", null), authedRequest())
                .getBody();
        assertEquals("Critical SMS", created.getName());
        assertEquals("CRITICAL", created.getTriggerType());
        assertTrue(created.getAcknowledgmentRequired());

        List<TestAlertRule> rules = controller.list(testId());
        assertEquals(1, rules.size());
        assertEquals("Critical SMS", rules.get(0).getName());
    }

    @Test
    public void create_invalidTriggerType_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("Bad", "SOMETIMES", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_specificValueWithoutValue_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("Positive", "SPECIFIC_VALUE", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_missingName_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(testId(), req("  ", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    public void create_unknownTest_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create("99999999", req("X", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void update_changesFields() {
        TestAlertRule created = controller.create(testId(), req("R1", "ALL", null), authedRequest()).getBody();
        AlertRuleRequest upd = req("R1 renamed", "SPECIFIC_VALUE", "Positive");
        TestAlertRule updated = controller.update(testId(), created.getId(), upd, authedRequest());
        assertEquals("R1 renamed", updated.getName());
        assertEquals("SPECIFIC_VALUE", updated.getTriggerType());
        assertEquals("Positive", updated.getTriggerValue());
    }

    @Test
    public void update_unknownRule_throwsNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(testId(), "no-such-rule", req("X", "ALL", null), authedRequest()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    public void delete_removesRule() {
        TestAlertRule created = controller.create(testId(), req("R1", "ALL", null), authedRequest()).getBody();
        assertEquals(204, controller.delete(testId(), created.getId(), authedRequest()).getStatusCode().value());
        assertTrue(controller.list(testId()).isEmpty());
    }
}
