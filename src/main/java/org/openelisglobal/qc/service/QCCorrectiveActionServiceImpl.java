package org.openelisglobal.qc.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.qc.dao.QCCorrectiveActionDAO;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for QCCorrectiveAction operations. Implements state
 * machine: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED
 *
 * Following Constitution IV.5: @Transactional in services ONLY
 */
@Service
public class QCCorrectiveActionServiceImpl implements QCCorrectiveActionService {

    @Autowired
    private QCCorrectiveActionDAO correctiveActionDAO;

    @Autowired
    private QCRuleViolationService violationService;

    @Override
    @Transactional
    public QCCorrectiveAction createAction(String violationId, String actionType, String description,
            Integer createdByUserId) {
        validateActionType(actionType);

        if (violationId == null || violationId.isEmpty()) {
            throw new IllegalArgumentException("Violation ID is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (createdByUserId == null) {
            throw new IllegalArgumentException("Created by user ID is required");
        }

        QCCorrectiveAction action = new QCCorrectiveAction();
        action.setId(UUID.randomUUID().toString());
        action.setViolationId(violationId);
        action.setActionType(actionType);
        action.setDescription(description.trim());
        action.setCreatedByUserId(createdByUserId);
        action.setCreatedDateTime(Timestamp.from(Instant.now()));
        action.setStatus(STATUS_PENDING);

        String insertedId = correctiveActionDAO.insert(action);
        action.setId(insertedId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createAction",
                "Created corrective action " + insertedId + " for violation " + violationId);

        return action;
    }

    @Override
    @Transactional
    public QCCorrectiveAction assignAction(String actionId, Integer assignedUserId) {
        QCCorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);

        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }
        if (assignedUserId == null) {
            throw new IllegalArgumentException("Assigned user ID is required");
        }

        // Validate state transition
        if (!STATUS_PENDING.equals(action.getStatus())) {
            throw new IllegalStateException(
                    "Cannot assign action in status " + action.getStatus() + ". Action must be in PENDING status.");
        }

        action.setAssignedUserId(assignedUserId);
        action.setStatus(STATUS_ASSIGNED);

        correctiveActionDAO.update(action);

        LogEvent.logInfo(this.getClass().getSimpleName(), "assignAction",
                "Assigned corrective action " + actionId + " to user " + assignedUserId);

        return action;
    }

    @Override
    @Transactional
    public QCCorrectiveAction startAction(String actionId) {
        QCCorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);

        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        // Validate state transition
        if (!STATUS_ASSIGNED.equals(action.getStatus())) {
            throw new IllegalStateException(
                    "Cannot start action in status " + action.getStatus() + ". Action must be in ASSIGNED status.");
        }

        action.setStatus(STATUS_IN_PROGRESS);
        action.setStartedDateTime(Timestamp.from(Instant.now()));

        correctiveActionDAO.update(action);

        LogEvent.logInfo(this.getClass().getSimpleName(), "startAction", "Started corrective action " + actionId);

        return action;
    }

    @Override
    @Transactional
    public QCCorrectiveAction completeAction(String actionId, String resolutionNotes, Integer completedByUserId) {
        QCCorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);

        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        // Validate state transition
        if (!STATUS_IN_PROGRESS.equals(action.getStatus())) {
            throw new IllegalStateException("Cannot complete action in status " + action.getStatus()
                    + ". Action must be in IN_PROGRESS status.");
        }

        // Resolution notes required for completion
        if (resolutionNotes == null || resolutionNotes.trim().isEmpty()) {
            throw new IllegalArgumentException("Resolution notes are required to complete a corrective action");
        }

        action.setStatus(STATUS_COMPLETED);
        action.setCompletedDateTime(Timestamp.from(Instant.now()));
        action.setResolutionNotes(resolutionNotes.trim());

        correctiveActionDAO.update(action);

        // Resolve the linked violation
        try {
            violationService.resolveViolation(action.getViolationId(), completedByUserId, resolutionNotes);
            LogEvent.logInfo(this.getClass().getSimpleName(), "completeAction",
                    "Completed corrective action " + actionId + " and resolved violation " + action.getViolationId());
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "completeAction",
                    "Completed corrective action " + actionId + " but failed to resolve violation: " + e.getMessage());
        }

        return action;
    }

    @Override
    @Transactional(readOnly = true)
    public QCCorrectiveAction get(String id) {
        return correctiveActionDAO.get(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCorrectiveAction> findByViolation(String violationId) {
        return correctiveActionDAO.findByViolation(violationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCorrectiveAction> findByAssignedUser(Integer userId) {
        return correctiveActionDAO.findByAssignedUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCorrectiveAction> findPendingByAssignedUser(Integer userId) {
        return correctiveActionDAO.findPendingByAssignedUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QCCorrectiveAction> findByStatus(String status) {
        return correctiveActionDAO.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public int getIncompleteCount() {
        int count = 0;
        count += correctiveActionDAO.findByStatus(STATUS_PENDING).size();
        count += correctiveActionDAO.findByStatus(STATUS_ASSIGNED).size();
        count += correctiveActionDAO.findByStatus(STATUS_IN_PROGRESS).size();
        return count;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasIncompleteActions(String violationId) {
        List<QCCorrectiveAction> actions = correctiveActionDAO.findByViolation(violationId);
        return actions.stream().anyMatch(a -> !STATUS_COMPLETED.equals(a.getStatus()));
    }

    private void validateActionType(String actionType) {
        if (actionType == null || actionType.isEmpty()) {
            throw new IllegalArgumentException("Action type is required");
        }
        if (!ACTION_TYPE_RECALIBRATION.equals(actionType) && !ACTION_TYPE_MAINTENANCE.equals(actionType)
                && !ACTION_TYPE_REPEAT_CONTROL.equals(actionType) && !ACTION_TYPE_REAGENT_CHANGE.equals(actionType)
                && !ACTION_TYPE_OTHER.equals(actionType)) {
            throw new IllegalArgumentException("Invalid action type: " + actionType
                    + ". Must be one of: RECALIBRATION, MAINTENANCE, REPEAT_CONTROL, REAGENT_CHANGE, OTHER");
        }
    }
}
