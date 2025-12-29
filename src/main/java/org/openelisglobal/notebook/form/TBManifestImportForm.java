package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for TB (Tuberculosis) Laboratory manifest CSV import operations.
 * Contains TB-specific data points per spec FR-014: - A. Sample Identity
 * (sample ID from submitter) - B. Specimen Information (type, quality) - C.
 * Referring Facility - D. Patient Metadata (name, age, sex, ID, study ID,
 * address, phone, consent) - E. Clinical Context (treatment history) - F.
 * Requested Tests (culture, smear, GeneXpert, identification, DST) - G. Receipt
 * Details (site, date, time)
 */
public class TBManifestImportForm {

    private Integer notebookId;

    // A. Sample Identity (external ID from submitter, not system-generated)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn;

    // B. Specimen Information
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenQualityColumn;

    // C. Request Paper Details
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String documentNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String referringFacilityColumn;

    // D. Patient / Participant Metadata
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientAgeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientSexColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientAddressColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientPhoneColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String physicianPhoneColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String consentStatusColumn;

    // E. Clinical Context
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String treatmentHistoryColumn;

    // F. Requested Tests
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cultureColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String smearMicroscopyColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String genexpertColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String identificationColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dstFirstLineColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dstSecondLineColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String intendedMethodColumn;

    // G. Receipt Details
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedSiteColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedTimeColumn;

    // Common
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

    // Getters and Setters
    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public String getSampleIdColumn() {
        return sampleIdColumn;
    }

    public void setSampleIdColumn(String sampleIdColumn) {
        this.sampleIdColumn = sampleIdColumn;
    }

    public String getSpecimenTypeColumn() {
        return specimenTypeColumn;
    }

    public void setSpecimenTypeColumn(String specimenTypeColumn) {
        this.specimenTypeColumn = specimenTypeColumn;
    }

    public String getSpecimenQualityColumn() {
        return specimenQualityColumn;
    }

    public void setSpecimenQualityColumn(String specimenQualityColumn) {
        this.specimenQualityColumn = specimenQualityColumn;
    }

    public String getDocumentNumberColumn() {
        return documentNumberColumn;
    }

    public void setDocumentNumberColumn(String documentNumberColumn) {
        this.documentNumberColumn = documentNumberColumn;
    }

    public String getReferringFacilityColumn() {
        return referringFacilityColumn;
    }

    public void setReferringFacilityColumn(String referringFacilityColumn) {
        this.referringFacilityColumn = referringFacilityColumn;
    }

    public String getPatientNameColumn() {
        return patientNameColumn;
    }

    public void setPatientNameColumn(String patientNameColumn) {
        this.patientNameColumn = patientNameColumn;
    }

    public String getPatientAgeColumn() {
        return patientAgeColumn;
    }

    public void setPatientAgeColumn(String patientAgeColumn) {
        this.patientAgeColumn = patientAgeColumn;
    }

    public String getPatientSexColumn() {
        return patientSexColumn;
    }

    public void setPatientSexColumn(String patientSexColumn) {
        this.patientSexColumn = patientSexColumn;
    }

    public String getPatientIdColumn() {
        return patientIdColumn;
    }

    public void setPatientIdColumn(String patientIdColumn) {
        this.patientIdColumn = patientIdColumn;
    }

    public String getStudyIdColumn() {
        return studyIdColumn;
    }

    public void setStudyIdColumn(String studyIdColumn) {
        this.studyIdColumn = studyIdColumn;
    }

    public String getPatientAddressColumn() {
        return patientAddressColumn;
    }

    public void setPatientAddressColumn(String patientAddressColumn) {
        this.patientAddressColumn = patientAddressColumn;
    }

    public String getPatientPhoneColumn() {
        return patientPhoneColumn;
    }

    public void setPatientPhoneColumn(String patientPhoneColumn) {
        this.patientPhoneColumn = patientPhoneColumn;
    }

    public String getPhysicianPhoneColumn() {
        return physicianPhoneColumn;
    }

    public void setPhysicianPhoneColumn(String physicianPhoneColumn) {
        this.physicianPhoneColumn = physicianPhoneColumn;
    }

    public String getConsentStatusColumn() {
        return consentStatusColumn;
    }

    public void setConsentStatusColumn(String consentStatusColumn) {
        this.consentStatusColumn = consentStatusColumn;
    }

    public String getTreatmentHistoryColumn() {
        return treatmentHistoryColumn;
    }

    public void setTreatmentHistoryColumn(String treatmentHistoryColumn) {
        this.treatmentHistoryColumn = treatmentHistoryColumn;
    }

    public String getCultureColumn() {
        return cultureColumn;
    }

    public void setCultureColumn(String cultureColumn) {
        this.cultureColumn = cultureColumn;
    }

    public String getSmearMicroscopyColumn() {
        return smearMicroscopyColumn;
    }

    public void setSmearMicroscopyColumn(String smearMicroscopyColumn) {
        this.smearMicroscopyColumn = smearMicroscopyColumn;
    }

    public String getGenexpertColumn() {
        return genexpertColumn;
    }

    public void setGenexpertColumn(String genexpertColumn) {
        this.genexpertColumn = genexpertColumn;
    }

    public String getIdentificationColumn() {
        return identificationColumn;
    }

    public void setIdentificationColumn(String identificationColumn) {
        this.identificationColumn = identificationColumn;
    }

    public String getDstFirstLineColumn() {
        return dstFirstLineColumn;
    }

    public void setDstFirstLineColumn(String dstFirstLineColumn) {
        this.dstFirstLineColumn = dstFirstLineColumn;
    }

    public String getDstSecondLineColumn() {
        return dstSecondLineColumn;
    }

    public void setDstSecondLineColumn(String dstSecondLineColumn) {
        this.dstSecondLineColumn = dstSecondLineColumn;
    }

    public String getIntendedMethodColumn() {
        return intendedMethodColumn;
    }

    public void setIntendedMethodColumn(String intendedMethodColumn) {
        this.intendedMethodColumn = intendedMethodColumn;
    }

    public String getReceivedSiteColumn() {
        return receivedSiteColumn;
    }

    public void setReceivedSiteColumn(String receivedSiteColumn) {
        this.receivedSiteColumn = receivedSiteColumn;
    }

    public String getReceivedDateColumn() {
        return receivedDateColumn;
    }

    public void setReceivedDateColumn(String receivedDateColumn) {
        this.receivedDateColumn = receivedDateColumn;
    }

    public String getReceivedTimeColumn() {
        return receivedTimeColumn;
    }

    public void setReceivedTimeColumn(String receivedTimeColumn) {
        this.receivedTimeColumn = receivedTimeColumn;
    }

    public String getNumOfSamplesColumn() {
        return numOfSamplesColumn;
    }

    public void setNumOfSamplesColumn(String numOfSamplesColumn) {
        this.numOfSamplesColumn = numOfSamplesColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
