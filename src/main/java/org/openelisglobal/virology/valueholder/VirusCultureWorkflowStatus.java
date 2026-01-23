package org.openelisglobal.virology.valueholder;

import java.sql.Timestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity to track progress through all 9 process steps of the virus culture workflow
 */
@Entity
@Table(name = "virus_culture_workflow_status")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureWorkflowStatus extends BaseObject<Integer> {

    public enum StepStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        SKIPPED,
        ON_HOLD
    }

    public enum QualityCheckResult {
        NOT_APPLICABLE,
        PENDING,
        PASSED,
        FAILED,
        CONDITIONAL_PASS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_workflow_status_seq")
    @SequenceGenerator(name = "virus_culture_workflow_status_seq", sequenceName = "virus_culture_workflow_status_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "step_name", nullable = false, length = 50)
    private String stepName;

    @NotNull
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepStatus status = StepStatus.PENDING;

    @Column(name = "started_date")
    private Timestamp startedDate;

    @Column(name = "completed_date")
    private Timestamp completedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    @JsonIgnore
    private SystemUser assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    @JsonIgnore
    private SystemUser completedBy;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_check_result")
    private QualityCheckResult qualityCheckResult = QualityCheckResult.NOT_APPLICABLE;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_date")
    private Timestamp createdDate;

    // Constructors
    public VirusCultureWorkflowStatus() {
        super();
    }

    public VirusCultureWorkflowStatus(VirusCultureBatch cultureBatch, String stepName, Integer stepOrder) {
        this();
        this.cultureBatch = cultureBatch;
        this.stepName = stepName;
        this.stepOrder = stepOrder;
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Calculate duration if step is completed
        if (status == StepStatus.COMPLETED && startedDate != null && completedDate != null) {
            long durationMs = completedDate.getTime() - startedDate.getTime();
            durationMinutes = (int) (durationMs / (1000 * 60));
        }
    }

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public VirusCultureBatch getCultureBatch() {
        return cultureBatch;
    }

    public void setCultureBatch(VirusCultureBatch cultureBatch) {
        this.cultureBatch = cultureBatch;
    }

    // Convenience method to get culture batch ID
    @Transient
    public Integer getCultureBatchId() {
        return cultureBatch != null ? cultureBatch.getId() : null;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public Timestamp getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(Timestamp startedDate) {
        this.startedDate = startedDate;
    }

    public Timestamp getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(Timestamp completedDate) {
        this.completedDate = completedDate;
    }

    public SystemUser getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(SystemUser assignedTo) {
        this.assignedTo = assignedTo;
    }

    public SystemUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(SystemUser completedBy) {
        this.completedBy = completedBy;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public QualityCheckResult getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(QualityCheckResult qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    // Utility methods
    public boolean isPending() {
        return status == StepStatus.PENDING;
    }

    public boolean isInProgress() {
        return status == StepStatus.IN_PROGRESS;
    }

    public boolean isCompleted() {
        return status == StepStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == StepStatus.FAILED;
    }

    public boolean isSkipped() {
        return status == StepStatus.SKIPPED;
    }

    public boolean isOnHold() {
        return status == StepStatus.ON_HOLD;
    }

    public boolean hasQualityIssue() {
        return qualityCheckResult == QualityCheckResult.FAILED ||
               qualityCheckResult == QualityCheckResult.CONDITIONAL_PASS;
    }

    /**
     * Starts this workflow step
     */
    public void start(SystemUser user) {
        this.status = StepStatus.IN_PROGRESS;
        this.startedDate = new Timestamp(System.currentTimeMillis());
        this.assignedTo = user;
    }

    /**
     * Completes this workflow step
     */
    public void complete(SystemUser user, QualityCheckResult qcResult) {
        this.status = StepStatus.COMPLETED;
        this.completedDate = new Timestamp(System.currentTimeMillis());
        this.completedBy = user;
        this.qualityCheckResult = qcResult != null ? qcResult : QualityCheckResult.NOT_APPLICABLE;
    }

    /**
     * Fails this workflow step
     */
    public void fail(SystemUser user, String failureReason) {
        this.status = StepStatus.FAILED;
        this.completedDate = new Timestamp(System.currentTimeMillis());
        this.completedBy = user;
        this.qualityCheckResult = QualityCheckResult.FAILED;
        this.notes = failureReason;
    }

    /**
     * Puts this workflow step on hold
     */
    public void putOnHold(String reason) {
        this.status = StepStatus.ON_HOLD;
        this.notes = reason;
    }

    @Override
    public String toString() {
        return "VirusCultureWorkflowStatus{" +
                "id=" + id +
                ", stepName='" + stepName + '\'' +
                ", stepOrder=" + stepOrder +
                ", status=" + status +
                ", qualityCheckResult=" + qualityCheckResult +
                '}';
    }
}