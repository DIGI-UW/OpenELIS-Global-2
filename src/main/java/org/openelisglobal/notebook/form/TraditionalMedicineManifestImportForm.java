package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Traditional Medicine manifest CSV import. Allows column mapping
 * for traditional medicine-specific data points.
 */
public class TraditionalMedicineManifestImportForm {

    private Integer notebookId;

    // Sample Identity
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleCategoryColumn;

    // Sample Source & Origin
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String originLocationColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionSiteColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectedByColumn;

    // Taxonomy / Identification
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String localNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String scientificNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String speciesColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String plantPartColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleConditionColumn;

    // Intended Use
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String intendedUseColumn;

    // Number of samples (for bulk creation)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String numOfSamplesColumn;

    // Notes
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notesColumn;

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

    public String getSampleCategoryColumn() {
        return sampleCategoryColumn;
    }

    public void setSampleCategoryColumn(String sampleCategoryColumn) {
        this.sampleCategoryColumn = sampleCategoryColumn;
    }

    public String getSourceTypeColumn() {
        return sourceTypeColumn;
    }

    public void setSourceTypeColumn(String sourceTypeColumn) {
        this.sourceTypeColumn = sourceTypeColumn;
    }

    public String getOriginLocationColumn() {
        return originLocationColumn;
    }

    public void setOriginLocationColumn(String originLocationColumn) {
        this.originLocationColumn = originLocationColumn;
    }

    public String getCollectionSiteColumn() {
        return collectionSiteColumn;
    }

    public void setCollectionSiteColumn(String collectionSiteColumn) {
        this.collectionSiteColumn = collectionSiteColumn;
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

    public String getLocalNameColumn() {
        return localNameColumn;
    }

    public void setLocalNameColumn(String localNameColumn) {
        this.localNameColumn = localNameColumn;
    }

    public String getScientificNameColumn() {
        return scientificNameColumn;
    }

    public void setScientificNameColumn(String scientificNameColumn) {
        this.scientificNameColumn = scientificNameColumn;
    }

    public String getSpeciesColumn() {
        return speciesColumn;
    }

    public void setSpeciesColumn(String speciesColumn) {
        this.speciesColumn = speciesColumn;
    }

    public String getPlantPartColumn() {
        return plantPartColumn;
    }

    public void setPlantPartColumn(String plantPartColumn) {
        this.plantPartColumn = plantPartColumn;
    }

    public String getSampleConditionColumn() {
        return sampleConditionColumn;
    }

    public void setSampleConditionColumn(String sampleConditionColumn) {
        this.sampleConditionColumn = sampleConditionColumn;
    }

    public String getIntendedUseColumn() {
        return intendedUseColumn;
    }

    public void setIntendedUseColumn(String intendedUseColumn) {
        this.intendedUseColumn = intendedUseColumn;
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
}
