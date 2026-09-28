package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
}
