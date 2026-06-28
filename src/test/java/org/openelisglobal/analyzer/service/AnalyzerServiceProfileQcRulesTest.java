package org.openelisglobal.analyzer.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.dao.AnalyzerDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule;
import org.openelisglobal.analyzer.valueholder.AnalyzerQcRule.RuleType;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;

/**
 * Tests that autoCreateTestMappings() extracts qcRules from profile
 * configDefaults and creates analyzer_qc_rule rows via AnalyzerQcRuleService.
 */
@RunWith(MockitoJUnitRunner.class)
public class AnalyzerServiceProfileQcRulesTest {

    @Mock
    private AnalyzerDAO analyzerDAO;

    @Mock
    private AnalyzerPluginConfigService analyzerPluginConfigService;

    @Mock
    private AnalyzerQcRuleService analyzerQcRuleService;

    @Mock
    private AnalyzerTestMappingService analyzerMappingService;

    @InjectMocks
    private AnalyzerServiceImpl analyzerService;

    @Test
    public void testAutoCreateTestMappings_WithQcRulesInProfile_CreatesDbRows() {
        // Simulate profile config with 2 QC rules
        Map<String, Object> rule1 = Map.of("ruleType", "FIELD_EQUALS", "targetField", "O.12", "operand", "Q",
                "isActive", true, "sortOrder", 1);
        Map<String, Object> rule2 = Map.of("ruleType", "SPECIMEN_ID_PREFIX", "operand", "QC-", "isActive", true,
                "sortOrder", 2);
        Map<String, Object> configDefaults = new HashMap<>();
        configDefaults.put("qcRules", List.of(rule1, rule2));
        Map<String, Object> config = new HashMap<>();
        config.put("configDefaults", configDefaults);

        // Mock dependencies
        Analyzer analyzer = new Analyzer();
        analyzer.setId("1");
        when(analyzerQcRuleService.createRule(anyString(), any(AnalyzerQcRule.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(1));

        // Act
        analyzerService.autoCreateTestMappings("1", config, "1");

        // Verify createRule was called twice
        ArgumentCaptor<AnalyzerQcRule> ruleCaptor = ArgumentCaptor.forClass(AnalyzerQcRule.class);
        verify(analyzerQcRuleService, times(2)).createRule(eq("1"), ruleCaptor.capture(), eq("1"));

        List<AnalyzerQcRule> capturedRules = ruleCaptor.getAllValues();

        // Verify first rule: FIELD_EQUALS O.12 = Q
        assertEquals(RuleType.FIELD_EQUALS, capturedRules.get(0).getRuleType());
        assertEquals("O.12", capturedRules.get(0).getTargetField());
        assertEquals("Q", capturedRules.get(0).getOperand());
        assertEquals(true, capturedRules.get(0).isActive());
        assertEquals(1, capturedRules.get(0).getDisplayOrder());

        // Verify second rule: SPECIMEN_ID_PREFIX QC-
        assertEquals(RuleType.SPECIMEN_ID_PREFIX, capturedRules.get(1).getRuleType());
        assertNull(capturedRules.get(1).getTargetField());
        assertEquals("QC-", capturedRules.get(1).getOperand());
        assertEquals(true, capturedRules.get(1).isActive());
        assertEquals(2, capturedRules.get(1).getDisplayOrder());
    }

    @Test
    public void testAutoCreateTestMappings_WithEmptyQcRules_CreatesNoRows() {
        Map<String, Object> configDefaults = new HashMap<>();
        configDefaults.put("qcRules", List.of());
        Map<String, Object> config = new HashMap<>();
        config.put("configDefaults", configDefaults);

        analyzerService.autoCreateTestMappings("1", config, "1");

        verify(analyzerQcRuleService, never()).createRule(anyString(), any(), anyString());
    }

    @Test
    public void testAutoCreateTestMappings_AppliesConfigDefaultsExactlyOnce() {
        Map<String, Object> configDefaults = new HashMap<>();
        configDefaults.put("aggregationMode", "BY_SESSION");
        configDefaults.put("aggregationWindowSeconds", 60);
        Map<String, Object> config = new HashMap<>();
        config.put("configDefaults", configDefaults);

        analyzerService.autoCreateTestMappings("1", config, "1");

        verify(analyzerPluginConfigService, times(1)).applyConfigDefaults("1", configDefaults, "1");
    }

    @Test
    public void testAutoCreateTestMappings_WithNoConfigDefaults_CreatesNoRows() {
        Map<String, Object> config = new HashMap<>();
        // no configDefaults key at all

        analyzerService.autoCreateTestMappings("1", config, "1");

        verify(analyzerQcRuleService, never()).createRule(anyString(), any(), anyString());
    }

    @Test
    public void testAutoCreateTestMappings_WithNullRuleType_SkipsRule() {
        Map<String, Object> badRule = new HashMap<>();
        badRule.put("ruleType", null);
        badRule.put("operand", "test");
        Map<String, Object> configDefaults = new HashMap<>();
        configDefaults.put("qcRules", List.of(badRule));
        Map<String, Object> config = new HashMap<>();
        config.put("configDefaults", configDefaults);

        analyzerService.autoCreateTestMappings("1", config, "1");

        verify(analyzerQcRuleService, never()).createRule(anyString(), any(), anyString());
    }
}
