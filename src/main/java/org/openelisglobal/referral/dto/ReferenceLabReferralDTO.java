package org.openelisglobal.referral.dto;

import java.util.List;

public class ReferenceLabReferralDTO {

    private String id;
    private String labNumber;
    private String patientDisplay;
    private String patientGender;
    private Integer patientAge;
    private String sampleType;
    private String collectedDate;
    private List<String> tests;
    private String referenceLabId;
    private String referenceLabName;
    private String boxId;
    private String sentDate;
    private String boxReceivedDate;
    private String fhirTaskUuid;
    private String status;
    private String priority;
    private String requestor;
    private Long daysOutstanding;
    private String returnedDate;
    private String closedDate;
    private String outcome;
    private Long daysTotal;
    private String resultSummary;
    private Boolean manuallyEntered;
    // OGC-802: result cards read live from DiagnosticReport.Observation for the
    // Returned view. Null/empty for Outstanding and History.
    private List<ResultCard> results;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getPatientDisplay() {
        return patientDisplay;
    }

    public void setPatientDisplay(String patientDisplay) {
        this.patientDisplay = patientDisplay;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getCollectedDate() {
        return collectedDate;
    }

    public void setCollectedDate(String collectedDate) {
        this.collectedDate = collectedDate;
    }

    public List<String> getTests() {
        return tests;
    }

    public void setTests(List<String> tests) {
        this.tests = tests;
    }

    public String getReferenceLabId() {
        return referenceLabId;
    }

    public void setReferenceLabId(String referenceLabId) {
        this.referenceLabId = referenceLabId;
    }

    public String getReferenceLabName() {
        return referenceLabName;
    }

    public void setReferenceLabName(String referenceLabName) {
        this.referenceLabName = referenceLabName;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getSentDate() {
        return sentDate;
    }

    public void setSentDate(String sentDate) {
        this.sentDate = sentDate;
    }

    public String getBoxReceivedDate() {
        return boxReceivedDate;
    }

    public void setBoxReceivedDate(String boxReceivedDate) {
        this.boxReceivedDate = boxReceivedDate;
    }

    public String getFhirTaskUuid() {
        return fhirTaskUuid;
    }

    public void setFhirTaskUuid(String fhirTaskUuid) {
        this.fhirTaskUuid = fhirTaskUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRequestor() {
        return requestor;
    }

    public void setRequestor(String requestor) {
        this.requestor = requestor;
    }

    public Long getDaysOutstanding() {
        return daysOutstanding;
    }

    public void setDaysOutstanding(Long daysOutstanding) {
        this.daysOutstanding = daysOutstanding;
    }

    public String getReturnedDate() {
        return returnedDate;
    }

    public void setReturnedDate(String returnedDate) {
        this.returnedDate = returnedDate;
    }

    public String getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(String closedDate) {
        this.closedDate = closedDate;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public Long getDaysTotal() {
        return daysTotal;
    }

    public void setDaysTotal(Long daysTotal) {
        this.daysTotal = daysTotal;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public Boolean getManuallyEntered() {
        return manuallyEntered;
    }

    public void setManuallyEntered(Boolean manuallyEntered) {
        this.manuallyEntered = manuallyEntered;
    }

    public List<ResultCard> getResults() {
        return results;
    }

    public void setResults(List<ResultCard> results) {
        this.results = results;
    }

    /** One returned result, read from a single DiagnosticReport.Observation. */
    public static class ResultCard {
        private String testName;
        private String value;
        private String units;
        private String referenceRange;
        // Normal / Abnormal / Critical (or null when the peer sent no interpretation)
        private String interpretation;
        private String note;

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public String getReferenceRange() {
            return referenceRange;
        }

        public void setReferenceRange(String referenceRange) {
            this.referenceRange = referenceRange;
        }

        public String getInterpretation() {
            return interpretation;
        }

        public void setInterpretation(String interpretation) {
            this.interpretation = interpretation;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
