package org.openelisglobal.qc.service.evaluator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.springframework.stereotype.Component;

/**
 * Evaluator for Westgard 7ₜ rule (T096)
 *
 * Rule: Seven consecutive control results show a consistent trend (all
 * increasing or all decreasing). Severity: WARNING (indicates systematic drift)
 *
 * This rule detects gradual drifts in the analytical system before they cause
 * rejection-level violations.
 */
@Component
public class Rule7_tEvaluator extends AbstractWestgardRuleEvaluator {

    private static final String RULE_CODE = "7ₜ";
    private static final String SEVERITY = "WARNING";

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
        return 7; // Needs current + 6 previous
    }

    @Override
    public RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults,
            QCStatistics statistics) {

        if (currentResult == null || statistics == null) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE, "Missing current result or statistics");
        }

        if (!hasSufficientData(historicalResults)) {
            return RuleEvaluationResult.cannotEvaluate(RULE_CODE,
                    "Insufficient data: need at least 6 previous results");
        }

        // Collect the last 7 results (6 historical + current)
        int historySize = historicalResults.size();
        List<BigDecimal> values = new ArrayList<>();
        List<String> affectedIds = new ArrayList<>();

        for (int i = historySize - 6; i < historySize; i++) {
            QCResult result = historicalResults.get(i);
            values.add(result.getResultValue());
            affectedIds.add(result.getId());
        }
        values.add(currentResult.getResultValue());
        affectedIds.add(currentResult.getId());

        // Check for consistent increasing trend
        boolean allIncreasing = true;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).compareTo(values.get(i - 1)) <= 0) {
                allIncreasing = false;
                break;
            }
        }

        // Check for consistent decreasing trend
        boolean allDecreasing = true;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).compareTo(values.get(i - 1)) >= 0) {
                allDecreasing = false;
                break;
            }
        }

        if (allIncreasing) {
            String message = "Seven consecutive results showing increasing trend";
            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, affectedIds, message);
        }

        if (allDecreasing) {
            String message = "Seven consecutive results showing decreasing trend";
            return RuleEvaluationResult.violation(RULE_CODE, SEVERITY, affectedIds, message);
        }

        return RuleEvaluationResult.noViolation(RULE_CODE);
    }
}
