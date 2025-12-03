package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 1₃ₛ rule (T090)
 *
 * Rule: Single control result exceeds ±3 standard deviations from the mean.
 * Severity: REJECTION (indicates serious error requiring action)
 *
 * This is the primary rejection rule. A single result beyond 3SD indicates a
 * significant analytical error with very low probability of random occurrence
 * (0.3%).
 */
@Component
public class Rule1_3sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "1₃ₛ";
    private static final String SEVERITY = "REJECTION";
    private static final BigDecimal THRESHOLD = new BigDecimal("3.0");

    @Override
    public String getRuleCode() {
        return RULE_CODE;
    }

    @Override
    public String getSeverity() {
        return SEVERITY;
    }

    @Override
    public int getRequiredResultCount() {
        return 1; // Only needs current result
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        BigDecimal zScore = getZScore(currentResult, statistics);

        if (exceedsThreshold(zScore, THRESHOLD)) {
            String direction = isPositiveSide(zScore) ? "above" : "below";
            String message = String.format("CRITICAL: Result exceeds 3SD %s mean (z-score: %.2f)", direction,
                    zScore.doubleValue());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, Collections.singletonList(currentResult.getId()),
                    message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
