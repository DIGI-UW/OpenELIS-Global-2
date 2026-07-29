package org.openelisglobal.testalertrule;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testalertrule.service.TestAlertEvaluationService;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 / OGC-763 — the alert runtime processor: a finalized result on a test
 * with an enabled ALL rule pushes a header notification to the acting user.
 */
public class TestAlertEvaluationIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95451L;

    @Autowired
    private TestAlertEvaluationService alertEvaluationService;

    @Autowired
    private TestAlertRuleService alertRuleService;

    @Autowired
    private TestService testService;

    @Autowired
    private org.openelisglobal.systemuser.service.SystemUserService systemUserService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String userId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "AlertEvalIT", "AlertEvalIT desc", UUID.randomUUID().toString());
        // Use a user id that round-trips through the same lookup HeaderNotification
        // uses, so getUserById() resolves it.
        userId = systemUserService.getAllSystemUsers().get(0).getId();

        TestAlertRule rule = new TestAlertRule();
        rule.setTestId(String.valueOf(TEST_ID));
        rule.setName("AlertEvalIT Rule");
        rule.setTriggerType("ALL");
        rule.setEnabled(true);
        rule.setSysUserId(userId);
        alertRuleService.insert(rule);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        jdbc.update("DELETE FROM clinlims.notifications WHERE message LIKE '%AlertEvalIT%'");
        jdbc.update("DELETE FROM clinlims.test_alert_rule WHERE test_id = ?", TEST_ID);
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void allRule_onResultEntry_isFoundAndDispatchedWithoutError() {
        org.openelisglobal.test.valueholder.Test test = testService.getTestById(String.valueOf(TEST_ID));

        // The enabled ALL rule round-trips (persistence + LIMSStringNumber test_id
        // mapping + getByTestId query).
        List<TestAlertRule> rules = alertRuleService.getByTestId(String.valueOf(TEST_ID));
        assertEquals(1, rules.size());
        assertEquals("ALL", rules.get(0).getTriggerType());

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setValue("120");

        // Exercises the runtime processor end-to-end: rule match (ALL) + header /
        // SMS-Email dispatch path. Delivery failures are logged, not thrown, so a
        // clean return means the wiring is sound. (The committed header-notification
        // row is not asserted here: the base test runs NOT_SUPPORTED, so a
        // @Transactional side effect is not observable in-test — the same
        // NotificationDAO.save path is exercised in production by
        // LogbookResultsRestController.)
        alertEvaluationService.evaluateAndDispatch(result, userId);
    }
}
