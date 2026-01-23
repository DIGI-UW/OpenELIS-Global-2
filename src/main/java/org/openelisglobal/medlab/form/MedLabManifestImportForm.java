/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Form for MedLab manifest import column mappings.
 *
 * <p>
 * Maps 13 CSV columns to sample fields per spec FR-010 to FR-014:
 * <ul>
 * <li>Required: sampleId, sampleType, containerType, quantity, unitOfMeasure,
 * collectionSource, collector, collectionDate, collectionTime
 * <li>Optional: customLabel, orderId, patientId, notes
 * </ul>
 */
public class MedLabManifestImportForm {

    // Required fields (FR-010 to FR-014)
    @NotBlank(message = "Sample ID column mapping is required")
    private String sampleIdColumn;

    @NotBlank(message = "Sample Type column mapping is required")
    private String sampleTypeColumn;

    @NotBlank(message = "Container Type column mapping is required")
    private String containerTypeColumn;

    @NotBlank(message = "Quantity column mapping is required")
    private String quantityColumn;

    @NotBlank(message = "Unit of Measure column mapping is required")
    private String unitOfMeasureColumn;

    @NotBlank(message = "Collection Source column mapping is required")
    private String collectionSourceColumn;

    @NotBlank(message = "Collector column mapping is required")
    private String collectorColumn;

    @NotBlank(message = "Collection Date column mapping is required")
    private String collectionDateColumn;

    @NotBlank(message = "Collection Time column mapping is required")
    private String collectionTimeColumn;

    // Optional fields
    private String customLabelColumn;
    private String orderIdColumn;
    private String patientIdColumn;
    private String notesColumn;

    // Date format (default: yyyy-MM-dd)
    private String dateFormat = "yyyy-MM-dd";

    // Time format (default: HH:mm)
    private String timeFormat = "HH:mm";

    public MedLabManifestImportForm() {
    }

    // Required field getters/setters
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

    public String getContainerTypeColumn() {
        return containerTypeColumn;
    }

    public void setContainerTypeColumn(String containerTypeColumn) {
        this.containerTypeColumn = containerTypeColumn;
    }

    public String getQuantityColumn() {
        return quantityColumn;
    }

    public void setQuantityColumn(String quantityColumn) {
        this.quantityColumn = quantityColumn;
    }

    public String getUnitOfMeasureColumn() {
        return unitOfMeasureColumn;
    }

    public void setUnitOfMeasureColumn(String unitOfMeasureColumn) {
        this.unitOfMeasureColumn = unitOfMeasureColumn;
    }

    public String getCollectionSourceColumn() {
        return collectionSourceColumn;
    }

    public void setCollectionSourceColumn(String collectionSourceColumn) {
        this.collectionSourceColumn = collectionSourceColumn;
    }

    public String getCollectorColumn() {
        return collectorColumn;
    }

    public void setCollectorColumn(String collectorColumn) {
        this.collectorColumn = collectorColumn;
    }

    public String getCollectionDateColumn() {
        return collectionDateColumn;
    }

    public void setCollectionDateColumn(String collectionDateColumn) {
        this.collectionDateColumn = collectionDateColumn;
    }

    public String getCollectionTimeColumn() {
        return collectionTimeColumn;
    }

    public void setCollectionTimeColumn(String collectionTimeColumn) {
        this.collectionTimeColumn = collectionTimeColumn;
    }

    // Optional field getters/setters
    public String getCustomLabelColumn() {
        return customLabelColumn;
    }

    public void setCustomLabelColumn(String customLabelColumn) {
        this.customLabelColumn = customLabelColumn;
    }

    public String getOrderIdColumn() {
        return orderIdColumn;
    }

    public void setOrderIdColumn(String orderIdColumn) {
        this.orderIdColumn = orderIdColumn;
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

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }
}
