package org.openelisglobal.analyzer.service;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Data Transfer Object for QC Result data extracted from ASTM Q-segments
 * 
 * 
 * This DTO contains the data structure needed to call Feature 003's
 * QCResultService.createQCResult() method. It holds mapped OpenELIS entity IDs
 * and converted values.
 */
public class QCResultDTO {

    private String analyzerId;
    private String testId;
    private String controlLotId;
    private ControlLevel controlLevel;
    private BigDecimal resultValue;
    private String unit;
    private Date timestamp;

    /**
     * Control Level enum matching Feature 003's ControlLevel enum (LOW/NORMAL/HIGH)
     */
    public enum ControlLevel {
        LOW, NORMAL, HIGH
    }

    /**
     * Default constructor
     */
    public QCResultDTO() {
    }

    /**
     * Constructor with all fields
     * 
     * @param analyzerId   Analyzer ID (from analyzer configuration)
     * @param testId       Test ID (mapped from ASTM test code)
     * @param controlLotId Control lot ID (mapped from ASTM control lot number)
     * @param controlLevel Control level enum (mapped from ASTM control level L/N/H)
     * @param resultValue  Result value (numeric, converted if unit conversion
     *                     applied)
     * @param unit         Unit of measure (converted if unit mapping applied)
     * @param timestamp    Run date/time (from Q-segment)
     */
    public QCResultDTO(String analyzerId, String testId, String controlLotId, ControlLevel controlLevel,
            BigDecimal resultValue, String unit, Date timestamp) {
        this.analyzerId = analyzerId;
        this.testId = testId;
        this.controlLotId = controlLotId;
        this.controlLevel = controlLevel;
        this.resultValue = resultValue;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    // Getters and setters

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getControlLotId() {
        return controlLotId;
    }

    public void setControlLotId(String controlLotId) {
        this.controlLotId = controlLotId;
    }

    public ControlLevel getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(ControlLevel controlLevel) {
        this.controlLevel = controlLevel;
    }

    public BigDecimal getResultValue() {
        return resultValue;
    }

    public void setResultValue(BigDecimal resultValue) {
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
        return "QCResultDTO{" + "analyzerId='" + analyzerId + '\'' + ", testId='" + testId + '\'' + ", controlLotId='"
                + controlLotId + '\'' + ", controlLevel=" + controlLevel + ", resultValue=" + resultValue + ", unit='"
                + unit + '\'' + ", timestamp=" + timestamp + '}';
    }
}
