package org.openelisglobal.qc.service.evaluator;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCStatistics;
import org.openelisglobal.qc.valueholder.WestgardRuleConfig;

/**
 * Interface for Westgard rule evaluators (T087)
 *
 * Each implementation evaluates a specific Westgard rule (1₂ₛ, 1₃ₛ, 2₂ₛ, R₄ₛ,
 * 4₁ₛ, 10ₓ, 3₁ₛ, 7ₜ) against QC results.
 */
public interface WestgardRuleEvaluator {

    /**
     * Get the rule code this evaluator handles.
     *
     * @return Rule code (e.g., "1₃ₛ", "2₂ₛ")
     */
    String getRuleCode();

    /**
     * Check if this evaluator can evaluate the given rule configuration.
     *
     * @param config The rule configuration to check
     * @return true if this evaluator handles the specified rule and it's enabled
     */
    boolean canEvaluate(WestgardRuleConfig config);

    /**
     * Get the minimum number of historical results required for evaluation.
     *
     * @return Minimum number of results needed (e.g., 2 for 2₂ₛ, 10 for 10ₓ)
     */
    int getRequiredResultCount();

    /**
     * Evaluate the rule against the current result and historical results.
     *
     * @param currentResult     The new QC result being evaluated
     * @param historicalResults Previous results in chronological order (oldest
     *                          first)
     * @param statistics        Current statistics (mean, SD) for the control lot
     * @return Evaluation result indicating violation status
     */
    RuleEvaluationResult evaluate(QCResult currentResult, List<QCResult> historicalResults, QCStatistics statistics);

    /**
     * Get the severity level for violations of this rule.
     *
     * @return "WARNING" or "REJECTION"
     */
    String getSeverity();
}
