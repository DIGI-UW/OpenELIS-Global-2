package org.openelisglobal.notebook.form;

import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for Analytics Laboratory manifest CSV import operations.
 * Contains Analytics-specific data points that are hardcoded for this workflow.
 *
 * Analytics Laboratory Data Points (Page 1 - Sample Creation):
 * - Sample Identity: Sample Identifier, Barcode/QR code
 * - Sample Category: Bioanalytical, Pharmaceutical analysis
 * - Sample Type: API, Tablet, Capsule, Suspension, Injection, Cream/ointment, Processed biological sample, Other
 * - Sample Source: Medical Laboratory, Researcher, External client
 * - Sample Context: Requesting unit/client, Study/project ID
 * - Storage & Handling: Storage condition, Received date & time, Received by
 */
public class AnalyticsManifestImportForm {

    private Integer notebookId;

    // Sample Identity
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdentifierColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String barcodeColumn;

    // Sample Category (Bioanalytical or Pharmaceutical)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleCategoryColumn;

    // Sample Type (API, Tablet, Capsule, etc.)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeColumn;

    // Sample Source (Medical Laboratory, Researcher, External client)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleSourceColumn;

    // Requesting unit/client
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requestingUnitColumn;

    // Requested tests (comma-separated: Assay, Dissolution, Disintegration, etc.)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String requestedTestsColumn;

    // Study/project ID
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String studyProjectIdColumn;

    // Storage condition
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String storageConditionColumn;

    // Received date & time
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedDateTimeColumn;

    // Received by
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String receivedByColumn;

    // Date format for parsing dates
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String dateFormat;

    // Getters and Setters

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public String getSampleIdentifierColumn() {
        return sampleIdentifierColumn;
    }

    public void setSampleIdentifierColumn(String sampleIdentifierColumn) {
        this.sampleIdentifierColumn = sampleIdentifierColumn;
    }

    public String getBarcodeColumn() {
        return barcodeColumn;
    }

    public void setBarcodeColumn(String barcodeColumn) {
        this.barcodeColumn = barcodeColumn;
    }

    public String getSampleCategoryColumn() {
        return sampleCategoryColumn;
    }

    public void setSampleCategoryColumn(String sampleCategoryColumn) {
        this.sampleCategoryColumn = sampleCategoryColumn;
    }

    public String getSampleTypeColumn() {
        return sampleTypeColumn;
    }

    public void setSampleTypeColumn(String sampleTypeColumn) {
        this.sampleTypeColumn = sampleTypeColumn;
    }

    public String getSampleSourceColumn() {
        return sampleSourceColumn;
    }

    public void setSampleSourceColumn(String sampleSourceColumn) {
        this.sampleSourceColumn = sampleSourceColumn;
    }

    public String getRequestingUnitColumn() {
        return requestingUnitColumn;
    }

    public void setRequestingUnitColumn(String requestingUnitColumn) {
        this.requestingUnitColumn = requestingUnitColumn;
    }

    public String getRequestedTestsColumn() {
        return requestedTestsColumn;
    }

    public void setRequestedTestsColumn(String requestedTestsColumn) {
        this.requestedTestsColumn = requestedTestsColumn;
    }

    public String getStudyProjectIdColumn() {
        return studyProjectIdColumn;
    }

    public void setStudyProjectIdColumn(String studyProjectIdColumn) {
        this.studyProjectIdColumn = studyProjectIdColumn;
    }

    public String getStorageConditionColumn() {
        return storageConditionColumn;
    }

    public void setStorageConditionColumn(String storageConditionColumn) {
        this.storageConditionColumn = storageConditionColumn;
    }

    public String getReceivedDateTimeColumn() {
        return receivedDateTimeColumn;
    }

    public void setReceivedDateTimeColumn(String receivedDateTimeColumn) {
        this.receivedDateTimeColumn = receivedDateTimeColumn;
    }

    public String getReceivedByColumn() {
        return receivedByColumn;
    }

    public void setReceivedByColumn(String receivedByColumn) {
        this.receivedByColumn = receivedByColumn;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}
