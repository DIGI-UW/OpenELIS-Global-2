package org.openelisglobal.reportqueue.dao;

import java.time.OffsetDateTime;

/**
 * One order as the queue query returns it, before the per-row detail is added.
 */
public class ReportQueueRowRecord {

    private Long sampleId;
    private String accessionNumber;
    private int releasedCount;
    private int totalCount;
    private OffsetDateTime latestRelease;
    private Long versionId;
    private Integer versionNumber;
    private boolean legacy;
    private boolean unprinted;
    private boolean amended;
    private boolean partial;
    private boolean awaitingValidation;
    private String patientId;
    private String patientLastName;
    private String patientFirstName;
    private String patientNationalId;
    private String facilityName;
    private String wardName;
    private String requesterName;

    public String getState() {
        if (amended) {
            return "AMENDED";
        }
        return partial ? "PARTIAL" : "FINAL";
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
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

    public OffsetDateTime getLatestRelease() {
        return latestRelease;
    }

    public void setLatestRelease(OffsetDateTime latestRelease) {
        this.latestRelease = latestRelease;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public boolean isUnprinted() {
        return unprinted;
    }

    public void setUnprinted(boolean unprinted) {
        this.unprinted = unprinted;
    }

    public boolean isAmended() {
        return amended;
    }

    public void setAmended(boolean amended) {
        this.amended = amended;
    }

    /** A printed result is back awaiting validation; the order holds as printed. */
    public boolean isAwaitingValidation() {
        return awaitingValidation;
    }

    public void setAwaitingValidation(boolean awaitingValidation) {
        this.awaitingValidation = awaitingValidation;
    }

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientNationalId() {
        return patientNationalId;
    }

    public void setPatientNationalId(String patientNationalId) {
        this.patientNationalId = patientNationalId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getWardName() {
        return wardName;
    }

    public void setWardName(String wardName) {
        this.wardName = wardName;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }
}
