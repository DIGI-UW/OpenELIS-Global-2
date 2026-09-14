package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

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

    @Column(name = "analyzer_instrument_id", precision = 10, scale = 0)
    @Convert(converter = StringToIntegerConverter.class)
    private String analyzerInstrumentId;

    @Column(name = "analyzer_card_id", length = 100)
    private String analyzerCardId;

    @Column(name = "analyzer_software_version", length = 100)
    private String analyzerSoftwareVersion;

    @Column(name = "analyzer_organism_id", length = 100)
    private String analyzerOrganismId;

    @Column(name = "analyzer_organism_name", length = 255)
    private String analyzerOrganismName;

    @Column(name = "analyzer_organism_confidence", precision = 7, scale = 3)
    private BigDecimal analyzerOrganismConfidence;

    @Column(name = "analyzer_expert_flags")
    private String analyzerExpertFlags;

    @Column(name = "instrument_qc_reference", length = 100)
    private String instrumentQcReference;

    @Column(name = "qc_state", nullable = false, length = 20)
    private String qcState = "NOT_REPORTED";

    @Column(name = "qc_override_reason")
    private String qcOverrideReason;

    @Column(name = "qc_overridden_at")
    private Timestamp qcOverriddenAt;

    @Column(name = "qc_overridden_by", length = 20)
    private String qcOverriddenBy;

    @Column(name = "analyzer_flags_acknowledged_at")
    private Timestamp analyzerFlagsAcknowledgedAt;

    @Column(name = "analyzer_flags_acknowledged_by", length = 20)
    private String analyzerFlagsAcknowledgedBy;

    @Column(name = "analyzer_flags_acknowledgement_reason")
    private String analyzerFlagsAcknowledgementReason;

    @Column(name = "analyzer_loaded_at")
    private Timestamp analyzerLoadedAt;

    @Column(name = "analyzer_completed_at")
    private Timestamp analyzerCompletedAt;

    @Column(name = "analyzer_message_codes")
    private String analyzerMessageCodes;

    @Column(name = "source_event_id", length = 100)
    private String sourceEventId;

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

    public String getAnalyzerInstrumentId() {
        return analyzerInstrumentId;
    }

    public void setAnalyzerInstrumentId(String analyzerInstrumentId) {
        this.analyzerInstrumentId = analyzerInstrumentId;
    }

    public String getAnalyzerCardId() {
        return analyzerCardId;
    }

    public void setAnalyzerCardId(String analyzerCardId) {
        this.analyzerCardId = analyzerCardId;
    }

    public String getAnalyzerSoftwareVersion() {
        return analyzerSoftwareVersion;
    }

    public void setAnalyzerSoftwareVersion(String analyzerSoftwareVersion) {
        this.analyzerSoftwareVersion = analyzerSoftwareVersion;
    }

    public String getAnalyzerOrganismId() {
        return analyzerOrganismId;
    }

    public void setAnalyzerOrganismId(String analyzerOrganismId) {
        this.analyzerOrganismId = analyzerOrganismId;
    }

    public String getAnalyzerOrganismName() {
        return analyzerOrganismName;
    }

    public void setAnalyzerOrganismName(String analyzerOrganismName) {
        this.analyzerOrganismName = analyzerOrganismName;
    }

    public BigDecimal getAnalyzerOrganismConfidence() {
        return analyzerOrganismConfidence;
    }

    public void setAnalyzerOrganismConfidence(BigDecimal analyzerOrganismConfidence) {
        this.analyzerOrganismConfidence = analyzerOrganismConfidence;
    }

    public String getAnalyzerExpertFlags() {
        return analyzerExpertFlags;
    }

    public void setAnalyzerExpertFlags(String analyzerExpertFlags) {
        this.analyzerExpertFlags = analyzerExpertFlags;
    }

    public String getInstrumentQcReference() {
        return instrumentQcReference;
    }

    public void setInstrumentQcReference(String instrumentQcReference) {
        this.instrumentQcReference = instrumentQcReference;
    }

    public String getQcState() {
        return qcState;
    }

    public void setQcState(String qcState) {
        this.qcState = qcState;
    }

    public String getQcOverrideReason() {
        return qcOverrideReason;
    }

    public void setQcOverrideReason(String qcOverrideReason) {
        this.qcOverrideReason = qcOverrideReason;
    }

    public Timestamp getQcOverriddenAt() {
        return qcOverriddenAt;
    }

    public void setQcOverriddenAt(Timestamp qcOverriddenAt) {
        this.qcOverriddenAt = qcOverriddenAt;
    }

    public String getQcOverriddenBy() {
        return qcOverriddenBy;
    }

    public void setQcOverriddenBy(String qcOverriddenBy) {
        this.qcOverriddenBy = qcOverriddenBy;
    }

    public Timestamp getAnalyzerFlagsAcknowledgedAt() {
        return analyzerFlagsAcknowledgedAt;
    }

    public void setAnalyzerFlagsAcknowledgedAt(Timestamp analyzerFlagsAcknowledgedAt) {
        this.analyzerFlagsAcknowledgedAt = analyzerFlagsAcknowledgedAt;
    }

    public String getAnalyzerFlagsAcknowledgedBy() {
        return analyzerFlagsAcknowledgedBy;
    }

    public void setAnalyzerFlagsAcknowledgedBy(String analyzerFlagsAcknowledgedBy) {
        this.analyzerFlagsAcknowledgedBy = analyzerFlagsAcknowledgedBy;
    }

    public String getAnalyzerFlagsAcknowledgementReason() {
        return analyzerFlagsAcknowledgementReason;
    }

    public void setAnalyzerFlagsAcknowledgementReason(String analyzerFlagsAcknowledgementReason) {
        this.analyzerFlagsAcknowledgementReason = analyzerFlagsAcknowledgementReason;
    }

    public Timestamp getAnalyzerLoadedAt() {
        return analyzerLoadedAt;
    }

    public void setAnalyzerLoadedAt(Timestamp analyzerLoadedAt) {
        this.analyzerLoadedAt = analyzerLoadedAt;
    }

    public Timestamp getAnalyzerCompletedAt() {
        return analyzerCompletedAt;
    }

    public void setAnalyzerCompletedAt(Timestamp analyzerCompletedAt) {
        this.analyzerCompletedAt = analyzerCompletedAt;
    }

    public String getAnalyzerMessageCodes() {
        return analyzerMessageCodes;
    }

    public void setAnalyzerMessageCodes(String analyzerMessageCodes) {
        this.analyzerMessageCodes = analyzerMessageCodes;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }
}
