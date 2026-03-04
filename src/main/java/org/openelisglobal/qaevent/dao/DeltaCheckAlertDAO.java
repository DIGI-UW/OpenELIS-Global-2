package org.openelisglobal.qaevent.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert.AlertStatus;

/**
 * DAO interface for managing delta check alerts. Handles database operations
 * for alert generation, tracking, and management.
 */
public interface DeltaCheckAlertDAO extends BaseDAO<DeltaCheckAlert, Integer> {

    /**
     * Find all active alerts for a specific result
     * 
     * @param resultId the ID of the result
     * @return list of active alerts for the result
     */
    List<DeltaCheckAlert> getActiveAlertsForResult(String resultId);

    /**
     * Find all alerts for a specific result (any status)
     * 
     * @param resultId the ID of the result
     * @return list of all alerts for the result
     */
    List<DeltaCheckAlert> getAllAlertsForResult(String resultId);

    /**
     * Find alerts by status
     * 
     * @param status the alert status to search for
     * @return list of alerts with the specified status
     */
    List<DeltaCheckAlert> getAlertsByStatus(AlertStatus status);

    /**
     * Find alerts dismissed by a specific user
     * 
     * @param dismissedBy the user who dismissed the alerts
     * @return list of alerts dismissed by the user
     */
    List<DeltaCheckAlert> getAlertsDismissedBy(String dismissedBy);

    /**
     * Find alerts that have been escalated to NCEs
     * 
     * @return list of escalated alerts
     */
    List<DeltaCheckAlert> getEscalatedAlerts();

    /**
     * Find recent alerts within a time period
     * 
     * @param daysSince number of days to look back
     * @return list of recent alerts
     */
    List<DeltaCheckAlert> getRecentAlerts(int daysSince);

    /**
     * Find alerts with change percentage above a threshold
     * 
     * @param thresholdPercent minimum change percentage
     * @return list of alerts above the threshold
     */
    List<DeltaCheckAlert> getAlertsAboveThreshold(double thresholdPercent);

    /**
     * Count active alerts for a specific result
     * 
     * @param resultId the ID of the result
     * @return count of active alerts
     */
    int countActiveAlertsForResult(String resultId);

    /**
     * Count all alerts by status
     * 
     * @param status the alert status
     * @return count of alerts with the specified status
     */
    int countAlertsByStatus(AlertStatus status);

    /**
     * Check if an alert already exists for a result comparison
     * 
     * @param resultId         the current result ID
     * @param previousResultId the previous result ID
     * @return true if alert exists, false otherwise
     */
    boolean alertExistsForResultComparison(String resultId, String previousResultId);

    /**
     * Find alerts for a specific analysis (test)
     * 
     * @param analysisId the ID of the analysis
     * @param status     optional status filter (null for all statuses)
     * @return list of alerts for the analysis
     */
    List<DeltaCheckAlert> getAlertsForAnalysis(String analysisId, AlertStatus status);

    /**
     * Get alerts that need attention (active alerts older than specified hours)
     * 
     * @param hoursOld minimum age in hours
     * @return list of alerts needing attention
     */
    List<DeltaCheckAlert> getAlertsNeedingAttention(int hoursOld);

    /**
     * Delete old dismissed/escalated alerts (cleanup)
     * 
     * @param daysSinceResolution minimum age in days for resolved alerts
     * @return number of alerts deleted
     */
    int deleteOldResolvedAlerts(int daysSinceResolution);

    /**
     * Count alerts since a cutoff date, optionally filtered by status
     *
     * @param cutoffDate the cutoff timestamp
     * @param status     optional status filter (null for all statuses)
     * @return count of matching alerts
     */
    long countAlertsSince(java.sql.Timestamp cutoffDate, AlertStatus status);

    /**
     * Get the average change percentage for alerts since a cutoff date
     *
     * @param cutoffDate the cutoff timestamp
     * @return the average change percentage, or 0.0 if no alerts
     */
    double getAverageChangePercentSince(java.sql.Timestamp cutoffDate);

    /**
     * Find alerts for multiple analyses
     *
     * @param analysisIds list of analysis IDs
     * @param status      optional status filter (null for all statuses)
     * @return list of alerts for the analyses
     */
    java.util.List<DeltaCheckAlert> getAlertsForAnalyses(java.util.List<String> analysisIds, AlertStatus status);
}
