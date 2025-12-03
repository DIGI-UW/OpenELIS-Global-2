package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCStatisticsDAO;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.service.evaluator.WestgardRuleEvaluator;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Unit tests for WestgardRuleEvaluationService (T085)
 *
 * Tests the orchestration of Westgard rule evaluation.
 */
@RunWith(MockitoJUnitRunner.class)
public class WestgardRuleEvaluationServiceTest {

    @Mock
    private QCResultDAO resultDAO;

    @Mock
    private QCStatisticsDAO statisticsDAO;

    @Mock
    private WestgardRuleConfigService ruleConfigService;

    @Mock
    private WestgardRuleEvaluator mockEvaluator1;

    @Mock
    private WestgardRuleEvaluator mockEvaluator2;

    private WestgardRuleEvaluationServiceImpl service;

    private QCResult currentResult;
    private QCStatistics statistics;
    private List<WestgardRuleEvaluator> evaluators;

    @Before
    public void setUp() {
        // Create a current result
        currentResult = new QCResult();
        currentResult.setId("R1");
        currentResult.setControlLotId("LOT1");
        currentResult.setTestId(100);
        currentResult.setInstrumentId(200);
        currentResult.setResultValue(new BigDecimal("112.00"));
        currentResult.setRunDateTime(new Timestamp(System.currentTimeMillis()));

        // Create statistics
        statistics = new QCStatistics();
        statistics.setId("S1");
        statistics.setControlLotId("LOT1");
        statistics.setMean(new BigDecimal("100.00"));
        statistics.setStandardDeviation(new BigDecimal("5.00"));

        // Set up evaluators list
        evaluators = new ArrayList<>();
        evaluators.add(mockEvaluator1);
        evaluators.add(mockEvaluator2);

        // Inject the evaluators list
        service = new WestgardRuleEvaluationServiceImpl();
        setPrivateField(service, "resultDAO", resultDAO);
        setPrivateField(service, "statisticsDAO", statisticsDAO);
        setPrivateField(service, "ruleConfigService", ruleConfigService);
        setPrivateField(service, "evaluators", evaluators);
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    private WestgardRuleConfig createRuleConfig(String ruleCode, boolean enabled) {
        WestgardRuleConfig config = new WestgardRuleConfig();
        config.setRuleCode(ruleCode);
        config.setEnabled(enabled);
        config.setTestId(100);
        config.setInstrumentId(200);
        return config;
    }

    // ===================== evaluateAllRules(String resultId) tests
    // =====================

    @Test
    public void testEvaluateAllRules_ByResultId_NotFound_ShouldReturnEmptyList() {
        when(resultDAO.get("UNKNOWN")).thenReturn(Optional.empty());

        List<RuleEvaluationResult> results = service.evaluateAllRules("UNKNOWN");

        assertTrue("Should return empty list for unknown result", results.isEmpty());
    }

    @Test
    public void testEvaluateAllRules_ByResultId_NoEnabledRules_ShouldReturnEmptyList() {
        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Collections.emptyList());

        List<RuleEvaluationResult> results = service.evaluateAllRules("R1");

        assertTrue("Should return empty list when no enabled rules", results.isEmpty());
    }

    @Test
    public void testEvaluateAllRules_ByResultId_NoStatistics_ShouldReturnEmptyList() {
        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(null);

        List<RuleEvaluationResult> results = service.evaluateAllRules("R1");

        assertTrue("Should return empty list when no statistics", results.isEmpty());
    }

    // ===================== evaluateAllRules with objects tests
    // =====================

    @Test
    public void testEvaluateAllRules_NullCurrentResult_ShouldReturnEmptyList() {
        List<RuleEvaluationResult> results = service.evaluateAllRules(null, Collections.emptyList(), 100, 200);

        assertTrue("Should return empty list for null current result", results.isEmpty());
    }

    @Test
    public void testEvaluateAllRules_WithEnabledRules_ShouldEvaluateAll() {
        WestgardRuleConfig config1 = createRuleConfig("1₃ₛ", true);
        WestgardRuleConfig config2 = createRuleConfig("2₂ₛ", true);

        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config1, config2));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        // Mock evaluator 1 - matches 1₃ₛ rule
        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config1)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"), "Violation"));

        // Mock evaluator 2 - matches 2₂ₛ rule
        when(mockEvaluator2.getRuleCode()).thenReturn("2₂ₛ");
        when(mockEvaluator2.canEvaluate(config2)).thenReturn(true);
        when(mockEvaluator2.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.noViolation("2₂ₛ"));

        List<RuleEvaluationResult> results = service.evaluateAllRules(currentResult, Collections.emptyList(), 100, 200);

        assertEquals("Should return 2 results", 2, results.size());
        assertTrue("First result should be violated", results.get(0).isViolated());
        assertFalse("Second result should not be violated", results.get(1).isViolated());
    }

    @Test
    public void testEvaluateAllRules_EvaluatorNotFound_ShouldSkipRule() {
        WestgardRuleConfig config = createRuleConfig("UNKNOWN_RULE", true);

        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        // No evaluator matches the UNKNOWN_RULE
        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator2.getRuleCode()).thenReturn("2₂ₛ");

        List<RuleEvaluationResult> results = service.evaluateAllRules(currentResult, Collections.emptyList(), 100, 200);

        assertTrue("Should return empty list when no evaluator found", results.isEmpty());
    }

    @Test
    public void testEvaluateAllRules_EvaluatorCannotEvaluate_ShouldSkipRule() {
        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(false); // Cannot evaluate

        List<RuleEvaluationResult> results = service.evaluateAllRules(currentResult, Collections.emptyList(), 100, 200);

        assertTrue("Should return empty list when evaluator cannot evaluate", results.isEmpty());
        verify(mockEvaluator1, never()).evaluate(any(), anyList(), any());
    }

    @Test
    public void testEvaluateAllRules_EvaluatorThrowsException_ShouldReturnCannotEvaluateResult() {
        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(true);
        when(mockEvaluator1.evaluate(any(), anyList(), any())).thenThrow(new RuntimeException("Test error"));

        List<RuleEvaluationResult> results = service.evaluateAllRules(currentResult, Collections.emptyList(), 100, 200);

        assertEquals("Should return 1 result", 1, results.size());
        assertFalse("Should not be evaluated (error)", results.get(0).isEvaluated());
        assertTrue("Message should contain error", results.get(0).getMessage().contains("Test error"));
    }

    // ===================== getViolations tests =====================

    @Test
    public void testGetViolations_ShouldReturnOnlyViolations() {
        WestgardRuleConfig config1 = createRuleConfig("1₃ₛ", true);
        WestgardRuleConfig config2 = createRuleConfig("2₂ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config1, config2));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config1)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"), "Violation"));

        when(mockEvaluator2.getRuleCode()).thenReturn("2₂ₛ");
        when(mockEvaluator2.canEvaluate(config2)).thenReturn(true);
        when(mockEvaluator2.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.noViolation("2₂ₛ"));

        List<RuleEvaluationResult> violations = service.getViolations("R1");

        assertEquals("Should return 1 violation", 1, violations.size());
        assertEquals("1₃ₛ", violations.get(0).getRuleCode());
    }

    @Test
    public void testGetViolations_NoViolations_ShouldReturnEmptyList() {
        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.noViolation("1₃ₛ"));

        List<RuleEvaluationResult> violations = service.getViolations("R1");

        assertTrue("Should return empty list when no violations", violations.isEmpty());
    }

    // ===================== hasRejectionViolation tests =====================

    @Test
    public void testHasRejectionViolation_WithRejection_ShouldReturnTrue() {
        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"), "Violation"));

        assertTrue("Should return true when rejection violation exists", service.hasRejectionViolation("R1"));
    }

    @Test
    public void testHasRejectionViolation_WithWarningOnly_ShouldReturnFalse() {
        WestgardRuleConfig config = createRuleConfig("1₂ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₂ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.violation("1₂ₛ", "WARNING", Arrays.asList("R1"), "Warning"));

        assertFalse("Should return false when only warning violations exist", service.hasRejectionViolation("R1"));
    }

    @Test
    public void testHasRejectionViolation_NoViolations_ShouldReturnFalse() {
        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Collections.emptyList());
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Collections.emptyList());

        assertFalse("Should return false when no violations", service.hasRejectionViolation("R1"));
    }

    // ===================== Historical results handling =====================

    @Test
    public void testEvaluateAllRules_WithHistoricalResults_ShouldPassToEvaluator() {
        // Create historical results
        QCResult r1 = new QCResult();
        r1.setId("R0");
        r1.setRunDateTime(new Timestamp(System.currentTimeMillis() - 10000));

        WestgardRuleConfig config = createRuleConfig("1₃ₛ", true);

        when(resultDAO.get("R1")).thenReturn(Optional.of(currentResult));
        when(resultDAO.findByControlLotIdOrderByRunDateTime("LOT1")).thenReturn(Arrays.asList(r1, currentResult));
        when(ruleConfigService.findEnabledByTestAndInstrument(100, 200)).thenReturn(Arrays.asList(config));
        when(statisticsDAO.findLatestByControlLot("LOT1")).thenReturn(statistics);

        when(mockEvaluator1.getRuleCode()).thenReturn("1₃ₛ");
        when(mockEvaluator1.canEvaluate(config)).thenReturn(true);
        when(mockEvaluator1.evaluate(eq(currentResult), anyList(), eq(statistics)))
                .thenReturn(RuleEvaluationResult.noViolation("1₃ₛ"));

        service.evaluateAllRules("R1");

        // Verify the evaluator was called with historical results (excluding current)
        verify(mockEvaluator1).evaluate(eq(currentResult),
                argThat(list -> list.size() == 1 && list.get(0).getId().equals("R0")), eq(statistics));
    }
}
