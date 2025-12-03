package org.openelisglobal.qc.service;

import java.util.List;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;

/**
 * Service interface for QCCorrectiveAction operations. Supports User Story 4:
 * Manage Corrective Actions.
 *
 * State machine: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED
 */
public interface QCCorrectiveActionService {

    /** Action type constants. */
    String ACTION_TYPE_RECALIBRATION = "RECALIBRATION";
    String ACTION_TYPE_MAINTENANCE = "MAINTENANCE";
    String ACTION_TYPE_REPEAT_CONTROL = "REPEAT_CONTROL";
    String ACTION_TYPE_REAGENT_CHANGE = "REAGENT_CHANGE";
    String ACTION_TYPE_OTHER = "OTHER";

    /** Status constants. */
    String STATUS_PENDING = "PENDING";
    String STATUS_ASSIGNED = "ASSIGNED";
    String STATUS_IN_PROGRESS = "IN_PROGRESS";
    String STATUS_COMPLETED = "COMPLETED";

    /**
     * Create a new corrective action for a violation.
     *
     * @param violationId     The violation this action addresses
     * @param actionType      The type of corrective action
     * @param description     Description of the action
     * @param createdByUserId User creating the action
     * @return The created corrective action
     */
    QCCorrectiveAction createAction(String violationId, String actionType, String description, Integer createdByUserId);

    /**
     * Assign a corrective action to a user. Transitions from PENDING to ASSIGNED.
     *
     * @param actionId       The action to assign
     * @param assignedUserId The user to assign to
     * @return The updated action
     * @throws IllegalStateException if action is not in PENDING status
     */
    QCCorrectiveAction assignAction(String actionId, Integer assignedUserId);

    /**
     * Start working on a corrective action. Transitions from ASSIGNED to
     * IN_PROGRESS.
     *
     * @param actionId The action to start
     * @return The updated action
     * @throws IllegalStateException if action is not in ASSIGNED status
     */
    QCCorrectiveAction startAction(String actionId);

    /**
     * Complete a corrective action. Transitions from IN_PROGRESS to COMPLETED. Also
     * resolves the linked violation.
     *
     * @param actionId          The action to complete
     * @param resolutionNotes   Notes describing how the issue was resolved
     * @param completedByUserId User completing the action
     * @return The updated action
     * @throws IllegalStateException    if action is not in IN_PROGRESS status
     * @throws IllegalArgumentException if resolutionNotes is empty
     */
    QCCorrectiveAction completeAction(String actionId, String resolutionNotes, Integer completedByUserId);

    /**
     * Get a corrective action by ID.
     */
    QCCorrectiveAction get(String id);

    /**
     * Find all corrective actions for a violation.
     */
    List<QCCorrectiveAction> findByViolation(String violationId);

    /**
     * Find all corrective actions assigned to a user.
     */
    List<QCCorrectiveAction> findByAssignedUser(Integer userId);

    /**
     * Find pending corrective actions assigned to a user.
     */
    List<QCCorrectiveAction> findPendingByAssignedUser(Integer userId);

    /**
     * Find corrective actions by status.
     */
    List<QCCorrectiveAction> findByStatus(String status);

    /**
     * Get count of incomplete actions (PENDING, ASSIGNED, IN_PROGRESS).
     */
    int getIncompleteCount();

    /**
     * Check if a violation has any incomplete corrective actions.
     */
    boolean hasIncompleteActions(String violationId);
}
