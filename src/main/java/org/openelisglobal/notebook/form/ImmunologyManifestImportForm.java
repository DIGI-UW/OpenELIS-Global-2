package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Immunology laboratory manifest CSV import. Allows column
 * mapping for immunology-specific reception metadata.
 *
 * Required fields: uniqueParentSampleId, projectNameId,
 * deliveryManifestReference, collectionDateTime, receptionDateTime,
 * sourceOrigin
 *
 * Sample Type: sampleType (validated against Immunology lab types)
 *
 * Optional fields: sampleVolume, storageConditionOnArrival,
 * transportTemperature, receivingPersonnelName, manifestVerificationStatus,
 * patientId, notes
 *
 * Auto-generated (not in CSV): accessionNumber, barcodeQrCode
 */
public class ImmunologyManifestImportForm {

    private Integer notebookId;

    // Required fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String uniqueParentSampleIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String projectNameIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String deliveryManifestReferenceColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String collectionDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receptionDateTimeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sourceOriginColumn;

    // Sample type field (validated against Immunology lab types)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    // Optional fields
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleVolumeColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionOnArrivalColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String transportTemperatureColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivingPersonnelNameColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String manifestVerificationStatusColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String patientIdColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String notesColumn;

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    // Required field getters/setters
    public String getUniqueParentSampleIdColumn() {
        return uniqueParentSampleIdColumn;
    }

    public void setUniqueParentSampleIdColumn(String uniqueParentSampleIdColumn) {
        this.uniqueParentSampleIdColumn = uniqueParentSampleIdColumn;
    }

    public String getProjectNameIdColumn() {
        return projectNameIdColumn;
    }

    public void setProjectNameIdColumn(String projectNameIdColumn) {
        this.projectNameIdColumn = projectNameIdColumn;
    }

    public String getDeliveryManifestReferenceColumn() {
        return deliveryManifestReferenceColumn;
    }

    public void setDeliveryManifestReferenceColumn(String deliveryManifestReferenceColumn) {
        this.deliveryManifestReferenceColumn = deliveryManifestReferenceColumn;
    }

    public String getCollectionDateTimeColumn() {
        return collectionDateTimeColumn;
    }

    public void setCollectionDateTimeColumn(String collectionDateTimeColumn) {
        this.collectionDateTimeColumn = collectionDateTimeColumn;
    }

    public String getReceptionDateTimeColumn() {
        return receptionDateTimeColumn;
    }

    public void setReceptionDateTimeColumn(String receptionDateTimeColumn) {
        this.receptionDateTimeColumn = receptionDateTimeColumn;
    }

    public String getSourceOriginColumn() {
        return sourceOriginColumn;
    }

    public void setSourceOriginColumn(String sourceOriginColumn) {
        this.sourceOriginColumn = sourceOriginColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    // Optional field getters/setters
    public String getSampleVolumeColumn() {
        return sampleVolumeColumn;
    }

    public void setSampleVolumeColumn(String sampleVolumeColumn) {
        this.sampleVolumeColumn = sampleVolumeColumn;
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

    public String getPatientIdColumn() {
        return patientIdColumn;
    }

    public void setPatientIdColumn(String patientIdColumn) {
        this.patientIdColumn = patientIdColumn;
    }

    public String getNotesColumn() {
        return notesColumn;
    }

    public void setNotesColumn(String notesColumn) {
        this.notesColumn = notesColumn;
    }
}
