package org.openelisglobal.notebook.form;

/**
 * Form class for GBD (Genomic Bioanalytical Database) manifest CSV column
 * mapping configuration.
 *
 * Allows mapping of CSV column names to GBD-specific fields during manifest
 * import. This enables flexibility in CSV format while maintaining consistent
 * data validation for genomic sample reception and processing metadata.
 */
public class GBDManifestImportForm {

    // Required field column names
    private String sampleIdColumn;
    private String sampleTypeColumn;
    private String sourceColumn;
    private String collectionDateColumn;
    private String receptionDateTimeColumn;

    // Optional reception metadata
    private String projectStudyAssociationColumn;
    private String volumeConcentrationColumn;
    private String a260_280Column;
    private String a260_230Column;
    private String rinColumn;

    // Optional processing metadata
    private String extractionMethodKitColumn;
    private String pcrProtocolColumn;
    private String libraryPrepProtocolColumn;
    private String sequencingPlatformColumn;
    private String runIdColumn;
    private String operatorColumn;
    private String processingDateTimeColumn;
    private String notesColumn;

    public GBDManifestImportForm() {
        // Default constructor for form binding
    }

    // ==================== Getters and Setters ====================

    public String getSampleIdColumn() {
        return sampleIdColumn;
    }

    public void setSampleIdColumn(String sampleIdColumn) {
        this.sampleIdColumn = sampleIdColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getSourceColumn() {
        return sourceColumn;
    }

    public void setSourceColumn(String sourceColumn) {
        this.sourceColumn = sourceColumn;
    }

    public String getCollectionDateColumn() {
        return collectionDateColumn;
    }

    public void setCollectionDateColumn(String collectionDateColumn) {
        this.collectionDateColumn = collectionDateColumn;
    }

    public String getReceptionDateTimeColumn() {
        return receptionDateTimeColumn;
    }

    public void setReceptionDateTimeColumn(String receptionDateTimeColumn) {
        this.receptionDateTimeColumn = receptionDateTimeColumn;
    }

    public String getProjectStudyAssociationColumn() {
        return projectStudyAssociationColumn;
    }

    public void setProjectStudyAssociationColumn(String projectStudyAssociationColumn) {
        this.projectStudyAssociationColumn = projectStudyAssociationColumn;
    }

    public String getVolumeConcentrationColumn() {
        return volumeConcentrationColumn;
    }

    public void setVolumeConcentrationColumn(String volumeConcentrationColumn) {
        this.volumeConcentrationColumn = volumeConcentrationColumn;
    }

    public String getA260_280Column() {
        return a260_280Column;
    }

    public void setA260_280Column(String a260_280Column) {
        this.a260_280Column = a260_280Column;
    }

    public String getA260_230Column() {
        return a260_230Column;
    }

    public void setA260_230Column(String a260_230Column) {
        this.a260_230Column = a260_230Column;
    }

    public String getRinColumn() {
        return rinColumn;
    }

    public void setRinColumn(String rinColumn) {
        this.rinColumn = rinColumn;
    }

    public String getExtractionMethodKitColumn() {
        return extractionMethodKitColumn;
    }

    public void setExtractionMethodKitColumn(String extractionMethodKitColumn) {
        this.extractionMethodKitColumn = extractionMethodKitColumn;
    }

    public String getPcrProtocolColumn() {
        return pcrProtocolColumn;
    }

    public void setPcrProtocolColumn(String pcrProtocolColumn) {
        this.pcrProtocolColumn = pcrProtocolColumn;
    }

    public String getLibraryPrepProtocolColumn() {
        return libraryPrepProtocolColumn;
    }

    public void setLibraryPrepProtocolColumn(String libraryPrepProtocolColumn) {
        this.libraryPrepProtocolColumn = libraryPrepProtocolColumn;
    }

    public String getSequencingPlatformColumn() {
        return sequencingPlatformColumn;
    }

    public void setSequencingPlatformColumn(String sequencingPlatformColumn) {
        this.sequencingPlatformColumn = sequencingPlatformColumn;
    }

    public String getRunIdColumn() {
        return runIdColumn;
    }

    public void setRunIdColumn(String runIdColumn) {
        this.runIdColumn = runIdColumn;
    }

    public String getOperatorColumn() {
        return operatorColumn;
    }

    public void setOperatorColumn(String operatorColumn) {
        this.operatorColumn = operatorColumn;
    }

    public String getProcessingDateTimeColumn() {
        return processingDateTimeColumn;
    }

    public void setProcessingDateTimeColumn(String processingDateTimeColumn) {
        this.processingDateTimeColumn = processingDateTimeColumn;
    }

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }
}
