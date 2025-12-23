package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Bacteriology Laboratory manifest CSV import operations.
 * Contains Bacteriology-specific data points matching the reception metadata: -
 * Project Name, Study ID - Participant ID, Barcode - Collection Site, Sample
 * Type, Collection Date & Time - Sample Received Date, Sample Arrival Time,
 * Received By - Storage Container Type, Storage Temperature on Arrival -
 * Consent Status, CRF Status - Sample Origin (Human/Animal/Environmental/Food),
 * Source Location/Facility
 */
public class BacteriologyManifestImportForm {

    private Integer notebookId;

    // Project Information
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyIdColumn;

    // Sample Identity
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String participantIdColumn; // For clinical samples

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String barcodeColumn;

    // Collection Metadata
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionSiteColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateTimeColumn;

    // Reception Metadata
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleReceivedDateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleArrivalTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedByColumn;

    // Storage Conditions
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageContainerTypeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageTemperatureOnArrivalColumn;

    // Compliance Status
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String consentStatusColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String crfStatusColumn;

    // Sample Origin Tracking
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleOriginColumn; // Human, Animal, Environmental, Food/Beverage

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceLocationFacilityColumn;

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

    public String getStudyIdColumn() {
        return studyIdColumn;
    }

    public void setStudyIdColumn(String studyIdColumn) {
        this.studyIdColumn = studyIdColumn;
    }

    public String getParticipantIdColumn() {
        return participantIdColumn;
    }

    public void setParticipantIdColumn(String participantIdColumn) {
        this.participantIdColumn = participantIdColumn;
    }

    public String getBarcodeColumn() {
        return barcodeColumn;
    }

    public void setBarcodeColumn(String barcodeColumn) {
        this.barcodeColumn = barcodeColumn;
    }

    public String getCollectionSiteColumn() {
        return collectionSiteColumn;
    }

    public void setCollectionSiteColumn(String collectionSiteColumn) {
        this.collectionSiteColumn = collectionSiteColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getCollectionDateTimeColumn() {
        return collectionDateTimeColumn;
    }

    public void setCollectionDateTimeColumn(String collectionDateTimeColumn) {
        this.collectionDateTimeColumn = collectionDateTimeColumn;
    }

    public String getSampleReceivedDateColumn() {
        return sampleReceivedDateColumn;
    }

    public void setSampleReceivedDateColumn(String sampleReceivedDateColumn) {
        this.sampleReceivedDateColumn = sampleReceivedDateColumn;
    }

    public String getSampleArrivalTimeColumn() {
        return sampleArrivalTimeColumn;
    }

    public void setSampleArrivalTimeColumn(String sampleArrivalTimeColumn) {
        this.sampleArrivalTimeColumn = sampleArrivalTimeColumn;
    }

    public String getReceivedByColumn() {
        return receivedByColumn;
    }

    public void setReceivedByColumn(String receivedByColumn) {
        this.receivedByColumn = receivedByColumn;
    }

    public String getStorageContainerTypeColumn() {
        return storageContainerTypeColumn;
    }

    public void setStorageContainerTypeColumn(String storageContainerTypeColumn) {
        this.storageContainerTypeColumn = storageContainerTypeColumn;
    }

    public String getStorageTemperatureOnArrivalColumn() {
        return storageTemperatureOnArrivalColumn;
    }

    public void setStorageTemperatureOnArrivalColumn(String storageTemperatureOnArrivalColumn) {
        this.storageTemperatureOnArrivalColumn = storageTemperatureOnArrivalColumn;
    }

    public String getConsentStatusColumn() {
        return consentStatusColumn;
    }

    public void setConsentStatusColumn(String consentStatusColumn) {
        this.consentStatusColumn = consentStatusColumn;
    }

    public String getCrfStatusColumn() {
        return crfStatusColumn;
    }

    public void setCrfStatusColumn(String crfStatusColumn) {
        this.crfStatusColumn = crfStatusColumn;
    }

    public String getSampleOriginColumn() {
        return sampleOriginColumn;
    }

    public void setSampleOriginColumn(String sampleOriginColumn) {
        this.sampleOriginColumn = sampleOriginColumn;
    }

    public String getSourceLocationFacilityColumn() {
        return sourceLocationFacilityColumn;
    }

    public void setSourceLocationFacilityColumn(String sourceLocationFacilityColumn) {
        this.sourceLocationFacilityColumn = sourceLocationFacilityColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
