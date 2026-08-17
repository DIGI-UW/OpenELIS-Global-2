package org.openelisglobal.qaevent.criticalcallback.bean;

import java.sql.Timestamp;

/**
 * One released critical result in the compliance window, with its latest
 * callback attempt if any. Null callback fields (status, recipientName,
 * loggedAt, loggedBy) mean no callback was ever logged — the actionable gap.
 */
public class CallbackEvent {

    private String analysisId;
    private String labNumber;
    private String testName;
    /** Value as communicated on the call when logged, else the saved value. */
    private String resultValue;
    private String criticalRange;
    private Timestamp releasedAt;
    private String recipientName;
    private String status;
    private Timestamp loggedAt;
    private String loggedBy;
    /**
     * Minutes from release to the latest callback attempt; negative means the call
     * was made before release (compliant). Null when never logged.
     */
    private Long minutesToCallback;

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

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getCriticalRange() {
        return criticalRange;
    }

    public void setCriticalRange(String criticalRange) {
        this.criticalRange = criticalRange;
    }

    public Timestamp getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Timestamp releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Timestamp loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getLoggedBy() {
        return loggedBy;
    }

    public void setLoggedBy(String loggedBy) {
        this.loggedBy = loggedBy;
    }

    public Long getMinutesToCallback() {
        return minutesToCallback;
    }

    public void setMinutesToCallback(Long minutesToCallback) {
        this.minutesToCallback = minutesToCallback;
    }
}
