package org.openelisglobal.qc.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for QCCorrectiveAction REST API requests. Supports User Story 4: Manage
 * Corrective Actions.
 */
public class QCCorrectiveActionForm {

    private String id;

    @NotBlank(message = "Violation ID is required")
    private String violationId;

    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotBlank(message = "Description is required")
    private String description;

    private Integer assignedUserId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getViolationId() {
        return violationId;
    }

    public void setViolationId(String violationId) {
        this.violationId = violationId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Integer assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    /** DTO for assigning a corrective action. */
    public static class AssignRequest {

        @NotNull(message = "Assigned user ID is required")
        private Integer assignedUserId;

        public Integer getAssignedUserId() {
            return assignedUserId;
        }

        public void setAssignedUserId(Integer assignedUserId) {
            this.assignedUserId = assignedUserId;
        }
    }

    /** DTO for completing a corrective action. */
    public static class CompleteRequest {

        @NotBlank(message = "Resolution notes are required")
        private String resolutionNotes;

        public String getResolutionNotes() {
            return resolutionNotes;
        }

        public void setResolutionNotes(String resolutionNotes) {
            this.resolutionNotes = resolutionNotes;
        }
    }
}
