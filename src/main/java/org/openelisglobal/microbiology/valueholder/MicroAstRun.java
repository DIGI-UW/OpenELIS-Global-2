package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_ast_run", schema = "clinlims")
public class MicroAstRun extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "isolate_id", nullable = false, length = 36)
    private String isolateId;

    @Column(name = "panel_id", length = 36)
    private String panelId;

    @Column(name = "panel_version")
    private Integer panelVersion;

    @Column(name = "panel_provenance", length = 40)
    private String panelProvenance;

    @Column(name = "panel_adjustment_reason")
    private String panelAdjustmentReason;

    @Column(name = "breakpoint_standard_id", length = 36)
    private String breakpointStandardId;

    @Column(name = "breakpoint_version", length = 50)
    private String breakpointVersion;

    @Column(name = "amendment_id", length = 36)
    private String amendmentId;

    @Column(name = "attempt_type", nullable = false, length = 20)
    private String attemptType = MicroAstAttemptType.ORIGINAL.name();

    @Column(name = "source_run_id", length = 36)
    private String sourceRunId;

    @Column(name = "attempt_reason")
    private String attemptReason;

    @Column(name = "method", length = 20)
    private String method;

    @Column(name = "technique", nullable = false, length = 40)
    private String technique = MicroAstTechnique.LEGACY_UNSPECIFIED_MIC.name();

    @Column(name = "reportable", nullable = false)
    private boolean reportable;

    @Column(name = "status", nullable = false, length = 40)
    private String status = MicroAstRunStatus.IN_PROGRESS.name();

    @Column(name = "started_at", nullable = false)
    private Timestamp startedAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "started_by", length = 20)
    private String startedBy;

    @Column(name = "reviewed_at")
    private Timestamp reviewedAt;

    @Column(name = "reviewed_by", length = 20)
    private String reviewedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getIsolateId() {
        return isolateId;
    }

    public void setIsolateId(String isolateId) {
        this.isolateId = isolateId;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public Integer getPanelVersion() {
        return panelVersion;
    }

    public void setPanelVersion(Integer panelVersion) {
        this.panelVersion = panelVersion;
    }

    public String getPanelProvenance() {
        return panelProvenance;
    }

    public void setPanelProvenance(String panelProvenance) {
        this.panelProvenance = panelProvenance;
    }

    public String getPanelAdjustmentReason() {
        return panelAdjustmentReason;
    }

    public void setPanelAdjustmentReason(String panelAdjustmentReason) {
        this.panelAdjustmentReason = panelAdjustmentReason;
    }

    public String getBreakpointStandardId() {
        return breakpointStandardId;
    }

    public void setBreakpointStandardId(String breakpointStandardId) {
        this.breakpointStandardId = breakpointStandardId;
    }

    public String getBreakpointVersion() {
        return breakpointVersion;
    }

    public void setBreakpointVersion(String breakpointVersion) {
        this.breakpointVersion = breakpointVersion;
    }

    public String getAmendmentId() {
        return amendmentId;
    }

    public void setAmendmentId(String amendmentId) {
        this.amendmentId = amendmentId;
    }

    public String getAttemptType() {
        return attemptType;
    }

    public void setAttemptType(String attemptType) {
        this.attemptType = attemptType;
    }

    public String getSourceRunId() {
        return sourceRunId;
    }

    public void setSourceRunId(String sourceRunId) {
        this.sourceRunId = sourceRunId;
    }

    public String getAttemptReason() {
        return attemptReason;
    }

    public void setAttemptReason(String attemptReason) {
        this.attemptReason = attemptReason;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTechnique() {
        return technique;
    }

    public void setTechnique(String technique) {
        this.technique = technique;
    }

    public boolean isReportable() {
        return reportable;
    }

    public void setReportable(boolean reportable) {
        this.reportable = reportable;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
