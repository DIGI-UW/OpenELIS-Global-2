package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Pathology Laboratory manifest CSV import operations. Contains
 * Pathology-specific data points that align with the enhanced metadata capture
 * requirements:
 *
 * Patient Identification: - First Name is MANDATORY (primary name field for
 * order acceptance) - Surname/Last Name is OPTIONAL (not required for order
 * acceptance) - National ID is OPTIONAL (not required for order acceptance)
 *
 * Clinical Samples: Patient ID, Requesting Clinician, Collection DateTime,
 * Specimen Type/Site, Clinical Details Research Samples: Study ID, PI Name,
 * Participant/Animal ID, Ethical Approval Ref All Samples: Receiving DateTime,
 * Receiving Staff Name, Source Facility
 *
 * Field naming convention: uses camelCase with "Column" suffix to indicate CSV
 * column mapping. Field names match the frontend
 * PathologyManifestImportModal.js columnMapping state.
 */
public class PathologyManifestImportForm {

    private Integer notebookId;

    // ========== PATIENT IDENTIFICATION COLUMNS ==========
    // First Name is MANDATORY (primary name field for order acceptance)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String firstNameColumn;

    // Surname/Last Name is OPTIONAL (not required for order acceptance)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String surnameColumn;

    // National ID is OPTIONAL (not required for order acceptance)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nationalIdColumn;

    // ========== SAMPLE CATEGORY COLUMN ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleCategoryColumn; // "Clinical diagnostic" or "Research"

    // ========== RECEIVING INFO COLUMNS (ALL SAMPLES) ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedByColumn; // Receiving staff name

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceFacilityColumn;

    // ========== SPECIMEN INFO COLUMNS ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenSiteColumn; // Patient Site / Anatomical Source

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionMethodColumn; // Biopsy, FNAC, fluid aspiration, blood draw, amputation

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectorColumn; // Collector / Personnel

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String processingConditionColumn; // Fresh, Fixed, Frozen, FFPE, etc.

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String laboratoryMaterialColumn; // Blocks, Slides, Smears, DNA/RNA extracts, Frozen sections

    // ========== CLINICAL SAMPLE COLUMNS ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requestingClinicianColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicalDetailsColumn;

    // ========== RESEARCH SAMPLE COLUMNS ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String piNameColumn; // Principal Investigator

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String participantAnimalIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String ethicalApprovalRefColumn;

    // ========== OPTIONAL COLUMNS ==========
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String remarksColumn;

    // Date format for parsing dates
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

    // ========== GETTERS AND SETTERS ==========

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public String getFirstNameColumn() {
        return firstNameColumn;
    }

    public void setFirstNameColumn(String firstNameColumn) {
        this.firstNameColumn = firstNameColumn;
    }

    public String getSurnameColumn() {
        return surnameColumn;
    }

    public void setSurnameColumn(String surnameColumn) {
        this.surnameColumn = surnameColumn;
    }

    public String getNationalIdColumn() {
        return nationalIdColumn;
    }

    public void setNationalIdColumn(String nationalIdColumn) {
        this.nationalIdColumn = nationalIdColumn;
    }

    public String getSampleCategoryColumn() {
        return sampleCategoryColumn;
    }

    public void setSampleCategoryColumn(String sampleCategoryColumn) {
        this.sampleCategoryColumn = sampleCategoryColumn;
    }

    public String getReceivedDateTimeColumn() {
        return receivedDateTimeColumn;
    }

    public void setReceivedDateTimeColumn(String receivedDateTimeColumn) {
        this.receivedDateTimeColumn = receivedDateTimeColumn;
    }

    public String getReceivedByColumn() {
        return receivedByColumn;
    }

    public void setReceivedByColumn(String receivedByColumn) {
        this.receivedByColumn = receivedByColumn;
    }

    public String getSourceFacilityColumn() {
        return sourceFacilityColumn;
    }

    public void setSourceFacilityColumn(String sourceFacilityColumn) {
        this.sourceFacilityColumn = sourceFacilityColumn;
    }

    public String getSpecimenTypeColumn() {
        return specimenTypeColumn;
    }

    public void setSpecimenTypeColumn(String specimenTypeColumn) {
        this.specimenTypeColumn = specimenTypeColumn;
    }

    public String getSpecimenSiteColumn() {
        return specimenSiteColumn;
    }

    public void setSpecimenSiteColumn(String specimenSiteColumn) {
        this.specimenSiteColumn = specimenSiteColumn;
    }

    public String getCollectionDateTimeColumn() {
        return collectionDateTimeColumn;
    }

    public void setCollectionDateTimeColumn(String collectionDateTimeColumn) {
        this.collectionDateTimeColumn = collectionDateTimeColumn;
    }

    public String getCollectionMethodColumn() {
        return collectionMethodColumn;
    }

    public void setCollectionMethodColumn(String collectionMethodColumn) {
        this.collectionMethodColumn = collectionMethodColumn;
    }

    public String getCollectorColumn() {
        return collectorColumn;
    }

    public void setCollectorColumn(String collectorColumn) {
        this.collectorColumn = collectorColumn;
    }

    public String getProcessingConditionColumn() {
        return processingConditionColumn;
    }

    public void setProcessingConditionColumn(String processingConditionColumn) {
        this.processingConditionColumn = processingConditionColumn;
    }

    public String getLaboratoryMaterialColumn() {
        return laboratoryMaterialColumn;
    }

    public void setLaboratoryMaterialColumn(String laboratoryMaterialColumn) {
        this.laboratoryMaterialColumn = laboratoryMaterialColumn;
    }

    public String getPatientIdColumn() {
        return patientIdColumn;
    }

    public void setPatientIdColumn(String patientIdColumn) {
        this.patientIdColumn = patientIdColumn;
    }

    public String getRequestingClinicianColumn() {
        return requestingClinicianColumn;
    }

    public void setRequestingClinicianColumn(String requestingClinicianColumn) {
        this.requestingClinicianColumn = requestingClinicianColumn;
    }

    public String getClinicalDetailsColumn() {
        return clinicalDetailsColumn;
    }

    public void setClinicalDetailsColumn(String clinicalDetailsColumn) {
        this.clinicalDetailsColumn = clinicalDetailsColumn;
    }

    public String getStudyIdColumn() {
        return studyIdColumn;
    }

    public void setStudyIdColumn(String studyIdColumn) {
        this.studyIdColumn = studyIdColumn;
    }

    public String getPiNameColumn() {
        return piNameColumn;
    }

    public void setPiNameColumn(String piNameColumn) {
        this.piNameColumn = piNameColumn;
    }

    public String getParticipantAnimalIdColumn() {
        return participantAnimalIdColumn;
    }

    public void setParticipantAnimalIdColumn(String participantAnimalIdColumn) {
        this.participantAnimalIdColumn = participantAnimalIdColumn;
    }

    public String getEthicalApprovalRefColumn() {
        return ethicalApprovalRefColumn;
    }

    public void setEthicalApprovalRefColumn(String ethicalApprovalRefColumn) {
        this.ethicalApprovalRefColumn = ethicalApprovalRefColumn;
    }

    public String getRemarksColumn() {
        return remarksColumn;
    }

    public void setRemarksColumn(String remarksColumn) {
        this.remarksColumn = remarksColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
