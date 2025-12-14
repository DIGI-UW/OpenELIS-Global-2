package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for manifest CSV import operations. Used to create new SampleItem
 * records from a manifest file.
 */
public class ManifestImportForm {

    private Integer notebookId;

    // Column mapping configuration
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String groupIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String volumeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

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

    public String getCollectionDateColumn() {
        return collectionDateColumn;
    }

    public void setCollectionDateColumn(String collectionDateColumn) {
        this.collectionDateColumn = collectionDateColumn;
    }

    public String getVolumeColumn() {
        return volumeColumn;
    }

    public void setVolumeColumn(String volumeColumn) {
        this.volumeColumn = volumeColumn;
    }

    public String getNumOfSamplesColumn() {
        return numOfSamplesColumn;
    }

    public void setNumOfSamplesColumn(String numOfSamplesColumn) {
        this.numOfSamplesColumn = numOfSamplesColumn;
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
