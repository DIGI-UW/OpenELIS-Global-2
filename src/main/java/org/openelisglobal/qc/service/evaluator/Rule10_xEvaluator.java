package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 10ₓ rule (T094)
 *
 * Rule: Ten consecutive control results fall on the same side of the mean (all
 * above or all below the mean). Severity: REJECTION (indicates systematic
 * error/shift)
 *
 * This rule detects systematic shifts where results are consistently biased in
 * one direction, even if they don't exceed SD limits.
 */
@Component
public class Rule10_xEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "10ₓ";
    private static final String SEVERITY = "REJECTION";

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
        return 10; // Needs current + 9 previous
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        if (!hasSufficientData(historicalResults)) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE,
                    "Insufficient data: need at least 9 previous results");
        }

        // Get z-scores for current and last 9 results
        BigDecimal currentZScore = getZScore(currentResult, statistics);
        boolean currentPositive = isPositiveSide(currentZScore);

        // Check last 9 historical results
        int historySize = historicalResults.size();
        List<String> affectedIds = new ArrayList<>();
        boolean allSameSide = true;

        for (int i = historySize - 9; i < historySize; i++) {
            QCResult result = historicalResults.get(i);
            BigDecimal zScore = getZScore(result, statistics);
            boolean positive = isPositiveSide(zScore);

            if (positive != currentPositive) {
                allSameSide = false;
                break;
            }
            affectedIds.add(result.getId());
        }

        if (allSameSide) {
            affectedIds.add(currentResult.getId());
            String direction = currentPositive ? "above" : "below";
            String message = String.format("Ten consecutive results on %s side of mean", direction);

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, affectedIds, message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
