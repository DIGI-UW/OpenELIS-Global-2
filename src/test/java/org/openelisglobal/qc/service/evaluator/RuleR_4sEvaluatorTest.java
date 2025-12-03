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
 * Unit tests for RuleR_4sEvaluator (T080)
 *
 * Tests the R₄ₛ rule: Range between consecutive results exceeds 4SD Reference:
 * Westgard QC rules - detects random error
 */
public class RuleR_4sEvaluatorTest {

    private RuleR_4sEvaluator evaluator;
    private QCStatistics statistics;

    @Before
    public void setUp() {
        evaluator = new RuleR_4sEvaluator();

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
        assertEquals("R₄ₛ", evaluator.getRuleCode());
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
     * Test: Range exceeds 4SD - should VIOLATE Previous z = +2.5, Current z = -2.0,
     * Range = 4.5
     */
    @Test
    public void testEvaluate_RangeExceeds4SD_ShouldViolate() {
        // Previous: 112.5 (z = +2.5), Current: 90 (z = -2.0), Range = 4.5 SD
        QCResult previous = createResult("R1", new BigDecimal("112.50"));
        QCResult current = createResult("R2", new BigDecimal("90.00"));

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertEquals("REJECTION", evaluation.getSeverity());
        assertEquals(2, evaluation.getAffectedResultIds().size());
        assertTrue(evaluation.getMessage().contains("4SD"));
    }

    /**
     * Test: Range less than 4SD - should NOT violate
     */
    @Test
    public void testEvaluate_RangeLessThan4SD_ShouldNotViolate() {
        // Previous: 108 (z = +1.6), Current: 92 (z = -1.6), Range = 3.2 SD
        QCResult previous = createResult("R1", new BigDecimal("108.00"));
        QCResult current = createResult("R2", new BigDecimal("92.00"));

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertFalse("Should not violate - range < 4SD", evaluation.isViolated());
    }

    /**
     * Test: Edge case - range exactly 4.0 SD - should VIOLATE Previous z = +2.0,
     * Current z = -2.0, Range = 4.0
     */
    @Test
    public void testEvaluate_RangeExactly4SD_ShouldViolate() {
        // Previous: 110 (z = +2.0), Current: 90 (z = -2.0), Range = 4.0 SD
        QCResult previous = createResult("R1", new BigDecimal("110.00"));
        QCResult current = createResult("R2", new BigDecimal("90.00"));

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate at exactly 4SD", evaluation.isViolated());
    }

    /**
     * Test: Insufficient data - cannot evaluate
     */
    @Test
    public void testEvaluate_InsufficientData_CannotEvaluate() {
        QCResult current = createResult("R1", new BigDecimal("115.00"));

        RuleEvaluationResult evaluation = evaluator.evaluate(current, Collections.emptyList(), statistics);

        assertFalse("Should not be evaluated", evaluation.isEvaluated());
        assertTrue(evaluation.getMessage().contains("Insufficient"));
    }

    /**
     * Test: Large swing from high to low - should VIOLATE
     */
    @Test
    public void testEvaluate_LargeSwingHighToLow_ShouldViolate() {
        // Previous: 115 (z = +3.0), Current: 85 (z = -3.0), Range = 6.0 SD
        QCResult previous = createResult("R1", new BigDecimal("115.00"));
        QCResult current = createResult("R2", new BigDecimal("85.00"));

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate - large swing", evaluation.isViolated());
        assertTrue(evaluation.getMessage().contains("6.0"));
    }

    /**
     * Test: Both on same side but far apart - can still violate
     */
    @Test
    public void testEvaluate_SameSideButFarApart_ShouldViolate() {
        // Previous: 120 (z = +4.0), Current: 100 (z = 0), Range = 4.0 SD
        QCResult previous = createResult("R1", new BigDecimal("120.00"));
        QCResult current = createResult("R2", new BigDecimal("100.00"));

        List<QCResult> history = Arrays.asList(previous);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate - range is 4SD", evaluation.isViolated());
    }
}
