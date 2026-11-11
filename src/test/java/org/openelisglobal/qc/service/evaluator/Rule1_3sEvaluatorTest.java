package org.openelisglobal.qc.service.evaluator;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for Rule1_3sEvaluator (T078)
 *
 * Tests the 1₃ₛ rule: Single result exceeds ±3SD from mean Reference: Westgard
 * QC rules - primary rejection rule
 */
public class Rule1_3sEvaluatorTest {

    private Rule1_3sEvaluator evaluator;
    private QCStatistics statistics;

    @Before
    public void setUp() {
        evaluator = new Rule1_3sEvaluator();

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
        assertEquals("1₃ₛ", evaluator.getRuleCode());
    }

    @Test
    public void testGetSeverity() {
        assertEquals("REJECTION", evaluator.getSeverity());
    }

    @Test
    public void testGetRequiredResultCount() {
        assertEquals(1, evaluator.getRequiredResultCount());
    }

    /**
     * Test: Result exceeds +3SD (above mean) - should VIOLATE Mean = 100, SD = 5,
     * +3SD = 115. Value = 116 exceeds threshold.
     */
    @Test
    public void testEvaluate_ResultExceedsPlus3SD_ShouldViolate() {
        QCResult result = createResult("R1", new BigDecimal("116.00")); // z = 3.2

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertEquals("REJECTION", evaluation.getSeverity());
        assertEquals(1, evaluation.getAffectedResultIds().size());
        assertTrue(evaluation.getMessage().contains("above"));
    }

    /**
     * Test: Result exceeds -3SD (below mean) - should VIOLATE Mean = 100, SD = 5,
     * -3SD = 85. Value = 84 exceeds threshold.
     */
    @Test
    public void testEvaluate_ResultExceedsMinus3SD_ShouldViolate() {
        QCResult result = createResult("R1", new BigDecimal("84.00")); // z = -3.2

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertEquals("REJECTION", evaluation.getSeverity());
        assertTrue(evaluation.getMessage().contains("below"));
    }

    /**
     * Test: Result within ±3SD - should NOT violate Mean = 100, SD = 5. Value = 110
     * is within +2SD.
     */
    @Test
    public void testEvaluate_ResultWithin3SD_ShouldNotViolate() {
        QCResult result = createResult("R1", new BigDecimal("110.00")); // z = 2.0

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate", evaluation.isViolated());
    }

    /**
     * Test: Edge case - result at exactly 3.0 SD - should VIOLATE (boundary is
     * inclusive)
     */
    @Test
    public void testEvaluate_ResultAtExactly3SD_ShouldViolate() {
        QCResult result = createResult("R1", new BigDecimal("115.00")); // z = 3.0 exactly

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate at exactly 3SD", evaluation.isViolated());
    }

    /**
     * Test: Edge case - result just under 3SD - should NOT violate
     */
    @Test
    public void testEvaluate_ResultJustUnder3SD_ShouldNotViolate() {
        QCResult result = createResult("R1", new BigDecimal("114.99")); // z = 2.998

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate just under 3SD", evaluation.isViolated());
    }

    /**
     * Test: Missing current result - cannot evaluate
     */
    @Test
    public void testEvaluate_MissingCurrentResult_CannotEvaluate() {
        RuleEvaluationResult evaluation = evaluator.evaluate(null, Collections.emptyList(), statistics);

        assertFalse("Should not be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate", evaluation.isViolated());
    }

    /**
     * Test: Missing statistics - cannot evaluate
     */
    @Test
    public void testEvaluate_MissingStatistics_CannotEvaluate() {
        QCResult result = createResult("R1", new BigDecimal("116.00"));

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), null);

        assertFalse("Should not be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate", evaluation.isViolated());
    }

    /**
     * Test with pre-calculated z-score on result
     */
    @Test
    public void testEvaluate_WithPreCalculatedZScore_ShouldUseIt() {
        QCResult result = createResult("R1", new BigDecimal("100.00"));
        result.setZScore(new BigDecimal("3.5")); // Pre-calculated z-score indicating violation

        RuleEvaluationResult evaluation = evaluator.evaluate(result, Collections.emptyList(), statistics);

        assertTrue("Should use pre-calculated z-score", evaluation.isViolated());
    }
}
