package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * StoragePosition entity - Specific location within a rack
 * Maps to FHIR Location resource with occupancy extension
 */
public class StoragePosition extends BaseObject<String> {

    private String id;
    private UUID fhirUuid;
    private String coordinate;
    private Integer rowIndex;
    private Integer columnIndex;
    private Boolean occupied;
    private StorageRack parentRack;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Integer columnIndex) {
        this.columnIndex = columnIndex;
    }

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public StorageRack getParentRack() {
        return parentRack;
    }

    public void setParentRack(StorageRack parentRack) {
        this.parentRack = parentRack;
    }

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Helper methods for FHIR transform
    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }
}
