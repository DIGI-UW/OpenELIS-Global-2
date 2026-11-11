package org.openelisglobal.resultvalidation.bean;

import java.io.Serializable;

/**
 * Display-only bean carrying a failed QC sample's summary for the validation
 * screen's QC acknowledgment panel (S-08 FR-04).
 */
public class QcFailureItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String analysisId;
    private String accessionNumber;
    private String qcType;
    private String testName;
    private String resultValue;
    private String qcEvaluationDetail;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getQcType() {
        return qcType;
    }

    public void setQcType(String qcType) {
        this.qcType = qcType;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getQcEvaluationDetail() {
        return qcEvaluationDetail;
    }

    public void setQcEvaluationDetail(String qcEvaluationDetail) {
        this.qcEvaluationDetail = qcEvaluationDetail;
    }
}
