package org.openelisglobal.qc.valueholder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * QCCorrectiveAction represents a corrective action taken to resolve a QC
 * violation.
 */
@Entity
@Table(name = "qc_corrective_action")
public class QCCorrectiveAction extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @NotNull
    @Column(name = "violation_id", nullable = false, length = 36)
    private String violationId;

    @NotNull
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @NotNull
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_user_id")
    private Integer assignedUserId;

    @NotNull
    @Column(name = "created_by_user_id", nullable = false)
    private Integer createdByUserId;

    @NotNull
    @Column(name = "created_date_time", nullable = false)
    private Timestamp createdDateTime;

    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "started_date_time")
    private Timestamp startedDateTime;

    @Column(name = "completed_date_time")
    private Timestamp completedDateTime;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    public QCCorrectiveAction() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
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

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Timestamp getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(Timestamp createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStartedDateTime() {
        return startedDateTime;
    }

    public void setStartedDateTime(Timestamp startedDateTime) {
        this.startedDateTime = startedDateTime;
    }

    public Timestamp getCompletedDateTime() {
        return completedDateTime;
    }

    public void setCompletedDateTime(Timestamp completedDateTime) {
        this.completedDateTime = completedDateTime;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
