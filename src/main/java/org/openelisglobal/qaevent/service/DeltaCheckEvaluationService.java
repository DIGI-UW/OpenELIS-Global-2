package org.openelisglobal.qaevent.service;

import java.math.BigDecimal;
import java.util.List;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.result.valueholder.Result;

/**
 * Service interface for delta check evaluation logic. Handles numeric value
 * extraction, threshold comparison, and alert generation for results.
 */
public interface DeltaCheckEvaluationService {

    /**
     * Evaluate a result for delta check and create alert if threshold exceeded
     *
     * @param result the current result to evaluate
     * @return the created alert if threshold exceeded, null otherwise
     */
    DeltaCheckAlert evaluateResultForDeltaCheck(Result result);

    /**
     * Find the most recent previous result for delta check comparison
     *
     * @param result the current result
     * @return the previous result for comparison, or null if none found
     */
    Result findPreviousResultForComparison(Result result);

    /**
     * Find previous results for a patient, test, and analyte combination
     *
     * @param patient    the patient
     * @param testId     the test ID
     * @param analyteId  the analyte ID (can be null for test-level comparison)
     * @param maxResults maximum number of results to return
     * @return list of previous results ordered by date (most recent first)
     */
    List<Result> findPreviousResultsForPatient(Patient patient, Integer testId, Integer analyteId, int maxResults);

    /**
     * Calculate percentage change between two numeric values
     *
     * @param currentValue  the current value
     * @param previousValue the previous value
     * @return the percentage change (absolute value)
     */
    BigDecimal calculatePercentageChange(BigDecimal currentValue, BigDecimal previousValue);

    /**
     * Check if a result value is numeric and can be used for delta check
     *
     * @param result the result to check
     * @return true if result has numeric value, false otherwise
     */
    boolean hasNumericValue(Result result);

    /**
     * Extract numeric value from a result
     *
     * @param result the result
     * @return the numeric value, or null if not numeric
     */
    BigDecimal extractNumericValue(Result result);

    /**
     * Validate if delta check should be performed for a result
     *
     * @param result the result to validate
     * @return true if delta check should be performed, false otherwise
     */
    boolean shouldPerformDeltaCheck(Result result);

    /**
     * Get the delta check configuration for a result
     *
     * @param result the result
     * @return the threshold percentage, or null if no configuration
     */
    BigDecimal getThresholdForResult(Result result);

    /**
     * Batch evaluate multiple results for delta check
     *
     * @param results the list of results to evaluate
     * @return list of alerts created (empty list if no alerts generated)
     */
    List<DeltaCheckAlert> batchEvaluateResults(List<Result> results);

    /**
     * Re-evaluate an existing alert (for testing or manual review)
     *
     * @param alertId the ID of the alert
     * @return the re-evaluated alert with updated calculations
     */
    DeltaCheckAlert reEvaluateAlert(Integer alertId);
}
