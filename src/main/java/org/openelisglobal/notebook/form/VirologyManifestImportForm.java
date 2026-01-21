package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Virology & Vaccine Unit manifest CSV import. Allows column
 * mapping for virology-specific reception metadata.
 *
 * Required fields: sampleId, source, sampleType, receptionDateTime, testType,
 * projectStudyAssociation
 *
 * Virus/Vaccine Production fields: batchId, productionStage, cellLineUsed,
 * passageNumber, titerValues, qualityControlResults, formulationDetails
 *
 * Optional fields: storageConditionOnArrival, transportTemperature,
 * receivingPersonnelName, manifestVerificationStatus, notes
 *
 * Auto-generated (not in CSV): accessionNumber, barcodeQrCode
 */
public class VirologyManifestImportForm {

    private Integer notebookId;

    // Required fields - Sample Arrival
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receptionDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String testTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectStudyAssociationColumn;

    // Virus/Vaccine Production fields (optional but important for production
    // workflow)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String batchIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String productionStageColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String cellLineUsedColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String passageNumberColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String titerValuesColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String qualityControlResultsColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String formulationDetailsColumn;

    // Optional fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionOnArrivalColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String transportTemperatureColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivingPersonnelNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String manifestVerificationStatusColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notesColumn;

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    // Required field getters/setters - Sample Arrival
    public String getSampleIdColumn() {
        return sampleIdColumn;
    }

    public void setSampleIdColumn(String sampleIdColumn) {
        this.sampleIdColumn = sampleIdColumn;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getReceptionDateTimeColumn() {
        return receptionDateTimeColumn;
    }

    public void setReceptionDateTimeColumn(String receptionDateTimeColumn) {
        this.receptionDateTimeColumn = receptionDateTimeColumn;
    }

    public String getTestTypeColumn() {
        return testTypeColumn;
    }

    public void setTestTypeColumn(String testTypeColumn) {
        this.testTypeColumn = testTypeColumn;
    }

    public String getProjectStudyAssociationColumn() {
        return projectStudyAssociationColumn;
    }

    public void setProjectStudyAssociationColumn(String projectStudyAssociationColumn) {
        this.projectStudyAssociationColumn = projectStudyAssociationColumn;
    }

    // Virus/Vaccine Production field getters/setters
    public String getBatchIdColumn() {
        return batchIdColumn;
    }

    public void setBatchIdColumn(String batchIdColumn) {
        this.batchIdColumn = batchIdColumn;
    }

    public String getProductionStageColumn() {
        return productionStageColumn;
    }

    public void setProductionStageColumn(String productionStageColumn) {
        this.productionStageColumn = productionStageColumn;
    }

    public String getCellLineUsedColumn() {
        return cellLineUsedColumn;
    }

    public void setCellLineUsedColumn(String cellLineUsedColumn) {
        this.cellLineUsedColumn = cellLineUsedColumn;
    }

    public String getPassageNumberColumn() {
        return passageNumberColumn;
    }

    public void setPassageNumberColumn(String passageNumberColumn) {
        this.passageNumberColumn = passageNumberColumn;
    }

    public String getTiterValuesColumn() {
        return titerValuesColumn;
    }

    public void setTiterValuesColumn(String titerValuesColumn) {
        this.titerValuesColumn = titerValuesColumn;
    }

    public String getQualityControlResultsColumn() {
        return qualityControlResultsColumn;
    }

    public void setQualityControlResultsColumn(String qualityControlResultsColumn) {
        this.qualityControlResultsColumn = qualityControlResultsColumn;
    }

    public String getFormulationDetailsColumn() {
        return formulationDetailsColumn;
    }

    public void setFormulationDetailsColumn(String formulationDetailsColumn) {
        this.formulationDetailsColumn = formulationDetailsColumn;
    }

    // Optional field getters/setters
    public String getCollectionDateTimeColumn() {
        return collectionDateTimeColumn;
    }

    public void setCollectionDateTimeColumn(String collectionDateTimeColumn) {
        this.collectionDateTimeColumn = collectionDateTimeColumn;
    }

    public String getStorageConditionOnArrivalColumn() {
        return storageConditionOnArrivalColumn;
    }

    public void setStorageConditionOnArrivalColumn(String storageConditionOnArrivalColumn) {
        this.storageConditionOnArrivalColumn = storageConditionOnArrivalColumn;
    }

    public String getTransportTemperatureColumn() {
        return transportTemperatureColumn;
    }

    public void setTransportTemperatureColumn(String transportTemperatureColumn) {
        this.transportTemperatureColumn = transportTemperatureColumn;
    }

    public String getReceivingPersonnelNameColumn() {
        return receivingPersonnelNameColumn;
    }

    public void setReceivingPersonnelNameColumn(String receivingPersonnelNameColumn) {
        this.receivingPersonnelNameColumn = receivingPersonnelNameColumn;
    }

    public String getManifestVerificationStatusColumn() {
        return manifestVerificationStatusColumn;
    }

    public void setManifestVerificationStatusColumn(String manifestVerificationStatusColumn) {
        this.manifestVerificationStatusColumn = manifestVerificationStatusColumn;
    }

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }
}
