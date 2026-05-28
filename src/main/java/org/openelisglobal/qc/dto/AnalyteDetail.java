package org.openelisglobal.qc.dto;

import java.math.BigDecimal;

/**
 * Per-analyte (test) QC detail for an instrument: latest z-score and run time.
 */
public class AnalyteDetail {

    private String testId;
    private String testName;
    private BigDecimal latestZScore;
    private String lastRunTime;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public BigDecimal getLatestZScore() {
        return latestZScore;
    }

    public void setLatestZScore(BigDecimal latestZScore) {
        this.latestZScore = latestZScore;
    }

    public String getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(String lastRunTime) {
        this.lastRunTime = lastRunTime;
    }
}