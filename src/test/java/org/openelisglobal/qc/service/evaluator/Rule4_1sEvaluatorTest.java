package org.openelisglobal.qc.service.evaluator;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;

/**
 * Unit tests for Rule4_1sEvaluator (T081)
 *
 * Tests the 4₁ₛ rule: Four consecutive results exceed same ±1SD limit
 * Reference: Westgard QC rules - detects systematic error/shift
 */
public class Rule4_1sEvaluatorTest {

    private Rule4_1sEvaluator evaluator;
    private QCStatistics statistics;

    @Before
    public void setUp() {
        evaluator = new Rule4_1sEvaluator();

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
        assertEquals("4₁ₛ", evaluator.getRuleCode());
    }

    @Test
    public void testGetSeverity() {
        assertEquals("REJECTION", evaluator.getSeverity());
    }

    @Test
    public void testGetRequiredResultCount() {
        assertEquals(4, evaluator.getRequiredResultCount());
    }

    /**
     * Test: Four consecutive results exceed +1SD - should VIOLATE
     */
    @Test
    public void testEvaluate_FourConsecutiveAbovePlus1SD_ShouldViolate() {
        // All values above +1SD (105+)
        QCResult r1 = createResult("R1", new BigDecimal("106.00")); // z = 1.2
        QCResult r2 = createResult("R2", new BigDecimal("107.00")); // z = 1.4
        QCResult r3 = createResult("R3", new BigDecimal("108.00")); // z = 1.6
        QCResult current = createResult("R4", new BigDecimal("106.00")); // z = 1.2

        List<QCResult> history = Arrays.asList(r1, r2, r3);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should be evaluated", evaluation.isEvaluated());
        assertTrue("Should violate", evaluation.isViolated());
        assertEquals("REJECTION", evaluation.getSeverity());
        assertEquals(4, evaluation.getAffectedResultIds().size());
        assertTrue(evaluation.getMessage().contains("above +1SD"));
    }

    /**
     * Test: Four consecutive results exceed -1SD - should VIOLATE
     */
    @Test
    public void testEvaluate_FourConsecutiveBelowMinus1SD_ShouldViolate() {
        // All values below -1SD (95-)
        QCResult r1 = createResult("R1", new BigDecimal("94.00")); // z = -1.2
        QCResult r2 = createResult("R2", new BigDecimal("93.00")); // z = -1.4
        QCResult r3 = createResult("R3", new BigDecimal("92.00")); // z = -1.6
        QCResult current = createResult("R4", new BigDecimal("94.00")); // z = -1.2

        List<QCResult> history = Arrays.asList(r1, r2, r3);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate", evaluation.isViolated());
        assertTrue(evaluation.getMessage().contains("below -1SD"));
    }

    /**
     * Test: Only three consecutive exceed 1SD - should NOT violate
     */
    @Test
    public void testEvaluate_OnlyThreeConsecutive_ShouldNotViolate() {
        // First result within 1SD, last three exceed
        QCResult r1 = createResult("R1", new BigDecimal("103.00")); // z = 0.6 (within 1SD)
        QCResult r2 = createResult("R2", new BigDecimal("106.00")); // z = 1.2
        QCResult r3 = createResult("R3", new BigDecimal("107.00")); // z = 1.4
        QCResult current = createResult("R4", new BigDecimal("106.00")); // z = 1.2

        List<QCResult> history = Arrays.asList(r1, r2, r3);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertFalse("Should not violate - only 3 consecutive", evaluation.isViolated());
    }

    /**
     * Test: Four exceed 1SD but on DIFFERENT sides - should NOT violate
     */
    @Test
    public void testEvaluate_FourExceedButDifferentSides_ShouldNotViolate() {
        QCResult r1 = createResult("R1", new BigDecimal("106.00")); // z = +1.2
        QCResult r2 = createResult("R2", new BigDecimal("94.00")); // z = -1.2 (different side)
        QCResult r3 = createResult("R3", new BigDecimal("107.00")); // z = +1.4
        QCResult current = createResult("R4", new BigDecimal("106.00")); // z = +1.2

        List<QCResult> history = Arrays.asList(r1, r2, r3);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertFalse("Should not violate - different sides", evaluation.isViolated());
    }

    /**
     * Test: Insufficient data - cannot evaluate
     */
    @Test
    public void testEvaluate_InsufficientData_CannotEvaluate() {
        QCResult r1 = createResult("R1", new BigDecimal("106.00"));
        QCResult r2 = createResult("R2", new BigDecimal("107.00"));
        QCResult current = createResult("R3", new BigDecimal("108.00"));

        // Only 2 historical results, need 3
        List<QCResult> history = Arrays.asList(r1, r2);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertFalse("Should not be evaluated", evaluation.isEvaluated());
        assertTrue(evaluation.getMessage().contains("Insufficient"));
    }

    /**
     * Test: Edge case - all exactly at 1.0 SD
     */
    @Test
    public void testEvaluate_AllExactlyAt1SD_ShouldViolate() {
        // All at exactly +1SD (105)
        QCResult r1 = createResult("R1", new BigDecimal("105.00")); // z = 1.0
        QCResult r2 = createResult("R2", new BigDecimal("105.00")); // z = 1.0
        QCResult r3 = createResult("R3", new BigDecimal("105.00")); // z = 1.0
        QCResult current = createResult("R4", new BigDecimal("105.00")); // z = 1.0

        List<QCResult> history = Arrays.asList(r1, r2, r3);
        RuleEvaluationResult evaluation = evaluator.evaluate(current, history, statistics);

        assertTrue("Should violate at exactly 1SD", evaluation.isViolated());
    }
}
