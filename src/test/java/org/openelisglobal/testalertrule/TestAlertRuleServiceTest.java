package org.openelisglobal.testalertrule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.hibernate.ObjectNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testalertrule.service.TestAlertRuleService;
import org.openelisglobal.testalertrule.valueholder.TestAlertRule;
import org.springframework.beans.factory.annotation.Autowired;

public class TestAlertRuleServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestAlertRuleService testAlertRuleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test_alert_rule.xml");
    }

    @Test
    public void getByTestId_withValidTestId_shouldReturnRulesForThatTest() {
        List<TestAlertRule> rules = testAlertRuleService.getByTestId("1");

        assertEquals("Expected exactly 3 CBC rules", 3, rules.size());
        rules.forEach(r -> assertEquals("Every returned rule must belong to test_id=1", "1", r.getTestId()));
    }

    @Test
    public void getByTestId_withValidTestId_shouldReturnCorrectRuleForUrinalysis() {
        List<TestAlertRule> rules = testAlertRuleService.getByTestId("2");

        assertEquals("Expected exactly 1 UA rule", 1, rules.size());
        assertEquals("UA Abnormal Alert", rules.get(0).getName());
        assertEquals("ABNORMAL", rules.get(0).getTriggerType());
        assertFalse("UA rule must be disabled in fixture", rules.get(0).getEnabled());
    }

    @Test
    public void getByTestId_withUnknownTestId_shouldReturnEmptyList() {
        List<TestAlertRule> rules = testAlertRuleService.getByTestId("99999");
        assertEquals("Expected zero rules for unknown test_id", 0, rules.size());
    }

    @Test
    public void get_withKnownId_shouldReturnCorrectRule() {
        TestAlertRule rule = testAlertRuleService.get("rule-001");

        assertEquals("rule-001", rule.getId());
        assertEquals("CBC All-Results Alert", rule.getName());
        assertEquals("ALL", rule.getTriggerType());
        assertEquals("1", rule.getTestId());
        assertTrue("rule-001 must be enabled", rule.getEnabled());
    }

    @Test(expected = ObjectNotFoundException.class)
    public void get_withUnknownId_shouldThrowForMissingEntity() {
        testAlertRuleService.get("non-existent-id");
    }

    @Test
    public void get_allTriggerRule_shouldHaveEmailAndSmsEnabled() {
        TestAlertRule rule = testAlertRuleService.get("rule-001");

        assertTrue("Expected notifyEmail=true", rule.getNotifyEmail());
        assertTrue("Expected notifySms=true", rule.getNotifySms());
        assertFalse("Expected notifyOrderingPhysician=false", rule.getNotifyOrderingPhysician());
    }

    @Test
    public void get_criticalTriggerRule_shouldHavePhysicianNotificationAndAckRequired() {
        TestAlertRule rule = testAlertRuleService.get("rule-002");

        assertEquals("CRITICAL", rule.getTriggerType());
        assertTrue("Expected notifyOrderingPhysician=true", rule.getNotifyOrderingPhysician());
        assertTrue("Expected acknowledgmentRequired=true", rule.getAcknowledgmentRequired());
    }

    @Test
    public void get_specificValueRule_shouldHaveCustomContactsPopulated() {
        TestAlertRule rule = testAlertRuleService.get("rule-004");

        assertEquals("SPECIFIC_VALUE", rule.getTriggerType());
        assertEquals("+1234567890", rule.getNotifyCustomPhone());
        assertEquals("lab-alerts@hospital.org", rule.getNotifyCustomEmail());
        assertTrue("Expected acknowledgmentRequired=true", rule.getAcknowledgmentRequired());
    }

    @Test
    public void get_disabledRule_shouldReturnEnabledFalse() {
        TestAlertRule rule = testAlertRuleService.get("rule-003");

        assertEquals("rule-003", rule.getId());
        assertFalse("Expected is_enabled=false for rule-003", rule.getEnabled());
        assertEquals("ABNORMAL", rule.getTriggerType());
        assertEquals("2", rule.getTestId());
    }

    @Test
    public void insert_newRule_shouldPersistAndBeRetrievable() {
        TestAlertRule newRule = new TestAlertRule();
        newRule.setTestId("1");
        newRule.setName("New Compliance Alert");
        newRule.setTriggerType("COMPLIANCE_BREACH");
        newRule.setEnabled(true);
        newRule.setNotifyEmail(true);
        newRule.setNotifySms(false);
        newRule.setNotifyOrderingPhysician(false);
        newRule.setNotifyPatient(false);
        newRule.setNotifyReferringFacility(false);
        newRule.setAcknowledgmentRequired(false);

        String insertedId = testAlertRuleService.insert(newRule);

        TestAlertRule fetched = testAlertRuleService.get(insertedId);
        assertEquals(insertedId, fetched.getId());
        assertEquals("New Compliance Alert", fetched.getName());
        assertEquals("COMPLIANCE_BREACH", fetched.getTriggerType());
        assertEquals("1", fetched.getTestId());
        assertTrue("inserted rule must be enabled", fetched.getEnabled());
        assertTrue("inserted rule must have notifyEmail=true", fetched.getNotifyEmail());
        assertFalse("inserted rule must have notifySms=false", fetched.getNotifySms());
        assertFalse("inserted rule must have acknowledgmentRequired=false", fetched.getAcknowledgmentRequired());
    }

    @Test
    public void insert_newRule_shouldAppearInGetByTestId() {
        int before = testAlertRuleService.getByTestId("2").size();

        TestAlertRule extraRule = new TestAlertRule();
        extraRule.setTestId("2");
        extraRule.setName("UA Extra Alert");
        extraRule.setTriggerType("ALL");
        extraRule.setEnabled(true);
        extraRule.setNotifyEmail(false);
        extraRule.setNotifySms(false);
        extraRule.setNotifyOrderingPhysician(false);
        extraRule.setNotifyPatient(false);
        extraRule.setNotifyReferringFacility(false);
        extraRule.setAcknowledgmentRequired(false);
        testAlertRuleService.insert(extraRule);

        List<TestAlertRule> after = testAlertRuleService.getByTestId("2");
        assertEquals("getByTestId must reflect the newly inserted rule", before + 1, after.size());
        assertEquals("UA Extra Alert", after.stream().filter(r -> "UA Extra Alert".equals(r.getName())).findFirst()
                .map(TestAlertRule::getName).orElse(null));
    }

    @Test
    public void update_existingRule_shouldPersistChanges() {
        TestAlertRule rule = testAlertRuleService.get("rule-003");
        assertFalse("Pre-condition: rule-003 should be disabled", rule.getEnabled());

        rule.setEnabled(true);
        rule.setNotifySms(true);
        testAlertRuleService.update(rule);

        TestAlertRule updated = testAlertRuleService.get("rule-003");
        assertTrue("Expected enabled=true after update", updated.getEnabled());
        assertTrue("Expected notifySms=true after update", updated.getNotifySms());
    }

    @Test
    public void delete_existingRule_shouldRemoveItFromDatabase() {
        List<TestAlertRule> beforeDelete = testAlertRuleService.getByTestId("2");
        assertEquals("Pre-condition: rule-003 should exist", 1, beforeDelete.size());

        testAlertRuleService.delete(beforeDelete.get(0));

        List<TestAlertRule> afterDelete = testAlertRuleService.getByTestId("2");
        assertTrue("Expected no UA rules after deletion", afterDelete.isEmpty());
    }

    @Test
    public void delete_existingRule_shouldNotAffectRulesForOtherTests() {
        TestAlertRule rule = testAlertRuleService.get("rule-003");
        testAlertRuleService.delete(rule);

        List<TestAlertRule> cbcRules = testAlertRuleService.getByTestId("1");
        assertEquals(3, cbcRules.size());
    }

    @Test
    public void getAll_shouldReturnAllFourFixtureRules() {
        List<TestAlertRule> all = testAlertRuleService.getAll();
        assertEquals("Expected exactly 4 rules from fixture", 4, all.size());
    }
}
