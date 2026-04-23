package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard R₄ₛ rule (T092)
 *
 * Rule: The range (difference) between two consecutive control results exceeds
 * 4 standard deviations. Severity: REJECTION (indicates random error)
 *
 * This rule detects random errors where one result is high and the next is low
 * (or vice versa), with a combined spread greater than 4SD.
 */
@Component
public class RuleR_4sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "R₄ₛ";
    private static final String SEVERITY = "REJECTION";
    private static final BigDecimal THRESHOLD = new BigDecimal("4.0");

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
        return 2; // Needs current + 1 previous
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        if (!hasSufficientData(historicalResults)) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Insufficient data: need at least 1 previous result");
        }

        BigDecimal currentZScore = getZScore(currentResult, statistics);
        QCResult previousResult = historicalResults.get(historicalResults.size() - 1);
        BigDecimal previousZScore = getZScore(previousResult, statistics);

        // Calculate the range (absolute difference) between z-scores
        BigDecimal range = currentZScore.subtract(previousZScore).abs();

        if (range.compareTo(THRESHOLD) >= 0) {
            String message = String.format(
                    "Range between consecutive results exceeds 4SD (z-scores: %.2f, %.2f, range: %.2f)",
                    previousZScore.doubleValue(), currentZScore.doubleValue(), range.doubleValue());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY,
                    Arrays.asList(previousResult.getId(), currentResult.getId()), message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
