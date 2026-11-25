package org.openelisglobal.reportqueue.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * What one version printed for one analysis: the raw stored value (the
 * dictionary id for coded results, every value of a multi-value result joined
 * in id order), the text as printed, the flag and the unit. The raw value and
 * unit are what later content is compared against to tell a changed result from
 * an added one.
 */
@Entity
@Table(name = "report_version_item", schema = "clinlims")
public class ReportVersionItem extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion version;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "printed_value")
    private String printedValue;

    @Column(name = "printed_display")
    private String printedDisplay;

    @Column(name = "printed_flag", length = 8)
    private String printedFlag;

    @Column(name = "printed_unit")
    private String printedUnit;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public ReportVersion getVersion() {
        return version;
    }

    public void setVersion(ReportVersion version) {
        this.version = version;
    }

    public Long getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getPrintedValue() {
        return printedValue;
    }

    public void setPrintedValue(String printedValue) {
        this.printedValue = printedValue;
    }

    public String getPrintedDisplay() {
        return printedDisplay;
    }

    public void setPrintedDisplay(String printedDisplay) {
        this.printedDisplay = printedDisplay;
    }

    public String getPrintedFlag() {
        return printedFlag;
    }

    public void setPrintedFlag(String printedFlag) {
        this.printedFlag = printedFlag;
    }

    public String getPrintedUnit() {
        return printedUnit;
    }

    public void setPrintedUnit(String printedUnit) {
        this.printedUnit = printedUnit;
    }
}
