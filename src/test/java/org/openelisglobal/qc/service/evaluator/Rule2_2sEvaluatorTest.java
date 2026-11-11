package org.openelisglobal.qc.service.evaluator;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for Rule2_2sEvaluator (T079)
 *
 * Tests the 2₂ₛ rule: Two consecutive results exceed same ±2SD limit Reference:
 * Westgard QC rules - detects systematic error
 */
public class Rule2_2sEvaluatorTest {

    private Rule2_2sEvaluator evaluator;
    private QCStatistics statistics;

    @Before
    public void setUp() {
        evaluator = new Rule2_2sEvaluator();

        // Standard statistics: mean = 100, SD = 5
        statistics = new QCStatistics();
        statistics.setMean(new BigDecimal("100.00"));
        statistics.setStandardDeviation(new BigDecimal("5.00"));
    }

    private QCResult createResult(String id, BigDecimal value) {
        QCResult result = new QCResult();
        result.setId(id);
        result.setResultValue(value);
        return result;
    }

    @Test
    public void testGetRuleCode() {
        assertEquals("2₂ₛ", evaluator.getRuleCode());
    }

    @Test
    public void testGetSeverity() {
        assertEquals("REJECTION", evaluator.getSeverity());
    }

    @Test
    public void testGetRequiredResultCount() {
        assertEquals(2, evaluator.getRequiredResultCount());
    }

    /**
     * Test: Two consecutive results exceed +2SD - should VIOLATE Both values above
     * +2SD (110+)
     */
    @Test
    public void testEvaluate_TwoConsecutiveAbovePlus2SD_ShouldViolate() {
        QCResult previous = createResult("R1", new BigDecimal("112.00")); // z = 2.4
        QCResult current = createResult("R2", new BigDecimal("111.00")); // z = 2.2

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertEquals("REJECTION", evaluation.getSeverity());
        assertEquals(2, evaluation.getAffectedResultIds().size());
        assertTrue(evaluation.getMessage().contains("above +2SD"));
    }

    /**
     * Test: Two consecutive results exceed -2SD - should VIOLATE Both values below
     * -2SD (90-)
     */
    @Test
    public void testEvaluate_TwoConsecutiveBelowMinus2SD_ShouldViolate() {
        QCResult previous = createResult("R1", new BigDecimal("88.00")); // z = -2.4
        QCResult current = createResult("R2", new BigDecimal("89.00")); // z = -2.2

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertTrue(evaluation.getMessage().contains("below -2SD"));
    }

    /**
     * Test: Two consecutive on OPPOSITE sides - should NOT violate One above +2SD,
     * one below -2SD
     */
    @Test
    public void testEvaluate_TwoConsecutiveOppositeSides_ShouldNotViolate() {
        QCResult previous = createResult("R1", new BigDecimal("112.00")); // z = +2.4
        QCResult current = createResult("R2", new BigDecimal("88.00")); // z = -2.4

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate - opposite sides", evaluation.isViolated());
    }

    /**
     * Test: Only one exceeds 2SD - should NOT violate
     */
    @Test
    public void testEvaluate_OnlyOneExceeds2SD_ShouldNotViolate() {
        QCResult previous = createResult("R1", new BigDecimal("108.00")); // z = 1.6 (within 2SD)
        QCResult current = createResult("R2", new BigDecimal("112.00")); // z = 2.4 (exceeds)

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate - only one exceeds", evaluation.isViolated());
    }

    /**
     * Test: Insufficient data (no previous results) - cannot evaluate
     */
    @Test
    public void testEvaluate_InsufficientData_CannotEvaluate() {
        QCResult current = createResult("R1", new BigDecimal("112.00"));

        RuleEvaluationResult evaluation = evaluator.evaluate(current, Collections.emptyList(), statistics);

        assertFalse("Should not be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate", evaluation.isViolated());
        assertTrue(evaluation.getMessage().contains("Insufficient"));
    }

    /**
     * Test: Edge case - both exactly at 2.0 SD on same side
     */
    @Test
    public void testEvaluate_BothExactlyAt2SD_ShouldViolate() {
        QCResult previous = createResult("R1", new BigDecimal("110.00")); // z = 2.0
        QCResult current = createResult("R2", new BigDecimal("110.00")); // z = 2.0

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate at exactly 2SD", evaluation.isViolated());
    }
}
