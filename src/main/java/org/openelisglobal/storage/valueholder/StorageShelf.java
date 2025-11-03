package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * StorageShelf entity - Storage shelf within a device
 * Maps to FHIR Location resource with physicalType = "co" (container)
 */
public class StorageShelf extends BaseObject<String> {

    private String id;
    private UUID fhirUuid;
    private String label;
    private Integer capacityLimit;
    private Boolean active;
    private StorageDevice parentDevice;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getCapacityLimit() {
        return capacityLimit;
    }

    public void setCapacityLimit(Integer capacityLimit) {
        this.capacityLimit = capacityLimit;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public StorageDevice getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(StorageDevice parentDevice) {
        this.parentDevice = parentDevice;
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
