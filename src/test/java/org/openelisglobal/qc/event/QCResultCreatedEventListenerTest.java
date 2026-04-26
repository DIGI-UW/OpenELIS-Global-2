package org.openelisglobal.qc.event;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.service.QCRuleViolationService;
import org.openelisglobal.qc.service.WestgardRuleEvaluationService;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Unit tests for QCResultCreatedEventListener (T086/T109)
 *
 * Tests async event handling and violation creation via service.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCResultCreatedEventListenerTest {

    @Mock
    private WestgardRuleEvaluationService ruleEvaluationService;

    @Mock
    private QCRuleViolationService violationService;

    @InjectMocks
    private QCResultCreatedEventListener listener;

    private QCResult testResult;
    private QCResultCreatedEvent testEvent;

    @Before
    public void setUp() {
        testResult = new QCResult();
        testResult.setId("R1");
        testResult.setControlLotId("LOT1");
        testResult.setTestId(100);
        testResult.setInstrumentId(200);
        testResult.setResultValue(new BigDecimal("115.00"));
        testResult.setRunDateTime(new Timestamp(System.currentTimeMillis()));

        testEvent = new QCResultCreatedEvent(this, testResult);
    }

    // ===================== Event handling tests =====================

    @Test
    public void testHandleQCResultCreated_WithNoViolations_ShouldNotCreateViolation() {
        when(ruleEvaluationService.evaluateAllRules("R1")).thenReturn(Collections.emptyList());

        listener.handleQCResultCreated(testEvent);

        verify(ruleEvaluationService).evaluateAllRules("R1");
        verify(violationService, never()).createViolation(any(), any());
    }

    @Test
    public void testHandleQCResultCreated_WithNullResult_ShouldReturnEarly() {
        QCResultCreatedEvent nullEvent = new QCResultCreatedEvent(this, null);

        listener.handleQCResultCreated(nullEvent);

        verify(ruleEvaluationService, never()).evaluateAllRules(anyString());
        verify(violationService, never()).createViolation(any(), any());
    }

    @Test
    public void testHandleQCResultCreated_WithSingleViolation_ShouldCreateOneViolation() {
        RuleEvaluationResult violation = RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"),
                "Result exceeds 3SD");

        QCRuleViolation createdViolation = new QCRuleViolation();
        createdViolation.setId("V1");

        when(ruleEvaluationService.evaluateAllRules("R1")).thenReturn(Arrays.asList(violation));
        when(violationService.createViolation(eq(violation), eq(testResult))).thenReturn(createdViolation);

        listener.handleQCResultCreated(testEvent);

        verify(violationService).createViolation(violation, testResult);
    }

    @Test
    public void testHandleQCResultCreated_WithMultipleViolations_ShouldCreateMultipleViolations() {
        RuleEvaluationResult violation1 = RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"),
                "Result exceeds 3SD");
        RuleEvaluationResult violation2 = RuleEvaluationResult.violation("2₂ₛ", "REJECTION", Arrays.asList("R0", "R1"),
                "Two consecutive exceed 2SD");

        QCRuleViolation v1 = new QCRuleViolation();
        v1.setId("V1");
        QCRuleViolation v2 = new QCRuleViolation();
        v2.setId("V2");

        when(ruleEvaluationService.evaluateAllRules("R1")).thenReturn(Arrays.asList(violation1, violation2));
        when(violationService.createViolation(eq(violation1), eq(testResult))).thenReturn(v1);
        when(violationService.createViolation(eq(violation2), eq(testResult))).thenReturn(v2);

        listener.handleQCResultCreated(testEvent);

        verify(violationService, times(2)).createViolation(any(), eq(testResult));
    }

    @Test
    public void testHandleQCResultCreated_WithMixedResults_ShouldOnlyCreateForViolations() {
        RuleEvaluationResult violation = RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"),
                "Result exceeds 3SD");
        RuleEvaluationResult noViolation = RuleEvaluationResult.noViolation("2₂ₛ");
        RuleEvaluationResult cannotEvaluate = RuleEvaluationResult.cannotEvaluate("R₄ₛ", "Insufficient data");

        QCRuleViolation v1 = new QCRuleViolation();
        v1.setId("V1");

        when(ruleEvaluationService.evaluateAllRules("R1"))
                .thenReturn(Arrays.asList(violation, noViolation, cannotEvaluate));
        when(violationService.createViolation(eq(violation), eq(testResult))).thenReturn(v1);

        listener.handleQCResultCreated(testEvent);

        verify(violationService, times(1)).createViolation(any(), any());
    }

    @Test
    public void testHandleQCResultCreated_WithWarningViolation_ShouldCreateWarningViolation() {
        RuleEvaluationResult warning = RuleEvaluationResult.violation("1₂ₛ", "WARNING", Arrays.asList("R1"),
                "Result exceeds 2SD");

        QCRuleViolation v1 = new QCRuleViolation();
        v1.setId("V1");
        v1.setSeverity("WARNING");

        when(ruleEvaluationService.evaluateAllRules("R1")).thenReturn(Arrays.asList(warning));
        when(violationService.createViolation(eq(warning), eq(testResult))).thenReturn(v1);

        listener.handleQCResultCreated(testEvent);

        verify(violationService).createViolation(warning, testResult);
    }

    // ===================== Error handling tests =====================

    @Test
    public void testHandleQCResultCreated_WhenEvaluationThrows_ShouldNotPropagate() {
        when(ruleEvaluationService.evaluateAllRules("R1"))
                .thenThrow(new RuntimeException("Database error"));

        // Should not throw
        listener.handleQCResultCreated(testEvent);

        verify(violationService, never()).createViolation(any(), any());
    }

    @Test
    public void testHandleQCResultCreated_WhenViolationServiceReturnsNull_ShouldContinue() {
        RuleEvaluationResult violation = RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"),
                "Result exceeds 3SD");

        when(ruleEvaluationService.evaluateAllRules("R1")).thenReturn(Arrays.asList(violation));
        when(violationService.createViolation(any(), any())).thenReturn(null);

        // Should not throw
        listener.handleQCResultCreated(testEvent);

        verify(violationService).createViolation(violation, testResult);
    }

    // ===================== Event accessors tests =====================

    @Test
    public void testEventAccessors() {
        assertEquals("R1", testEvent.getResultId());
        assertEquals("LOT1", testEvent.getControlLotId());
        assertEquals(testResult, testEvent.getResult());
    }

    @Test
    public void testEventAccessors_WithNullResult() {
        QCResultCreatedEvent nullEvent = new QCResultCreatedEvent(this, null);

        assertNull(nullEvent.getResultId());
        assertNull(nullEvent.getControlLotId());
        assertNull(nullEvent.getResult());
    }
}
