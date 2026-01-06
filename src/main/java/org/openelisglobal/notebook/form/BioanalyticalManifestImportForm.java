package org.openelisglobal.notebook.form;

/**
 * Form class for Bioanalytical manifest CSV column mapping configuration.
 *
 * Allows mapping of CSV column names to bioanalytical-specific fields during
 * manifest import. This enables flexibility in CSV format while maintaining
 * consistent data validation.
 */
public class BioanalyticalManifestImportForm {

    // Required field column names
    private String uniqueSampleIdColumn;
    private String sampleTypeColumn;
    private String sourceOriginColumn;
    private String requestedTestsColumn;
    private String dateTimeOfReceiptColumn;
    private String receivingPersonnelColumn;

    // Optional field column names
    private String projectStudyAssociationColumn;
    private String storageConditionPriorColumn;
    private String sampleVolumeColumn;
    private String transportTemperatureColumn;
    private String manifestVerificationStatusColumn;
    private String subjectIdColumn;
    private String timepointColumn;
    private String notesColumn;

    public BioanalyticalManifestImportForm() {
        // Default constructor for form binding
    }

    // ==================== Getters and Setters ====================

    public String getUniqueSampleIdColumn() {
        return uniqueSampleIdColumn;
    }

    public void setUniqueSampleIdColumn(String uniqueSampleIdColumn) {
        this.uniqueSampleIdColumn = uniqueSampleIdColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getSourceOriginColumn() {
        return sourceOriginColumn;
    }

    public void setSourceOriginColumn(String sourceOriginColumn) {
        this.sourceOriginColumn = sourceOriginColumn;
    }

    public String getRequestedTestsColumn() {
        return requestedTestsColumn;
    }

    public void setRequestedTestsColumn(String requestedTestsColumn) {
        this.requestedTestsColumn = requestedTestsColumn;
    }

    public String getDateTimeOfReceiptColumn() {
        return dateTimeOfReceiptColumn;
    }

    public void setDateTimeOfReceiptColumn(String dateTimeOfReceiptColumn) {
        this.dateTimeOfReceiptColumn = dateTimeOfReceiptColumn;
    }

    public String getReceivingPersonnelColumn() {
        return receivingPersonnelColumn;
    }

    public void setReceivingPersonnelColumn(String receivingPersonnelColumn) {
        this.receivingPersonnelColumn = receivingPersonnelColumn;
    }

    public String getProjectStudyAssociationColumn() {
        return projectStudyAssociationColumn;
    }

    public void setProjectStudyAssociationColumn(String projectStudyAssociationColumn) {
        this.projectStudyAssociationColumn = projectStudyAssociationColumn;
    }

    public String getStorageConditionPriorColumn() {
        return storageConditionPriorColumn;
    }

    public void setStorageConditionPriorColumn(String storageConditionPriorColumn) {
        this.storageConditionPriorColumn = storageConditionPriorColumn;
    }

    public String getSampleVolumeColumn() {
        return sampleVolumeColumn;
    }

    public void setSampleVolumeColumn(String sampleVolumeColumn) {
        this.sampleVolumeColumn = sampleVolumeColumn;
    }

    public String getTransportTemperatureColumn() {
        return transportTemperatureColumn;
    }

    public void setTransportTemperatureColumn(String transportTemperatureColumn) {
        this.transportTemperatureColumn = transportTemperatureColumn;
    }

    public String getManifestVerificationStatusColumn() {
        return manifestVerificationStatusColumn;
    }

    public void setManifestVerificationStatusColumn(String manifestVerificationStatusColumn) {
        this.manifestVerificationStatusColumn = manifestVerificationStatusColumn;
    }

    public String getSubjectIdColumn() {
        return subjectIdColumn;
    }

    public void setSubjectIdColumn(String subjectIdColumn) {
        this.subjectIdColumn = subjectIdColumn;
    }

    public String getTimepointColumn() {
        return timepointColumn;
    }

    public void setTimepointColumn(String timepointColumn) {
        this.timepointColumn = timepointColumn;
    }

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }
}
