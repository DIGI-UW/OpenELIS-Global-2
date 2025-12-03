package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 3₁ₛ rule (T095)
 *
 * Rule: Three consecutive control results exceed the same ±1SD limit (all above
 * +1SD or all below -1SD). Severity: WARNING (early warning for systematic
 * error)
 *
 * This rule provides an early warning for smaller systematic errors before they
 * trigger the 4₁ₛ rejection rule.
 */
@Component
public class Rule3_1sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "3₁ₛ";
    private static final String SEVERITY = "WARNING";
    private static final BigDecimal THRESHOLD = new BigDecimal("1.0");

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
        return 3; // Needs current + 2 previous
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        if (!hasSufficientData(historicalResults)) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE,
                    "Insufficient data: need at least 2 previous results");
        }

        // Get z-scores for current and last 2 results
        BigDecimal currentZScore = getZScore(currentResult, statistics);

        int historySize = historicalResults.size();
        QCResult result1 = historicalResults.get(historySize - 2);
        QCResult result2 = historicalResults.get(historySize - 1);

        BigDecimal z1 = getZScore(result1, statistics);
        BigDecimal z2 = getZScore(result2, statistics);

        // Check if all 3 exceed 1SD on the SAME side
        boolean allExceed = exceedsThreshold(z1, THRESHOLD) && exceedsThreshold(z2, THRESHOLD)
                && exceedsThreshold(currentZScore, THRESHOLD);

        boolean allSameSide = sameSide(z1, z2) && sameSide(z2, currentZScore);

        if (allExceed && allSameSide) {
            String direction = isPositiveSide(currentZScore) ? "above +1SD" : "below -1SD";
            String message = String.format("Three consecutive results %s (z-scores: %.2f, %.2f, %.2f)", direction,
                    z1.doubleValue(), z2.doubleValue(), currentZScore.doubleValue());

            List<String> affectedIds = new ArrayList<>();
            affectedIds.add(result1.getId());
            affectedIds.add(result2.getId());
            affectedIds.add(currentResult.getId());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, affectedIds, message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
