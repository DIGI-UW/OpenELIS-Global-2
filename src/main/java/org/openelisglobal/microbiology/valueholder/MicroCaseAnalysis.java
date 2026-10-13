package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Links one routed OpenELIS analysis to its microbiology case. The link
 * snapshots the configured reportable analyte so later configuration changes
 * cannot silently alter an in-flight case's patient-report target.
 */
@Entity
@Table(name = "micro_case_analysis", schema = "clinlims")
public class MicroCaseAnalysis extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "analysis_id", nullable = false, precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analysisId;

    @Column(name = "reportable_test_analyte_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String reportableTestAnalyteId;

    @Column(name = "projected_result_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String projectedResultId;

    @Column(name = "case_role")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private MicroCaseRole caseRole;

    @Column(name = "collected_in_sets")
    private Boolean collectedInSets;

    @Column(name = "placement")
    private String placement;

    @Column(name = "cancelled_at")
    private Timestamp cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getReportableTestAnalyteId() {
        return reportableTestAnalyteId;
    }

    public void setReportableTestAnalyteId(String reportableTestAnalyteId) {
        this.reportableTestAnalyteId = reportableTestAnalyteId;
    }

    public String getProjectedResultId() {
        return projectedResultId;
    }

    public void setProjectedResultId(String projectedResultId) {
        this.projectedResultId = projectedResultId;
    }

    public MicroCaseRole getCaseRole() {
        return caseRole;
    }

    public void setCaseRole(MicroCaseRole caseRole) {
        this.caseRole = caseRole;
    }

    public Boolean getCollectedInSets() {
        return collectedInSets;
    }

    public void setCollectedInSets(Boolean collectedInSets) {
        this.collectedInSets = collectedInSets;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Timestamp getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Timestamp cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
