package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 2₂ₛ rule (T091)
 *
 * Rule: Two consecutive control results exceed the same ±2SD limit (both above
 * +2SD or both below -2SD). Severity: REJECTION (indicates systematic error)
 *
 * This rule detects systematic errors (shifts) where the method is consistently
 * biased in one direction.
 */
@Component
public class Rule2_2sEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "2₂ₛ";
    private static final String SEVERITY = "REJECTION";
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

        // Check if both exceed 2SD on the SAME side
        boolean currentExceeds = exceedsThreshold(currentZScore, THRESHOLD);
        boolean previousExceeds = exceedsThreshold(previousZScore, THRESHOLD);

        if (currentExceeds && previousExceeds && sameSide(currentZScore, previousZScore)) {
            String direction = isPositiveSide(currentZScore) ? "above +2SD" : "below -2SD";
            String message = String.format("Two consecutive results %s (z-scores: %.2f, %.2f)", direction,
                    previousZScore.doubleValue(), currentZScore.doubleValue());

            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY,
                    Arrays.asList(previousResult.getId(), currentResult.getId()), message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
