package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AstmQcRuleDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AstmQcRule;

@RunWith(MockitoJUnitRunner.class)
public class AstmQcRuleServiceTest {

    @Mock
    private AstmQcRuleDAO qcRuleDAO;

    @Mock
    private AnalyzerService analyzerService;

    @InjectMocks
    private AstmQcRuleServiceImpl service;

    private Analyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerService.get("1")).thenReturn(analyzer);
    }

    @Test
    public void createUpdateDeleteRule_PerformsCrud() {
        AstmQcRule rule = buildRule("rule-1", "FIELD_EQUALS", "SPECIMEN_ID_FIELD", "QC");
        when(qcRuleDAO.get("rule-1")).thenReturn(Optional.of(rule));

        Map<String, Object> create = new HashMap<>();
        create.put("ruleType", "FIELD_EQUALS");
        create.put("targetField", "SPECIMEN_ID_FIELD");
        create.put("operand", "QC");
        create.put("isActive", true);
        create.put("sortOrder", 1);
        service.create("1", create);
        verify(qcRuleDAO).insert(any(AstmQcRule.class));

        Map<String, Object> update = new HashMap<>();
        update.put("operand", "QC-");
        service.update("rule-1", update);
        verify(qcRuleDAO).update(rule);

        service.delete("rule-1");
        verify(qcRuleDAO).delete(rule);
    }

    @Test
    public void evaluateAsQc_OrLogicAnyMatchReturnsTrue() {
        AstmQcRule rule1 = buildRule("rule-1", "FIELD_EQUALS", "RESULT_STATUS_FIELD", "N");
        AstmQcRule rule2 = buildRule("rule-2", "SPECIMEN_ID_PREFIX", "SPECIMEN_ID_FIELD", "QC-");
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule1, rule2));

        Map<String, String> extracted = new HashMap<>();
        extracted.put("SPECIMEN_ID_FIELD", "QC-123");
        extracted.put("RESULT_STATUS_FIELD", "F");

        assertTrue(service.evaluateAsQc("1", extracted));
    }

    @Test
    public void evaluateAsQc_FieldEqualsRule_ReturnsTrueOnExactMatch() {
        AstmQcRule rule = buildRule("rule-1", "FIELD_EQUALS", "RESULT_STATUS_FIELD", "Q");
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule));

        assertTrue(service.evaluateAsQc("1", Map.of("RESULT_STATUS_FIELD", "Q")));
        assertFalse(service.evaluateAsQc("1", Map.of("RESULT_STATUS_FIELD", "F")));
    }

    @Test
    public void evaluateAsQc_SpecimenPrefixRule_ReturnsTrueOnPrefixMatch() {
        AstmQcRule rule = buildRule("rule-1", "SPECIMEN_ID_PREFIX", "SPECIMEN_ID_FIELD", "QC-");
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule));

        assertTrue(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "QC-99")));
        assertFalse(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "PAT-99")));
    }

    @Test
    public void evaluateAsQc_SpecimenPatternRule_SupportsRegexAndInvalidPattern() {
        AstmQcRule valid = buildRule("rule-1", "SPECIMEN_ID_PATTERN", "SPECIMEN_ID_FIELD", "^QC-[0-9]+$");
        AstmQcRule invalid = buildRule("rule-2", "SPECIMEN_ID_PATTERN", "SPECIMEN_ID_FIELD", "*invalid[");

        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(valid));
        assertTrue(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "QC-123")));
        assertFalse(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "PAT-123")));

        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(invalid));
        assertFalse(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "QC-123")));
    }

    @Test
    public void evaluateAsQc_FieldContainsRule_ReturnsTrueWhenSubstringFound() {
        AstmQcRule rule = buildRule("rule-1", "FIELD_CONTAINS", "SENDER_FIELD", "QC");
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of(rule));

        assertTrue(service.evaluateAsQc("1", Map.of("SENDER_FIELD", "GENEXPERT-QC")));
        assertFalse(service.evaluateAsQc("1", Map.of("SENDER_FIELD", "GENEXPERT")));
    }

    @Test
    public void evaluateAsQc_NoActiveRules_ReturnsFalse() {
        when(qcRuleDAO.findActiveByAnalyzerId("1")).thenReturn(List.of());
        assertFalse(service.evaluateAsQc("1", Map.of("SPECIMEN_ID_FIELD", "QC-1")));
    }

    private AstmQcRule buildRule(String id, String type, String targetField, String operand) {
        AstmQcRule rule = new AstmQcRule();
        rule.setId(id);
        rule.setRuleType(type);
        rule.setTargetField(targetField);
        rule.setOperand(operand);
        rule.setIsActive(true);
        return rule;
    }
}
