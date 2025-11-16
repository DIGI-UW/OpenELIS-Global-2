package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for SampleItem storage assignment Supports flexible hierarchy:
 * assign to device/shelf/rack level with optional position coordinate
 */
public class SampleAssignmentForm {

    @NotBlank(message = "SampleItem ID is required")
    private String sampleItemId;

    // Simplified polymorphic location
    @NotBlank(message = "Location ID is required")
    private String locationId; // Can be device/shelf/rack ID

    @NotBlank(message = "Location type is required")
    private String locationType; // Enum: 'device', 'shelf', 'rack'

    @Size(max = 50, message = "Position coordinate must not exceed 50 characters")
    private String positionCoordinate; // Optional text-based coordinate (position is just text, not an entity)

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Getters and Setters

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getPositionCoordinate() {
        return positionCoordinate;
    }

    public void setPositionCoordinate(String positionCoordinate) {
        this.positionCoordinate = positionCoordinate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
