package org.openelisglobal.sample.form;

import java.io.Serializable;

/**
 * Form for sample search results
 */
public class SampleSearchForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String accessionNumber;
    private String sampleType;
    private String referralTest;
    private Integer analysisId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getReferralTest() {
        return referralTest;
    }

    public void setReferralTest(String referralTest) {
        this.referralTest = referralTest;
    }

    public Integer getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(Integer analysisId) {
        this.analysisId = analysisId;
    }
}
