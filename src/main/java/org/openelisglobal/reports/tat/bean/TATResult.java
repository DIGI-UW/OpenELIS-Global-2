package org.openelisglobal.reports.tat.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class TATResult {

    private String labNumber;

    /** Not populated in V1 — gated by patient-data permission in future. */
    @JsonIgnore
    private String patientName;
    private String testName;
    private String labUnit;
    private String priority;
    private String sampleType;
    private String orderingSite;
    private Timestamp orderCreated;
    private Timestamp collected;
    private Timestamp received;
    private Timestamp testingStarted;
    private Timestamp resultEntered;
    private Timestamp validated;
    private BigDecimal selectedSegmentTat;
    private BigDecimal overallTat;

    // Getters and setters
    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getLabUnit() {
        return labUnit;
    }

    public void setLabUnit(String labUnit) {
        this.labUnit = labUnit;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getOrderingSite() {
        return orderingSite;
    }

    public void setOrderingSite(String orderingSite) {
        this.orderingSite = orderingSite;
    }

    public Timestamp getOrderCreated() {
        return orderCreated;
    }

    public void setOrderCreated(Timestamp orderCreated) {
        this.orderCreated = orderCreated;
    }

    public Timestamp getCollected() {
        return collected;
    }

    public void setCollected(Timestamp collected) {
        this.collected = collected;
    }

    public Timestamp getReceived() {
        return received;
    }

    public void setReceived(Timestamp received) {
        this.received = received;
    }

    public Timestamp getTestingStarted() {
        return testingStarted;
    }

    public void setTestingStarted(Timestamp testingStarted) {
        this.testingStarted = testingStarted;
    }

    public Timestamp getResultEntered() {
        return resultEntered;
    }

    public void setResultEntered(Timestamp resultEntered) {
        this.resultEntered = resultEntered;
    }

    public Timestamp getValidated() {
        return validated;
    }

    public void setValidated(Timestamp validated) {
        this.validated = validated;
    }

    public BigDecimal getSelectedSegmentTat() {
        return selectedSegmentTat;
    }

    public void setSelectedSegmentTat(BigDecimal selectedSegmentTat) {
        this.selectedSegmentTat = selectedSegmentTat;
    }

    public BigDecimal getOverallTat() {
        return overallTat;
    }

    public void setOverallTat(BigDecimal overallTat) {
        this.overallTat = overallTat;
    }
}
