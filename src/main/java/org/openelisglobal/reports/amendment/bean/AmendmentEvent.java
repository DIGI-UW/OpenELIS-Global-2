package org.openelisglobal.reports.amendment.bean;

import java.sql.Timestamp;

public class AmendmentEvent {

    private String analysisId;
    private String labNumber;
    private String testName;
    private String priorValue;
    private String currentValue;
    private String amendedBy;
    private Timestamp amendedAt;
    private Timestamp releasedAt;
    /**
     * Minutes between release and amendment; null when not derivable (e.g.
     * re-released since).
     */
    private Long minutesToAmend;

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getPriorValue() {
        return priorValue;
    }

    public void setPriorValue(String priorValue) {
        this.priorValue = priorValue;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getAmendedBy() {
        return amendedBy;
    }

    public void setAmendedBy(String amendedBy) {
        this.amendedBy = amendedBy;
    }

    public Timestamp getAmendedAt() {
        return amendedAt;
    }

    public void setAmendedAt(Timestamp amendedAt) {
        this.amendedAt = amendedAt;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
    }

    public Long getMinutesToAmend() {
        return minutesToAmend;
    }

    public void setMinutesToAmend(Long minutesToAmend) {
        this.minutesToAmend = minutesToAmend;
    }
}
