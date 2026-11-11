package org.openelisglobal.reportqueue.dto;

/** One row of the queue as the page shows it. */
public class ReportQueueRowDTO {

    private Long sampleId;
    private String labNumber;
    private String type = "PATIENT";
    private String patientName;
    private String patientCode;
    private String facility;
    private String ward;
    private String requester;
    private String released;
    private int releasedCount;
    private int totalCount;
    /** PARTIAL, FINAL or AMENDED. */
    private String state;
    /** UNPRINTED or PRINTED. */
    private String printStatus;
    private boolean critical;
    private boolean awaitingValidation;
    private Integer version;
    private boolean legacy;
    /** True for a lab-number lookup that found an order with no released result. */
    private boolean nothingReleased;

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getReleased() {
        return released;
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public int getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(int releasedCount) {
        this.releasedCount = releasedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public void setPrintStatus(String printStatus) {
        this.printStatus = printStatus;
    }

    /**
     * A result on the printed version is back awaiting validation (OGC-1529): the
     * order stays printed and a print reprints that version until it is validated.
     */
    public boolean isAwaitingValidation() {
        return awaitingValidation;
    }

    public void setAwaitingValidation(boolean awaitingValidation) {
        this.awaitingValidation = awaitingValidation;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public boolean isNothingReleased() {
        return nothingReleased;
    }

    public void setNothingReleased(boolean nothingReleased) {
        this.nothingReleased = nothingReleased;
    }
}
