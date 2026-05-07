package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;

/**
 * Service interface for Westgard Rule Evaluation (T097)
 *
 * Orchestrates evaluation of all enabled Westgard rules against QC results.
 */
public interface WestgardRuleEvaluationService {

    /**
     * Evaluate all enabled rules for a QC result.
     *
     * @param resultId ID of the QC result to evaluate
     * @return List of evaluation results for each enabled rule
     */
    List<RuleEvaluationResult> evaluateAllRules(String resultId);

    /**
     * Evaluate all enabled rules for a QC result with provided context.
     *
     * @param currentResult     The current QC result
     * @param historicalResults Previous results for multi-result rules
     * @param testId            Test ID for rule configuration lookup
     * @param instrumentId      Instrument ID for rule configuration lookup
     * @return List of evaluation results
     */
    List<RuleEvaluationResult> evaluateAllRules(QCResult currentResult, List<QCResult> historicalResults,
            Integer testId, Integer instrumentId);

    /**
     * Get only the violations from an evaluation.
     *
     * @param resultId ID of the QC result to evaluate
     * @return List of rule violations (excluding passes and cannot-evaluate)
     */
    List<RuleEvaluationResult> getViolations(String resultId);

    /**
     * Check if any rejection-level rules were violated.
     *
     * @param resultId ID of the QC result to evaluate
     * @return true if any REJECTION severity violations occurred
     */
    boolean hasRejectionViolation(String resultId);
}
