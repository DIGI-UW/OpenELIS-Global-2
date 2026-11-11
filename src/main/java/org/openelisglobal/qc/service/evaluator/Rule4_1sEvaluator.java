package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 4₁ₛ rule (T093)
 *
 * Rule: Four consecutive control results exceed the same ±1SD limit (all above
 * +1SD or all below -1SD). Severity: REJECTION (indicates systematic error)
 *
 * This rule detects smaller systematic errors (shifts/drifts) that may not
 * trigger the 2₂ₛ rule but still indicate a bias in the method.
 */
@Component
public class Rule4_1sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "4₁ₛ";
    private static final String SEVERITY = "REJECTION";
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
        return 4; // Needs current + 3 previous
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        if (!hasSufficientData(historicalResults)) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE,
                    "Insufficient data: need at least 3 previous results");
        }

        // Get z-scores for current and last 3 results
        BigDecimal currentZScore = getZScore(currentResult, statistics);

        int historySize = historicalResults.size();
        QCResult result1 = historicalResults.get(historySize - 3);
        QCResult result2 = historicalResults.get(historySize - 2);
        QCResult result3 = historicalResults.get(historySize - 1);

        BigDecimal z1 = getZScore(result1, statistics);
        BigDecimal z2 = getZScore(result2, statistics);
        BigDecimal z3 = getZScore(result3, statistics);

        // Check if all 4 exceed 1SD on the SAME side
        boolean allExceed = exceedsThreshold(z1, THRESHOLD) && exceedsThreshold(z2, THRESHOLD)
                && exceedsThreshold(z3, THRESHOLD) && exceedsThreshold(currentZScore, THRESHOLD);

        boolean allSameSide = sameSide(z1, z2) && sameSide(z2, z3) && sameSide(z3, currentZScore);

        if (allExceed && allSameSide) {
            String direction = isPositiveSide(currentZScore) ? "above +1SD" : "below -1SD";
            String message = String.format("Four consecutive results %s (z-scores: %.2f, %.2f, %.2f, %.2f)", direction,
                    z1.doubleValue(), z2.doubleValue(), z3.doubleValue(), currentZScore.doubleValue());

            List<String> affectedIds = new ArrayList<>();
            affectedIds.add(result1.getId());
            affectedIds.add(result2.getId());
            affectedIds.add(result3.getId());
            affectedIds.add(currentResult.getId());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, affectedIds, message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
