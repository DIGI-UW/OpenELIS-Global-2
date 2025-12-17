package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Pharmaceuticals manifest CSV import. Allows column mapping for
 * pharma-specific data points aligned with the updated dataPoints schema.
 *
 * Required fields: sampleName, lotBatchNumber, dateOfManufacture,
 * expiryRetestDate, storageCondition, ownerRequester
 *
 * Optional fields: alphanumericCode, chemicalIupacName, gradeSpecification,
 * chainOfCustodyDetails, patientId, clinicalTrialNumber, consentStatus
 *
 * Auto-generated (not in CSV): uniqueSampleId, barcodeQrCode
 */
public class PharmaManifestImportForm {

    private Integer notebookId;

    // Required fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String lotBatchNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateOfManufactureColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String expiryRetestDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String ownerRequesterColumn;

    // Sample type field (validated against Pharmaceutical lab types)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    // Optional fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String alphanumericCodeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String chemicalIupacNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String gradeSpecificationColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String chainOfCustodyDetailsColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String clinicalTrialNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String consentStatusColumn;

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    // Required field getters/setters
    public String getSampleNameColumn() {
        return sampleNameColumn;
    }

    public void setSampleNameColumn(String sampleNameColumn) {
        this.sampleNameColumn = sampleNameColumn;
    }

    public String getLotBatchNumberColumn() {
        return lotBatchNumberColumn;
    }

    public void setLotBatchNumberColumn(String lotBatchNumberColumn) {
        this.lotBatchNumberColumn = lotBatchNumberColumn;
    }

    public String getDateOfManufactureColumn() {
        return dateOfManufactureColumn;
    }

    public void setDateOfManufactureColumn(String dateOfManufactureColumn) {
        this.dateOfManufactureColumn = dateOfManufactureColumn;
    }

    public String getExpiryRetestDateColumn() {
        return expiryRetestDateColumn;
    }

    public void setExpiryRetestDateColumn(String expiryRetestDateColumn) {
        this.expiryRetestDateColumn = expiryRetestDateColumn;
    }

    public String getStorageConditionColumn() {
        return storageConditionColumn;
    }

    public void setStorageConditionColumn(String storageConditionColumn) {
        this.storageConditionColumn = storageConditionColumn;
    }

    public String getOwnerRequesterColumn() {
        return ownerRequesterColumn;
    }

    public void setOwnerRequesterColumn(String ownerRequesterColumn) {
        this.ownerRequesterColumn = ownerRequesterColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    // Optional field getters/setters
    public String getAlphanumericCodeColumn() {
        return alphanumericCodeColumn;
    }

    public void setAlphanumericCodeColumn(String alphanumericCodeColumn) {
        this.alphanumericCodeColumn = alphanumericCodeColumn;
    }

    public String getChemicalIupacNameColumn() {
        return chemicalIupacNameColumn;
    }

    public void setChemicalIupacNameColumn(String chemicalIupacNameColumn) {
        this.chemicalIupacNameColumn = chemicalIupacNameColumn;
    }

    public String getGradeSpecificationColumn() {
        return gradeSpecificationColumn;
    }

    public void setGradeSpecificationColumn(String gradeSpecificationColumn) {
        this.gradeSpecificationColumn = gradeSpecificationColumn;
    }

    public String getChainOfCustodyDetailsColumn() {
        return chainOfCustodyDetailsColumn;
    }

    public void setChainOfCustodyDetailsColumn(String chainOfCustodyDetailsColumn) {
        this.chainOfCustodyDetailsColumn = chainOfCustodyDetailsColumn;
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
}
