package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Pharmaceuticals manifest CSV import. Allows column mapping for
 * pharma-specific data points.
 */
public class PharmaManifestImportForm {

    private Integer notebookId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String groupIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String chemicalNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String gradeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String lotNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateOfManufactureColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String expiryOrRetestDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String ownerColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicalTrialNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String consentStatusColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notesColumn;

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

    public String getChemicalNameColumn() {
        return chemicalNameColumn;
    }

    public void setChemicalNameColumn(String chemicalNameColumn) {
        this.chemicalNameColumn = chemicalNameColumn;
    }

    public String getGradeColumn() {
        return gradeColumn;
    }

    public void setGradeColumn(String gradeColumn) {
        this.gradeColumn = gradeColumn;
    }

    public String getLotNumberColumn() {
        return lotNumberColumn;
    }

    public void setLotNumberColumn(String lotNumberColumn) {
        this.lotNumberColumn = lotNumberColumn;
    }

    public String getDateOfManufactureColumn() {
        return dateOfManufactureColumn;
    }

    public void setDateOfManufactureColumn(String dateOfManufactureColumn) {
        this.dateOfManufactureColumn = dateOfManufactureColumn;
    }

    public String getExpiryOrRetestDateColumn() {
        return expiryOrRetestDateColumn;
    }

    public void setExpiryOrRetestDateColumn(String expiryOrRetestDateColumn) {
        this.expiryOrRetestDateColumn = expiryOrRetestDateColumn;
    }

    public String getStorageConditionColumn() {
        return storageConditionColumn;
    }

    public void setStorageConditionColumn(String storageConditionColumn) {
        this.storageConditionColumn = storageConditionColumn;
    }

    public String getOwnerColumn() {
        return ownerColumn;
    }

    public void setOwnerColumn(String ownerColumn) {
        this.ownerColumn = ownerColumn;
    }

    public String getPatientIdColumn() {
        return patientIdColumn;
    }

    public void setPatientIdColumn(String patientIdColumn) {
        this.patientIdColumn = patientIdColumn;
    }

    public String getClinicalTrialNumberColumn() {
        return clinicalTrialNumberColumn;
    }

    public void setClinicalTrialNumberColumn(String clinicalTrialNumberColumn) {
        this.clinicalTrialNumberColumn = clinicalTrialNumberColumn;
    }

    public String getConsentStatusColumn() {
        return consentStatusColumn;
    }

    public void setConsentStatusColumn(String consentStatusColumn) {
        this.consentStatusColumn = consentStatusColumn;
    }

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }
}
