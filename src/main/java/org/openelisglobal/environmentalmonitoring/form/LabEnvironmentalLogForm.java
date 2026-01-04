package org.openelisglobal.environmentalmonitoring.form;

import java.sql.Timestamp;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for LabEnvironmentalLog client-server communication.
 *
 * Handles form validation and data transfer between frontend and controller.
 * Validation annotations validate client input before processing.
 */
public class LabEnvironmentalLogForm {

    @NotNull(message = "Storage unit type is required")
    @Size(min = 1, max = 30, message = "Storage unit type must be 1-30 characters")
    private String storageUnitType;

    @NotNull(message = "Storage unit ID is required")
    @Size(min = 1, max = 100, message = "Storage unit ID must be 1-100 characters")
    private String storageUnitId;

    @Size(max = 10, message = "Interval type must be 10 characters or less")
    private String intervalType; // AM or PM

    @NotNull(message = "Temperature value is required")
    private Double temperatureValue;

    @Size(max = 5, message = "Temperature unit must be 5 characters or less")
    private String temperatureUnit; // C or F

    private Double humidityValue; // Optional humidity reading

    @Size(max = 255, message = "Checked by must be 255 characters or less")
    private String checkedBy;

    private Timestamp checkedDateTime;

    @Size(max = 1000, message = "Notes must be 1000 characters or less")
    private String notes;

    // Additional fields for Movable Fridge context
    @Size(max = 100, message = "Sample type must be 100 characters or less")
    private String sampleType; // Required when storageUnitType = MOVABLE_FRIDGE

    @Size(max = 255, message = "Project name must be 255 characters or less")
    private String projectName; // Required when storageUnitType = MOVABLE_FRIDGE

    @Size(max = 100, message = "Sample ID must be 100 characters or less")
    private String sampleId; // Required when storageUnitType = MOVABLE_FRIDGE

    @Size(max = 500, message = "Additional details must be 500 characters or less")
    private String additionalDetails; // Any other relevant sample details

    public LabEnvironmentalLogForm() {
        // Default constructor
    }

    public String getStorageUnitType() {
        return storageUnitType;
    }

    public void setStorageUnitType(String storageUnitType) {
        this.storageUnitType = storageUnitType;
    }

    public String getStorageUnitId() {
        return storageUnitId;
    }

    public void setStorageUnitId(String storageUnitId) {
        this.storageUnitId = storageUnitId;
    }

    public String getIntervalType() {
        return intervalType;
    }

    public void setIntervalType(String intervalType) {
        this.intervalType = intervalType;
    }

    public Double getTemperatureValue() {
        return temperatureValue;
    }

    public void setTemperatureValue(Double temperatureValue) {
        this.temperatureValue = temperatureValue;
    }

    public String getTemperatureUnit() {
        return temperatureUnit;
    }

    public void setTemperatureUnit(String temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public Double getHumidityValue() {
        return humidityValue;
    }

    public void setHumidityValue(Double humidityValue) {
        this.humidityValue = humidityValue;
    }

    public String getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(String checkedBy) {
        this.checkedBy = checkedBy;
    }

    public Timestamp getCheckedDateTime() {
        return checkedDateTime;
    }

    public void setCheckedDateTime(Timestamp checkedDateTime) {
        this.checkedDateTime = checkedDateTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }
}