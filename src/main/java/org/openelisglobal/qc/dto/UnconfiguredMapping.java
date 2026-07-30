package org.openelisglobal.qc.dto;

/**
 * DTO representing a control-lot (test, instrument) mapping that has no
 * Westgard rule configuration. Returned by the GET
 * /rest/qc/ruleConfig/unconfigured endpoint.
 */
public class UnconfiguredMapping {

    private String testId;
    private String instrumentId;
    private String testName;
    private String instrumentName;
    private int activeControlLotCount;

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public int getActiveControlLotCount() {
        return activeControlLotCount;
    }

    public void setActiveControlLotCount(int activeControlLotCount) {
        this.activeControlLotCount = activeControlLotCount;
    }
}
