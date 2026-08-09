package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for InventoryLot storage assignment (OGC-657). Mirrors
 * {@link SampleAssignmentForm}, keyed by inventoryLotId instead of
 * sampleItemId.
 */
public class InventoryLotAssignmentForm {

    @NotBlank(message = "InventoryLot ID is required")
    private String inventoryLotId;

    @NotBlank(message = "Location ID is required")
    private String locationId; // Can be room/device/shelf/rack/box ID

    @NotBlank(message = "Location type is required")
    private String locationType; // Enum: 'room', 'device', 'shelf', 'rack', 'box'

    @Size(max = 50, message = "Position coordinate must not exceed 50 characters")
    private String positionCoordinate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public String getInventoryLotId() {
        return inventoryLotId;
    }

    public void setInventoryLotId(String inventoryLotId) {
        this.inventoryLotId = inventoryLotId;
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
