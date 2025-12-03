package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Service interface for QC Alert management (T100-T102).
 *
 * Handles creation, retrieval, and management of QC alerts. Implements batching
 * logic for non-critical violations.
 */
public interface QCAlertService {

    /**
     * Create an alert for a rule violation. For REJECTION severity, alerts are sent
     * immediately. For WARNING severity, alerts may be batched (max 1 per 15
     * minutes per test/instrument).
     *
     * @param violation The rule violation to create an alert for
     * @return The created alert, or null if batched/suppressed
     */
    QCAlert createAlertForViolation(QCRuleViolation violation);

    /**
     * Get all alerts for a specific user.
     *
     * @param userId The user ID
     * @return List of alerts for the user
     */
    List<QCAlert> getAlertsForUser(Integer userId);

    /**
     * Get unread alerts for a specific user.
     *
     * @param userId The user ID
     * @return List of unread alerts
     */
    List<QCAlert> getUnreadAlertsForUser(Integer userId);

    /**
     * Mark an alert as read.
     *
     * @param alertId The alert ID
     * @return The updated alert
     */
    QCAlert markAsRead(String alertId);

    /**
     * Mark multiple alerts as read.
     *
     * @param alertIds List of alert IDs
     * @return Number of alerts marked as read
     */
    int markMultipleAsRead(List<String> alertIds);

    /**
     * Get alert count for a user.
     *
     * @param userId The user ID
     * @return Count of unread alerts
     */
    int getUnreadAlertCount(Integer userId);

    /**
     * Check if an alert should be batched based on recent alerts. Batching applies
     * to WARNING severity only.
     *
     * @param violation The violation to check
     * @return true if the alert should be batched (suppressed)
     */
    boolean shouldBatchAlert(QCRuleViolation violation);
}
