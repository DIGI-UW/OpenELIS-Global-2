package org.openelisglobal.analyzer.service;

import java.util.Date;

/**
 * Value object representing parsed QC result data from ASTM Q-segment
 * 
 * 
 * This class holds extracted QC data from ASTM LIS2-A2 Q-segments including:
 * instrument ID (from H-segment header), test code, control lot number, control
 * level, result value, unit, and timestamp.
 */
public class QCSegmentData {

    private String instrumentId;
    private String testCode;
    private String controlLotNumber;
    private String controlLevel; // L (Low), N (Normal), H (High)
    private String resultValue; // Numeric or qualitative
    private String unit; // Unit of measure (may be empty for qualitative)
    private Date timestamp; // Parsed timestamp from Q-segment

    /**
     * Default constructor
     */
    public QCSegmentData() {
    }

    /**
     * Constructor with all fields
     * 
     * @param instrumentId     Instrument ID extracted from H-segment header
     * @param testCode         Test code from Q-segment
     * @param controlLotNumber Control lot number
     * @param controlLevel     Control level (L, N, H)
     * @param resultValue      Result value (numeric or qualitative)
     * @param unit             Unit of measure (may be empty for qualitative)
     * @param timestamp        Parsed timestamp
     */
    public QCSegmentData(String instrumentId, String testCode, String controlLotNumber, String controlLevel,
            String resultValue, String unit, Date timestamp) {
        this.instrumentId = instrumentId;
        this.testCode = testCode;
        this.controlLotNumber = controlLotNumber;
        this.controlLevel = controlLevel;
        this.resultValue = resultValue;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    // Getters and setters

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

    public String getControlLotNumber() {
        return controlLotNumber;
    }

    public void setControlLotNumber(String controlLotNumber) {
        this.controlLotNumber = controlLotNumber;
    }

    public String getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(String controlLevel) {
        this.controlLevel = controlLevel;
    }

    public String getResultValue() {
        return resultValue;
    }

    public void setResultValue(String resultValue) {
        this.resultValue = resultValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "QCSegmentData{" + "instrumentId='" + instrumentId + '\'' + ", testCode='" + testCode + '\''
                + ", controlLotNumber='" + controlLotNumber + '\'' + ", controlLevel='" + controlLevel + '\''
                + ", resultValue='" + resultValue + '\'' + ", unit='" + unit + '\'' + ", timestamp=" + timestamp + '}';
    }
}
