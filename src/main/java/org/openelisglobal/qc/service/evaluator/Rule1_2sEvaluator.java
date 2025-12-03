package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 1₂ₛ rule (T089)
 *
 * Rule: Single control result exceeds ±2 standard deviations from the mean.
 * Severity: WARNING (early warning indicator, not rejection)
 *
 * This rule provides an early warning when a single QC result falls outside the
 * 2SD limits. It has a 4.6% false rejection rate when used alone.
 */
@Component
public class Rule1_2sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "1₂ₛ";
    private static final String SEVERITY = "WARNING";
    private static final BigDecimal THRESHOLD = new BigDecimal("2.0");

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
            String message = String.format("Result exceeds 2SD %s mean (z-score: %.2f)", direction,
                    zScore.doubleValue());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, Collections.singletonList(currentResult.getId()),
                    message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
