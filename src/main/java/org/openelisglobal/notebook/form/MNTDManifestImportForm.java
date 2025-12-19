package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for MNTD (Malaria and Neglected Tropical Disease) Laboratory
 * manifest CSV import operations. Contains MNTD-specific data points that are
 * hardcoded for this workflow.
 *
 * Field naming convention: uses camelCase with "Column" suffix to indicate CSV
 * column mapping. Field names match the frontend MNTDManifestImportModal.js
 * columnMapping state.
 */
public class MNTDManifestImportForm {

    private Integer notebookId;

    // Required MNTD-specific column mappings
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdTagColumn; // Sample ID/Tag - pre-labeled identifier

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numberOfSamplesColumn; // Quantity and container type

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleSourceLocationColumn; // Geographic origin

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String broughtByColumn; // Person/institution delivering sample

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedDateTimeColumn; // When sample was received

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receptionistNameColumn; // Who received the sample

    // Sample type (validated against MNTD lab types)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    // Optional MNTD-specific column mappings
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String feedingStatusColumn; // For vector samples

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String parasiteStageColumn; // Developmental stage

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionColumn; // Required storage temp

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String externalOrganizationColumn; // Source organization

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String countryOfOriginColumn; // For international samples

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionMethodColumn; // How sample was collected

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String specimenConditionColumn; // Condition on arrival

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String remarksColumn; // Additional notes

    // Date format for parsing dates
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

    // Getters and setters
    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public String getProjectNameColumn() {
        return projectNameColumn;
    }

    public void setProjectNameColumn(String projectNameColumn) {
        this.projectNameColumn = projectNameColumn;
    }

    public String getSampleIdTagColumn() {
        return sampleIdTagColumn;
    }

    public void setSampleIdTagColumn(String sampleIdTagColumn) {
        this.sampleIdTagColumn = sampleIdTagColumn;
    }

    public String getNumberOfSamplesColumn() {
        return numberOfSamplesColumn;
    }

    public void setNumberOfSamplesColumn(String numberOfSamplesColumn) {
        this.numberOfSamplesColumn = numberOfSamplesColumn;
    }

    public String getSampleSourceLocationColumn() {
        return sampleSourceLocationColumn;
    }

    public void setSampleSourceLocationColumn(String sampleSourceLocationColumn) {
        this.sampleSourceLocationColumn = sampleSourceLocationColumn;
    }

    public String getBroughtByColumn() {
        return broughtByColumn;
    }

    public void setBroughtByColumn(String broughtByColumn) {
        this.broughtByColumn = broughtByColumn;
    }

    public String getReceivedDateTimeColumn() {
        return receivedDateTimeColumn;
    }

    public void setReceivedDateTimeColumn(String receivedDateTimeColumn) {
        this.receivedDateTimeColumn = receivedDateTimeColumn;
    }

    public String getReceptionistNameColumn() {
        return receptionistNameColumn;
    }

    public void setReceptionistNameColumn(String receptionistNameColumn) {
        this.receptionistNameColumn = receptionistNameColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getFeedingStatusColumn() {
        return feedingStatusColumn;
    }

    public void setFeedingStatusColumn(String feedingStatusColumn) {
        this.feedingStatusColumn = feedingStatusColumn;
    }

    public String getParasiteStageColumn() {
        return parasiteStageColumn;
    }

    public void setParasiteStageColumn(String parasiteStageColumn) {
        this.parasiteStageColumn = parasiteStageColumn;
    }

    public String getStorageConditionColumn() {
        return storageConditionColumn;
    }

    public void setStorageConditionColumn(String storageConditionColumn) {
        this.storageConditionColumn = storageConditionColumn;
    }

    public String getExternalOrganizationColumn() {
        return externalOrganizationColumn;
    }

    public void setExternalOrganizationColumn(String externalOrganizationColumn) {
        this.externalOrganizationColumn = externalOrganizationColumn;
    }

    public String getCountryOfOriginColumn() {
        return countryOfOriginColumn;
    }

    public void setCountryOfOriginColumn(String countryOfOriginColumn) {
        this.countryOfOriginColumn = countryOfOriginColumn;
    }

    public String getCollectionMethodColumn() {
        return collectionMethodColumn;
    }

    public void setCollectionMethodColumn(String collectionMethodColumn) {
        this.collectionMethodColumn = collectionMethodColumn;
    }

    public String getSpecimenConditionColumn() {
        return specimenConditionColumn;
    }

    public void setSpecimenConditionColumn(String specimenConditionColumn) {
        this.specimenConditionColumn = specimenConditionColumn;
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
