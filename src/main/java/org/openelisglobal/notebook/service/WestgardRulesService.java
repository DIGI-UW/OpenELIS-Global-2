package org.openelisglobal.notebook.service;

import java.util.List;
import java.util.Map;

/**
 * Basic Westgard Rules Engine Service for QC validation.
 *
 * This is a simplified implementation to handle immediate lab delivery needs
 * while the full OpenELIS Westgard rules engine is being developed.
 *
 * Implements the most common Westgard rules: - 1:2s (Warning: 1 control outside
 * ±2SD) - 1:3s (Rejection: 1 control outside ±3SD) - 2:2s (Rejection: 2
 * consecutive controls outside same ±2SD) - R:4s (Rejection: Range of controls
 * > 4SD) - 4:1s (Rejection: 4 consecutive controls outside same ±1SD) - 10:x
 * (Rejection: 10 consecutive controls on same side of mean)
 */
public interface WestgardRulesService {

    /**
     * QC control result with value and expected range.
     */
    record QCControl(double value, double mean, double standardDeviation, String controlLevel) {
    }

    /**
     * Westgard rule evaluation result.
     */
    record WestgardRuleResult(String ruleCode, String ruleName, String status, String message,
            List<Integer> failedControlIndices) {
    }

    /**
     * Complete Westgard evaluation results for a QC run.
     */
    record WestgardEvaluation(List<WestgardRuleResult> ruleResults, String overallStatus, String recommendation,
            int totalControls, int passedRules, int failedRules) {
    }

    /**
     * Evaluate QC controls against Westgard rules.
     *
     * @param controls List of QC control measurements
     * @return Complete Westgard evaluation with rule-by-rule results
     */
    WestgardEvaluation evaluateWestgardRules(List<QCControl> controls);

    /**
     * Extract QC controls from bioanalytical sample data.
     *
     * @param qcResults QC results data from sample analysis
     * @return List of QC controls ready for Westgard evaluation
     */
    List<QCControl> extractQCControlsFromResults(List<Map<String, Object>> qcResults);

    /**
     * Calculate control statistics (mean, SD) from historical QC data.
     *
     * This is a simplified version - in production this would use established
     * control lot statistics.
     *
     * @param controlValues Historical control values for same lot/level
     * @return Map with "mean" and "standardDeviation" keys
     */
    Map<String, Double> calculateControlStatistics(List<Double> controlValues);

    /**
     * Get Westgard rules configuration for the lab.
     *
     * @return Map of rule codes to enabled status
     */
    Map<String, Boolean> getEnabledWestgardRules();

    /**
     * Validate QC run and return simplified pass/fail status.
     *
     * @param qcResults QC results data
     * @return Simple validation result for immediate use
     */
    boolean isQCRunValid(List<Map<String, Object>> qcResults);
}