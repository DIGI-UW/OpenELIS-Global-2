package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for MNTD (Malaria and Neglected Tropical Disease) Laboratory
 * manifest CSV import operations. Contains MNTD-specific data points that are
 * hardcoded for this workflow.
 */
public class MNTDManifestImportForm {

    private Integer notebookId;

    // MNTD-specific column mappings (all required for MNTD workflow)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn; // Sample ID / Registration name

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleSourceColumn; // Field, External organization, etc.

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionSiteColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateTimeColumn; // Combined date and time

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectedByColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

    // Date format for parsing collection dates
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

    public String getSampleSourceColumn() {
        return sampleSourceColumn;
    }

    public void setSampleSourceColumn(String sampleSourceColumn) {
        this.sampleSourceColumn = sampleSourceColumn;
    }

    public String getProjectNameColumn() {
        return projectNameColumn;
    }

    public void setProjectNameColumn(String projectNameColumn) {
        this.projectNameColumn = projectNameColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getCollectionSiteColumn() {
        return collectionSiteColumn;
    }

    public void setCollectionSiteColumn(String collectionSiteColumn) {
        this.collectionSiteColumn = collectionSiteColumn;
    }

    public String getCollectionDateTimeColumn() {
        return collectionDateTimeColumn;
    }

    public void setCollectionDateTimeColumn(String collectionDateTimeColumn) {
        this.collectionDateTimeColumn = collectionDateTimeColumn;
    }

    public String getCollectedByColumn() {
        return collectedByColumn;
    }

    public void setCollectedByColumn(String collectedByColumn) {
        this.collectedByColumn = collectedByColumn;
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
