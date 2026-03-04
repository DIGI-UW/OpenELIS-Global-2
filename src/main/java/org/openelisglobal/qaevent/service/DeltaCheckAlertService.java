package org.openelisglobal.qaevent.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;
import org.openelisglobal.qaevent.valueholder.NcEvent;
import org.openelisglobal.result.valueholder.Result;

/**
 * Service interface for managing delta check alerts. Provides business logic
 * for generating, managing, and resolving delta check alerts.
 */
public interface DeltaCheckAlertService extends BaseObjectService<DeltaCheckAlert, Integer> {

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
     * Get all active alerts for a specific result
     * 
     * @param resultId the ID of the result
     * @return list of active alerts
     */
    List<DeltaCheckAlert> getActiveAlertsForResult(String resultId);

    /**
     * Get all alerts for a specific result (any status)
     * 
     * @param resultId the ID of the result
     * @return list of all alerts
     */
    List<DeltaCheckAlert> getAllAlertsForResult(String resultId);

    /**
     * Dismiss an alert with reason
     * 
     * @param alertId     the ID of the alert
     * @param reason      the reason for dismissal
     * @param dismissedBy the user dismissing the alert
     * @return the updated alert
     */
    DeltaCheckAlert dismissAlert(Integer alertId, String reason, String dismissedBy);

    /**
     * Escalate an alert to create an NCE
     * 
     * @param alertId the ID of the alert
     * @param ncEvent the NCE to link to
     * @return the updated alert
     */
    DeltaCheckAlert escalateAlertToNCE(Integer alertId, NcEvent ncEvent);

    /**
     * Get alerts by status
     * 
     * @param status the alert status
     * @return list of alerts with the specified status
     */
    List<DeltaCheckAlert> getAlertsByStatus(AlertStatus status);

    /**
     * Get alerts that need attention (active alerts older than specified time)
     * 
     * @param hoursOld minimum age in hours
     * @return list of alerts needing attention
     */
    List<DeltaCheckAlert> getAlertsNeedingAttention(int hoursOld);

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
     * Batch evaluate multiple results for delta check
     * 
     * @param results the list of results to evaluate
     * @return list of alerts created (empty list if no alerts generated)
     */
    List<DeltaCheckAlert> batchEvaluateResults(List<Result> results);

    /**
     * Get alert statistics for reporting
     * 
     * @param daysSince number of days to analyze
     * @return map containing alert statistics
     */
    Map<String, Object> getAlertStatistics(int daysSince);

    /**
     * Get recent alerts
     * 
     * @param daysSince number of days to look back
     * @return list of recent alerts
     */
    List<DeltaCheckAlert> getRecentAlerts(int daysSince);

    /**
     * Clean up old resolved alerts (for maintenance)
     * 
     * @param daysSinceResolution minimum age in days for resolved alerts
     * @return number of alerts deleted
     */
    int cleanupOldResolvedAlerts(int daysSinceResolution);

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
     * Re-evaluate an existing alert (for testing or manual review)
     * 
     * @param alertId the ID of the alert
     * @return the re-evaluated alert with updated calculations
     */
    DeltaCheckAlert reEvaluateAlert(Integer alertId);

    /**
     * Bulk dismiss multiple alerts
     * 
     * @param alertIds    list of alert IDs to dismiss
     * @param reason      the reason for dismissal
     * @param dismissedBy the user dismissing the alerts
     * @return number of alerts dismissed
     */
    int bulkDismissAlerts(List<Integer> alertIds, String reason, String dismissedBy);

    /**
     * Get alerts filtered by status and/or analysis IDs. Performs all lazy entity
     * traversal within the transactional service boundary.
     * 
     * @param status      optional alert status filter (null for default: recent
     *                    active alerts)
     * @param analysisIds optional list of analysis IDs to filter by (null or empty
     *                    for no filter)
     * @param recentDays  number of days to look back when no status filter is
     *                    provided
     * @return filtered list of alerts
     */
    List<DeltaCheckAlert> getFilteredAlerts(AlertStatus status, List<String> analysisIds, int recentDays);
}