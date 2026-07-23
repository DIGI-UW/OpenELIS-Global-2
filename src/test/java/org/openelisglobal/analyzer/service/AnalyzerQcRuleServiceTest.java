package org.openelisglobal.analyzer.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule.RuleType;

/**
 * Unit tests for AnalyzerQcRuleService (FR-15). Tests CRUD operations,
 * validation, and DTO mapping.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerQcRuleServiceTest {

    @Mock
    private AnalyzerQcRuleDAO analyzerQcRuleDAO;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AnalyzerQcRuleServiceImpl service;

    private AnalyzerQcRule rule1;
    private AnalyzerQcRule rule2;
    private AnalyzerQcRule rule3;
    private AnalyzerQcRule rule4;

    @Before
    public void setUp() {
        rule1 = createRule("rule-001", "1", RuleType.FIELD_EQUALS, "O.12", "Q", true, 1);
        rule2 = createRule("rule-002", "1", RuleType.SPECIMEN_ID_PREFIX, null, "QC-", true, 2);
        rule3 = createRule("rule-003", "1", RuleType.SPECIMEN_ID_PATTERN, null, "^CTRL-\\d{4}$", false, 3);
        rule4 = createRule("rule-004", "1", RuleType.FIELD_CONTAINS, "QC_TASK", "CONTROL", true, 4);
    }

    private AnalyzerQcRule createRule(String id, String analyzerId, RuleType type, String targetField, String operand,
            boolean active, int order) {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setId(id);
        rule.setAnalyzerId(analyzerId);
        rule.setRuleType(type);
        rule.setTargetField(targetField);
        rule.setOperand(operand);
        rule.setActive(active);
        rule.setDisplayOrder(order);
        return rule;
    }

    // ---- getRulesForAnalyzer ----

    @Test
    public void testGetRulesForAnalyzer_DelegatesToDAO() {
        when(analyzerQcRuleDAO.findByAnalyzerId("1")).thenReturn(List.of(rule1, rule2, rule3, rule4));

        List<AnalyzerQcRule> rules = service.getRulesForAnalyzer("1");

        assertEquals(4, rules.size());
        verify(analyzerQcRuleDAO).findByAnalyzerId("1");
    }

    @Test
    public void testGetRulesForAnalyzer_WithNoRules_ReturnsEmptyList() {
        when(analyzerQcRuleDAO.findByAnalyzerId("2")).thenReturn(List.of());

        List<AnalyzerQcRule> rules = service.getRulesForAnalyzer("2");

        assertEquals(0, rules.size());
    }

    // ---- getActiveRulesForAnalyzer ----

    @Test
    public void testGetActiveRulesForAnalyzer_DelegatesToDAO() {
        when(analyzerQcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule1, rule2, rule4));

        List<AnalyzerQcRule> rules = service.getActiveRulesForAnalyzer("1");

        assertEquals(3, rules.size());
        verify(analyzerQcRuleDAO).findActiveByAnalyzerId("1");
    }

    // ---- hasAtLeastOneActiveRule ----

    @Test
    public void testHasAtLeastOneActiveRule_WithOneActiveRule_ReturnsTrue() {
        when(analyzerQcRuleDAO.countActiveByAnalyzerId("1")).thenReturn(1L);

        assertEquals(true, service.hasAtLeastOneActiveRule("1"));
    }

    @Test
    public void testHasAtLeastOneActiveRule_WithNoActiveRules_ReturnsFalse() {
        when(analyzerQcRuleDAO.countActiveByAnalyzerId("2")).thenReturn(0L);

        assertEquals(false, service.hasAtLeastOneActiveRule("2"));
    }

    @Test
    public void testHasAtLeastOneActiveRule_WithNonExistentAnalyzer_ReturnsFalse() {
        when(analyzerQcRuleDAO.countActiveByAnalyzerId("999")).thenReturn(0L);

        assertEquals(false, service.hasAtLeastOneActiveRule("999"));
    }

    // ---- createRule ----

    @Test
    public void testCreateRule_WithValidFieldEqualsRule_Succeeds() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);
        when(analyzerQcRuleDAO.findByAnalyzerId("1")).thenReturn(List.of(rule1, rule2));

        AnalyzerQcRule newRule = new AnalyzerQcRule();
        newRule.setRuleType(RuleType.FIELD_EQUALS);
        newRule.setTargetField("O.16");
        newRule.setOperand("QC");
        newRule.setActive(true);

        AnalyzerQcRule created = service.createRule("1", newRule, "1");

        assertEquals("1", created.getAnalyzerId());
        assertEquals(RuleType.FIELD_EQUALS, created.getRuleType());
        assertEquals("O.16", created.getTargetField());
        assertEquals("QC", created.getOperand());
        assertEquals(true, created.isActive());
        assertEquals(3, created.getDisplayOrder()); // 2 existing + 1
        assertEquals("1", created.getSysUserId());
    }

    @Test
    public void testCreateRule_WithExplicitDisplayOrder_UsesProvidedOrder() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);
        when(analyzerQcRuleDAO.findByAnalyzerId("1")).thenReturn(List.of());

        AnalyzerQcRule newRule = new AnalyzerQcRule();
        newRule.setRuleType(RuleType.SPECIMEN_ID_PREFIX);
        newRule.setOperand("QC-");
        newRule.setDisplayOrder(99);

        AnalyzerQcRule created = service.createRule("1", newRule, "1");

        assertEquals(99, created.getDisplayOrder());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRule_WithNonExistentAnalyzer_ThrowsIllegalArgumentException() {
        when(analyzerService.get("999")).thenReturn(null);

        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setTargetField("O.12");
        rule.setOperand("Q");
        service.createRule("999", rule, "1");
    }

    @Test(expected = IllegalStateException.class)
    public void testCreateRule_ExceedingMaxRulesLimit_ThrowsIllegalStateException() {
        Analyzer analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);

        List<AnalyzerQcRule> fiftyRules = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fiftyRules.add(createRule("r-" + i, "1", RuleType.FIELD_EQUALS, "O.12", "Q", true, i));
        }
        when(analyzerQcRuleDAO.findByAnalyzerId("1")).thenReturn(fiftyRules);

        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setTargetField("O.12");
        rule.setOperand("Q");
        service.createRule("1", rule, "1");
    }

    // ---- validateRule ----

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_NullRuleType_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setOperand("test");
        service.validateRule(rule);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_NullOperand_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setTargetField("O.12");
        service.validateRule(rule);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_BlankOperand_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setTargetField("O.12");
        rule.setOperand("   ");
        service.validateRule(rule);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_FieldEquals_NullTargetField_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setOperand("Q");
        service.validateRule(rule);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_FieldEquals_BlankTargetField_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_EQUALS);
        rule.setTargetField("  ");
        rule.setOperand("Q");
        service.validateRule(rule);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_FieldContains_NullTargetField_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.FIELD_CONTAINS);
        rule.setOperand("CTRL");
        service.validateRule(rule);
    }

    @Test
    public void testValidateRule_SpecimenIdPrefix_NullTargetField_Succeeds() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.SPECIMEN_ID_PREFIX);
        rule.setOperand("QC-");
        service.validateRule(rule); // no exception
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateRule_SpecimenIdPattern_InvalidRegex_Throws() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.SPECIMEN_ID_PATTERN);
        rule.setOperand("[invalid(");
        service.validateRule(rule);
    }

    @Test
    public void testValidateRule_SpecimenIdPattern_ValidComplexRegex_Succeeds() {
        AnalyzerQcRule rule = new AnalyzerQcRule();
        rule.setRuleType(RuleType.SPECIMEN_ID_PATTERN);
        rule.setOperand("^(QC|CTRL)-[A-Z]{2}\\d{3,5}$");
        service.validateRule(rule); // no exception
    }

    // ---- updateRule ----

    @Test
    public void testUpdateRule_ChangesRuleTypeAndOperand_Succeeds() {
        when(analyzerQcRuleDAO.get("rule-001")).thenReturn(java.util.Optional.of(rule1));
        when(analyzerQcRuleDAO.update(any(AnalyzerQcRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalyzerQcRule updates = new AnalyzerQcRule();
        updates.setRuleType(RuleType.SPECIMEN_ID_PREFIX);
        updates.setOperand("CTRL-");
        updates.setActive(true);

        AnalyzerQcRule updated = service.updateRule("1", "rule-001", updates, "1");

        assertEquals(RuleType.SPECIMEN_ID_PREFIX, updated.getRuleType());
        assertEquals("CTRL-", updated.getOperand());
        assertEquals("1", updated.getAnalyzerId());
        // Verify DAO update was invoked
        verify(analyzerQcRuleDAO).update(any(AnalyzerQcRule.class));
    }

    @Test
    public void testUpdateRule_Deactivates_PreservesOtherFields() {
        when(analyzerQcRuleDAO.get("rule-001")).thenReturn(java.util.Optional.of(rule1));
        when(analyzerQcRuleDAO.update(any(AnalyzerQcRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AnalyzerQcRule updates = new AnalyzerQcRule();
        updates.setActive(false);

        AnalyzerQcRule updated = service.updateRule("1", "rule-001", updates, "1");

        assertEquals(false, updated.isActive());
        // Verify original fields preserved when only active changed
        assertEquals(RuleType.FIELD_EQUALS, updated.getRuleType());
        assertEquals("O.12", updated.getTargetField());
        assertEquals("Q", updated.getOperand());
        verify(analyzerQcRuleDAO).update(any(AnalyzerQcRule.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRule_WrongAnalyzerId_Throws() {
        when(analyzerQcRuleDAO.get("rule-001")).thenReturn(java.util.Optional.of(rule1));

        AnalyzerQcRule updates = new AnalyzerQcRule();
        updates.setActive(false);

        service.updateRule("2", "rule-001", updates, "1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateRule_NonExistentRuleId_Throws() {
        when(analyzerQcRuleDAO.get("nonexistent")).thenReturn(java.util.Optional.empty());

        AnalyzerQcRule updates = new AnalyzerQcRule();
        updates.setActive(false);

        service.updateRule("1", "nonexistent", updates, "1");
    }

    // ---- deleteRule ----

    @Test
    public void testDeleteRule_CallsDAO() {
        service.deleteRule("1", "rule-001");

        verify(analyzerQcRuleDAO).deleteByAnalyzerIdAndRuleId("1", "rule-001");
    }

    // ---- getActiveRuleDtosForAnalyzer ----

    @Test
    public void testGetActiveRuleDtosForAnalyzer_ReturnsMappedDtos() {
        when(analyzerQcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule1, rule2, rule4));

        List<QcRuleDto> dtos = service.getActiveRuleDtosForAnalyzer("1");

        assertEquals(3, dtos.size());
        assertEquals("FIELD_EQUALS", dtos.get(0).ruleType());
        assertEquals("O.12", dtos.get(0).targetField());
        assertEquals("Q", dtos.get(0).operand());
        assertEquals("SPECIMEN_ID_PREFIX", dtos.get(1).ruleType());
        assertNull(dtos.get(1).targetField());
        assertEquals("QC-", dtos.get(1).operand());
        assertEquals("FIELD_CONTAINS", dtos.get(2).ruleType());
        assertEquals("QC_TASK", dtos.get(2).targetField());
        assertEquals("CONTROL", dtos.get(2).operand());
    }

    @Test
    public void testGetActiveRuleDtosForAnalyzer_WithNoRules_ReturnsEmptyList() {
        when(analyzerQcRuleDAO.findActiveByAnalyzerId("2")).thenReturn(List.of());

        List<QcRuleDto> dtos = service.getActiveRuleDtosForAnalyzer("2");

        assertEquals(0, dtos.size());
    }
}
