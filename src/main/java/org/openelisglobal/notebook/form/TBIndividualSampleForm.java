package org.openelisglobal.notebook.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for registering an individual TB sample. Contains the same data
 * points as the TB manifest (FR-014) but as direct field values instead of CSV
 * column mappings.
 */
public class TBIndividualSampleForm {

    // A. Sample Identity
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleId;

    // B. Specimen Information
    @NotBlank(message = "Specimen type is required")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenQuality;

    @Min(value = 1, message = "Number of samples must be at least 1")
    private int numOfSamples = 1;

    // C. Request Paper Details
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String documentNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String referringFacility;

    // D. Patient / Participant Metadata
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientAge;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientSex;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientAddress;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientPhone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String physicianPhone;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String consentStatus;

    // E. Clinical Context
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String treatmentHistory;

    // F. Requested Tests (Yes/No values)
    private boolean culture;
    private boolean smearMicroscopy;
    private boolean genexpert;
    private boolean identification;
    private boolean dstFirstLine;
    private boolean dstSecondLine;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String intendedMethod;

    // G. Receipt Details
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedSite;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedTime;

    // Getters and Setters

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(String specimenType) {
        this.specimenType = specimenType;
    }

    public String getSpecimenQuality() {
        return specimenQuality;
    }

    public void setSpecimenQuality(String specimenQuality) {
        this.specimenQuality = specimenQuality;
    }

    public int getNumOfSamples() {
        return numOfSamples;
    }

    public void setNumOfSamples(int numOfSamples) {
        this.numOfSamples = numOfSamples;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(String patientAge) {
        this.patientAge = patientAge;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getStudyId() {
        return studyId;
    }

    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPhysicianPhone() {
        return physicianPhone;
    }

    public void setPhysicianPhone(String physicianPhone) {
        this.physicianPhone = physicianPhone;
    }

    public String getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(String consentStatus) {
        this.consentStatus = consentStatus;
    }

    public String getTreatmentHistory() {
        return treatmentHistory;
    }

    public void setTreatmentHistory(String treatmentHistory) {
        this.treatmentHistory = treatmentHistory;
    }

    public boolean isCulture() {
        return culture;
    }

    public void setCulture(boolean culture) {
        this.culture = culture;
    }

    public boolean isSmearMicroscopy() {
        return smearMicroscopy;
    }

    public void setSmearMicroscopy(boolean smearMicroscopy) {
        this.smearMicroscopy = smearMicroscopy;
    }

    public boolean isGenexpert() {
        return genexpert;
    }

    public void setGenexpert(boolean genexpert) {
        this.genexpert = genexpert;
    }

    public boolean isIdentification() {
        return identification;
    }

    public void setIdentification(boolean identification) {
        this.identification = identification;
    }

    public boolean isDstFirstLine() {
        return dstFirstLine;
    }

    public void setDstFirstLine(boolean dstFirstLine) {
        this.dstFirstLine = dstFirstLine;
    }

    public boolean isDstSecondLine() {
        return dstSecondLine;
    }

    public void setDstSecondLine(boolean dstSecondLine) {
        this.dstSecondLine = dstSecondLine;
    }

    public String getIntendedMethod() {
        return intendedMethod;
    }

    public void setIntendedMethod(String intendedMethod) {
        this.intendedMethod = intendedMethod;
    }

    public String getReceivedSite() {
        return receivedSite;
    }

    public void setReceivedSite(String receivedSite) {
        this.receivedSite = receivedSite;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getReceivedTime() {
        return receivedTime;
    }

    public void setReceivedTime(String receivedTime) {
        this.receivedTime = receivedTime;
    }

    /**
     * Convert this form to a TBManifestRow for reuse of the sample creation
     * service.
     */
    public org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow toManifestRow() {
        return new org.openelisglobal.notebook.service.TBManifestImportService.TBManifestRow(
                1, // rowNumber (single row)
                sampleId, specimenType, specimenQuality, documentNumber, referringFacility, patientName, patientAge,
                patientSex, patientId, studyId, patientAddress, patientPhone, physicianPhone, consentStatus,
                treatmentHistory, culture ? "Yes" : "No", smearMicroscopy ? "Yes" : "No", genexpert ? "Yes" : "No",
                identification ? "Yes" : "No", dstFirstLine ? "Yes" : "No", dstSecondLine ? "Yes" : "No",
                intendedMethod, receivedSite, receivedDate, receivedTime, numOfSamples);
    }
}
