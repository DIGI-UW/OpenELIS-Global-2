package org.openelisglobal.coldstorage.service;

import java.time.OffsetDateTime;
import java.util.List;
import org.openelisglobal.coldstorage.dao.CorrectiveActionDAO;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionType;
import org.openelisglobal.coldstorage.valueholder.Freezer;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CorrectiveActionServiceImpl extends BaseObjectServiceImpl<CorrectiveAction, Long>
        implements CorrectiveActionService {

    @Autowired
    private CorrectiveActionDAO correctiveActionDAO;

    @Autowired
    private FreezerService freezerService;

    @Autowired
    private SystemUserService systemUserService;

    public CorrectiveActionServiceImpl() {
        super(CorrectiveAction.class);
    }

    @Override
    protected CorrectiveActionDAO getBaseObjectDAO() {
        return correctiveActionDAO;
    }

    @Override
    @Transactional
    public CorrectiveAction createCorrectiveAction(Long freezerId, CorrectiveActionType actionType, String description,
            Integer createdByUserId) {

        if (freezerId == null) {
            throw new IllegalArgumentException("Freezer ID is required");
        }

        // Fetch the managed Freezer entity from database
        Freezer freezer = freezerService.findById(freezerId)
                .orElseThrow(() -> new IllegalArgumentException("Freezer not found: " + freezerId));

        SystemUser createdBy = systemUserService.get(createdByUserId.toString());
        if (createdBy == null) {
            throw new IllegalArgumentException("User not found: " + createdByUserId);
        }

        CorrectiveAction action = new CorrectiveAction();
        action.setFreezer(freezer); // Set the managed entity directly
        action.setActionType(actionType);
        action.setDescription(description);
        action.setStatus(CorrectiveActionStatus.PENDING);
        action.setCreatedAt(OffsetDateTime.now());
        action.setCreatedBy(createdBy);
        action.setIsEdited(false);

        Long id = correctiveActionDAO.insert(action);
        return correctiveActionDAO.get(id).orElse(action);
    }

    @Override
    @Transactional
    public CorrectiveAction updateCorrectiveActionStatus(Long actionId, CorrectiveActionStatus newStatus,
            Integer updatedByUserId) {
        CorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);
        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        SystemUser updatedBy = systemUserService.get(updatedByUserId.toString());
        if (updatedBy == null) {
            throw new IllegalArgumentException("User not found: " + updatedByUserId);
        }

        action.setStatus(newStatus);
        action.setUpdatedAt(OffsetDateTime.now());
        action.setUpdatedBy(updatedBy);

        correctiveActionDAO.update(action);
        return action;
    }

    @Override
    @Transactional
    public CorrectiveAction updateCorrectiveActionDescription(Long actionId, String description,
            Integer updatedByUserId) {
        CorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);
        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        SystemUser updatedBy = systemUserService.get(updatedByUserId.toString());
        if (updatedBy == null) {
            throw new IllegalArgumentException("User not found: " + updatedByUserId);
        }

        action.setDescription(description);
        action.setIsEdited(true); // Mark as edited
        action.setUpdatedAt(OffsetDateTime.now());
        action.setUpdatedBy(updatedBy);

        correctiveActionDAO.update(action);
        return action;
    }

    @Override
    @Transactional
    public CorrectiveAction completeCorrectiveAction(Long actionId, Integer completedByUserId, String completionNotes) {
        CorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);
        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        SystemUser completedBy = systemUserService.get(completedByUserId.toString());
        if (completedBy == null) {
            throw new IllegalArgumentException("User not found: " + completedByUserId);
        }

        action.setStatus(CorrectiveActionStatus.COMPLETED);
        action.setCompletedAt(OffsetDateTime.now());
        action.setCompletionNotes(completionNotes);
        action.setUpdatedAt(OffsetDateTime.now());
        action.setUpdatedBy(completedBy);

        correctiveActionDAO.update(action);
        return action;
    }

    @Override
    @Transactional
    public CorrectiveAction retractCorrectiveAction(Long actionId, Integer retractedByUserId, String retractionReason) {
        CorrectiveAction action = correctiveActionDAO.get(actionId).orElse(null);
        if (action == null) {
            throw new IllegalArgumentException("Corrective action not found: " + actionId);
        }

        SystemUser retractedBy = systemUserService.get(retractedByUserId.toString());
        if (retractedBy == null) {
            throw new IllegalArgumentException("User not found: " + retractedByUserId);
        }

        action.setStatus(CorrectiveActionStatus.RETRACTED);
        action.setRetractedAt(OffsetDateTime.now());
        action.setRetractionReason(retractionReason);
        action.setUpdatedAt(OffsetDateTime.now());
        action.setUpdatedBy(retractedBy);

        correctiveActionDAO.update(action);
        return action;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getCorrectiveActionsByStatus(CorrectiveActionStatus status) {
        return correctiveActionDAO.getActionsByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getCorrectiveActionsByFreezerId(Long freezerId) {
        return correctiveActionDAO.getActionsByFreezerId(freezerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getCorrectiveActionsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate) {
        return correctiveActionDAO.getActionsByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorrectiveAction> getAllCorrectiveActions() {
        return correctiveActionDAO.getAllActions();
    }
}
