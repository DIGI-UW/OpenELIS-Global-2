package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Viral & Vaccine Unit Laboratory manifest CSV import operations.
 * Contains Viral & Vaccine-specific data points for the workflow including
 * sample identity, metadata, storage, and testing intent fields.
 */
public class ViralVaccineManifestImportForm {

    private Integer notebookId;

    // Sample Identity columns
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn; // Sample ID / Registration name

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleNameColumn; // Sample name/description

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn; // Type of sample

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String batchIdColumn; // Batch identifier

    // Metadata columns
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceOrganismColumn; // Source organism

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String passageNumberColumn; // Passage number

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateColumn; // Collection date

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectedByColumn; // Person who collected

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String originLabColumn; // Lab of origin

    // Storage columns
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageLocationColumn; // Storage location

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageTemperatureColumn; // Storage temperature

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String volumeColumn; // Sample volume

    // Testing intent columns
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String intendedUseColumn; // Intended use

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectNameColumn; // Project name

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn; // Number of samples to create

    // Date format for parsing dates
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

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

    public String getSampleNameColumn() {
        return sampleNameColumn;
    }

    public void setSampleNameColumn(String sampleNameColumn) {
        this.sampleNameColumn = sampleNameColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getBatchIdColumn() {
        return batchIdColumn;
    }

    public void setBatchIdColumn(String batchIdColumn) {
        this.batchIdColumn = batchIdColumn;
    }

    public String getSourceOrganismColumn() {
        return sourceOrganismColumn;
    }

    public void setSourceOrganismColumn(String sourceOrganismColumn) {
        this.sourceOrganismColumn = sourceOrganismColumn;
    }

    public String getPassageNumberColumn() {
        return passageNumberColumn;
    }

    public void setPassageNumberColumn(String passageNumberColumn) {
        this.passageNumberColumn = passageNumberColumn;
    }

    public String getCollectionDateColumn() {
        return collectionDateColumn;
    }

    public void setCollectionDateColumn(String collectionDateColumn) {
        this.collectionDateColumn = collectionDateColumn;
    }

    public String getCollectedByColumn() {
        return collectedByColumn;
    }

    public void setCollectedByColumn(String collectedByColumn) {
        this.collectedByColumn = collectedByColumn;
    }

    public String getOriginLabColumn() {
        return originLabColumn;
    }

    public void setOriginLabColumn(String originLabColumn) {
        this.originLabColumn = originLabColumn;
    }

    public String getStorageLocationColumn() {
        return storageLocationColumn;
    }

    public void setStorageLocationColumn(String storageLocationColumn) {
        this.storageLocationColumn = storageLocationColumn;
    }

    public String getStorageTemperatureColumn() {
        return storageTemperatureColumn;
    }

    public void setStorageTemperatureColumn(String storageTemperatureColumn) {
        this.storageTemperatureColumn = storageTemperatureColumn;
    }

    public String getVolumeColumn() {
        return volumeColumn;
    }

    public void setVolumeColumn(String volumeColumn) {
        this.volumeColumn = volumeColumn;
    }

    public String getIntendedUseColumn() {
        return intendedUseColumn;
    }

    public void setIntendedUseColumn(String intendedUseColumn) {
        this.intendedUseColumn = intendedUseColumn;
    }

    public String getProjectNameColumn() {
        return projectNameColumn;
    }

    public void setProjectNameColumn(String projectNameColumn) {
        this.projectNameColumn = projectNameColumn;
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
