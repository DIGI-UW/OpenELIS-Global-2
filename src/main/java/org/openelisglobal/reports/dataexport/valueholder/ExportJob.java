package org.openelisglobal.reports.dataexport.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import org.hibernate.annotations.GenericGenerator;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "reporting_export_job", uniqueConstraints = @UniqueConstraint(name = "reporting_job_submission_unique", columnNames = {
        "owner_id", "client_request_id" }))
public class ExportJob extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "reporting-uuid")
    @GenericGenerator(name = "reporting-uuid", strategy = "uuid2")
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "owner_id", nullable = false, updatable = false, length = 36)
    private String ownerId;

    @Column(name = "client_request_id", nullable = false, updatable = false, length = 80)
    private String clientRequestId;

    @Column(name = "source_id", nullable = false, updatable = false, length = 80)
    private String sourceId;

    @Column(name = "layout", nullable = false, updatable = false, length = 20)
    private String layout;

    @Column(name = "request_json", nullable = false, updatable = false, columnDefinition = "text")
    private String requestJson;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "parent_id", updatable = false, length = 36)
    private String parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private ExportJobState state = ExportJobState.QUEUED;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "worker_id", length = 36)
    private String workerId;

    @Column(name = "lease_until")
    private Instant leaseUntil;

    @Column(name = "row_count")
    private Long rowCount;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "failure_code", length = 120)
    private String failureCode;

    public ExportJob() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getLayout() {
        return layout;
    }

    public String getRequestJson() {
        return requestJson;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getParentId() {
        return parentId;
    }

    public ExportJobState getState() {
        return state;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(Instant leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public ExportJob(String ownerId, String clientRequestId, String sourceId, String layout, String requestJson,
            String requestHash, Instant submittedAt, String parentId) {
        this.ownerId = ownerId;
        this.clientRequestId = clientRequestId;
        this.sourceId = sourceId;
        this.layout = layout;
        this.requestJson = requestJson;
        this.requestHash = requestHash;
        this.submittedAt = submittedAt;
        this.parentId = parentId;
    }

    public void transitionTo(ExportJobState next) {
        if (!state.canTransitionTo(next)) {
            throw new IllegalStateException("reporting.job.invalidTransition");
        }
        state = next;
    }
}
