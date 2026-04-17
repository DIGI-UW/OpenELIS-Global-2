package org.openelisglobal.coldstorage.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionType;
import org.openelisglobal.common.service.BaseObjectService;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CorrectiveActionService extends BaseObjectService<CorrectiveAction, Long> {

    /**
     * Create a new corrective action for a device (freezer).
     *
     * @param freezerId       Device (freezer) ID
     * @param actionType      Type of action (TEMPERATURE_ADJUSTMENT,
     *                        EQUIPMENT_REPAIR, etc.)
     * @param description     Human-readable description of the action
     * @param createdByUserId User ID who created the action
     * @return Created corrective action
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    CorrectiveAction createCorrectiveAction(Long freezerId, CorrectiveActionType actionType, String description,
            Integer createdByUserId);

    /**
     * Update corrective action status.
     *
     * @param actionId        Action ID
     * @param newStatus       New status (PENDING, IN_PROGRESS, COMPLETED,
     *                        CANCELLED, RETRACTED)
     * @param updatedByUserId User ID who updated the action
     * @return Updated corrective action
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    CorrectiveAction updateCorrectiveActionStatus(Long actionId, CorrectiveActionStatus newStatus,
            Integer updatedByUserId);

    /**
     * Update corrective action description (marks as edited).
     *
     * @param actionId        Action ID
     * @param description     New description
     * @param updatedByUserId User ID who updated the action
     * @return Updated corrective action
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    CorrectiveAction updateCorrectiveActionDescription(Long actionId, String description, Integer updatedByUserId);

    /**
     * Complete a corrective action.
     *
     * @param actionId          Action ID
     * @param completedByUserId User ID who completed the action
     * @param completionNotes   Notes describing the completion
     * @return Updated corrective action
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    CorrectiveAction completeCorrectiveAction(Long actionId, Integer completedByUserId, String completionNotes);

    /**
     * Retract a corrective action.
     *
     * @param actionId          Action ID
     * @param retractedByUserId User ID who retracted the action
     * @param retractionReason  Reason for retraction
     * @return Updated corrective action
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_MANAGE')")
    CorrectiveAction retractCorrectiveAction(Long actionId, Integer retractedByUserId, String retractionReason);

    /**
     * Get all corrective actions by status.
     *
     * @param status Status filter
     * @return List of corrective actions
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<CorrectiveAction> getCorrectiveActionsByStatus(CorrectiveActionStatus status);

    /**
     * Get all corrective actions for a device.
     *
     * @param freezerId Device (freezer) ID
     * @return List of corrective actions
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<CorrectiveAction> getCorrectiveActionsByFreezerId(Long freezerId);

    /**
     * Get all corrective actions within a date range.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of corrective actions
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<CorrectiveAction> getCorrectiveActionsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate);

    /**
     * Get all corrective actions (for dashboard).
     *
     * @return List of all corrective actions
     */
    @PreAuthorize("hasAuthority('PRIV_COLDSTORAGE_VIEW')")
    List<CorrectiveAction> getAllCorrectiveActions();
}
