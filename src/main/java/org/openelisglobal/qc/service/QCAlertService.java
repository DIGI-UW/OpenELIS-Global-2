package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service interface for QC Alert management (T100-T102).
 *
 * Handles creation, retrieval, and management of QC alerts. Implements batching
 * logic for non-critical violations.
 */
public interface QCAlertService {

    /**
     * Create alerts for a rule violation, one per active system user. For REJECTION
     * severity, alerts are sent immediately. For WARNING severity, alerts may be
     * batched (max 1 per 15 minutes per test/instrument).
     *
     * <p>
     * TODO: Make recipient selection configurable (e.g., by role or instrument
     * assignment) instead of broadcasting to all active users.
     *
     * @param violation The rule violation to create alerts for
     * @return The created alerts, or empty list if batched/suppressed
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QCAlert> createAlertsForViolation(QCRuleViolation violation);

    /**
     * Get all alerts for a specific user.
     *
     * @param userId The user ID
     * @return List of alerts for the user
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QCAlert> getAlertsForUser(Integer userId);

    /**
     * Get unread alerts for a specific user.
     *
     * @param userId The user ID
     * @return List of unread alerts
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    List<QCAlert> getUnreadAlertsForUser(Integer userId);

    /**
     * Mark an alert as read.
     *
     * @param alertId The alert ID
     * @return The updated alert
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    QCAlert markAsRead(String alertId);

    /**
     * Mark multiple alerts as read.
     *
     * @param alertIds List of alert IDs
     * @return Number of alerts marked as read
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    int markMultipleAsRead(List<String> alertIds);

    /**
     * Get alert count for a user.
     *
     * @param userId The user ID
     * @return Count of unread alerts
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    int getUnreadAlertCount(Integer userId);

    /**
     * Check if an alert should be batched based on recent alerts. Batching applies
     * to WARNING severity only.
     *
     * @param violation The violation to check
     * @return true if the alert should be batched (suppressed)
     */
    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    boolean shouldBatchAlert(QCRuleViolation violation);
}
