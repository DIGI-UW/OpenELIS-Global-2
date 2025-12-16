package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Pathology Laboratory manifest CSV import operations. Contains
 * pathology-specific data points including clinical and research sample
 * metadata.
 */
public class PathologyManifestImportForm {

    private Integer notebookId;

    // Required fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String groupIdColumn; // Group ID / Batch identifier

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateColumn;

    // Sample Identity
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleCategoryColumn; // Clinical diagnostic or Research

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceFacilityColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenSiteColumn;

    // Clinical metadata (used when sampleCategory is "Clinical diagnostic")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requestingClinicianColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicalDetailsColumn;

    // Research metadata (used when sampleCategory is "Research")
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String piNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String participantAnimalIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String ethicalApprovalRefColumn;

    // Notes
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notesColumn;

    // Date format for parsing collection dates
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public String getGroupIdColumn() {
        return groupIdColumn;
    }

    public void setGroupIdColumn(String groupIdColumn) {
        this.groupIdColumn = groupIdColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getNumOfSamplesColumn() {
        return numOfSamplesColumn;
    }

    public void setNumOfSamplesColumn(String numOfSamplesColumn) {
        this.numOfSamplesColumn = numOfSamplesColumn;
    }

    public String getCollectionDateColumn() {
        return collectionDateColumn;
    }

    public void setCollectionDateColumn(String collectionDateColumn) {
        this.collectionDateColumn = collectionDateColumn;
    }

    public String getSampleCategoryColumn() {
        return sampleCategoryColumn;
    }

    public void setSampleCategoryColumn(String sampleCategoryColumn) {
        this.sampleCategoryColumn = sampleCategoryColumn;
    }

    public String getSourceFacilityColumn() {
        return sourceFacilityColumn;
    }

    public void setSourceFacilityColumn(String sourceFacilityColumn) {
        this.sourceFacilityColumn = sourceFacilityColumn;
    }

    public String getSpecimenSiteColumn() {
        return specimenSiteColumn;
    }

    public void setSpecimenSiteColumn(String specimenSiteColumn) {
        this.specimenSiteColumn = specimenSiteColumn;
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

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
