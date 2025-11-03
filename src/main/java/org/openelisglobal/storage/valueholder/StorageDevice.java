package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * StorageDevice entity - Storage equipment (freezers, refrigerators, cabinets)
 * Maps to FHIR Location resource with physicalType = "ve" (vehicle/equipment)
 */
public class StorageDevice extends BaseObject<String> {

    public enum DeviceType {
        FREEZER("freezer"),
        REFRIGERATOR("refrigerator"),
        CABINET("cabinet"),
        OTHER("other");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DeviceType fromValue(String value) {
            for (DeviceType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid device type: " + value);
        }
    }

    private String id;
    private UUID fhirUuid;
    private String name;
    private String code;
    private DeviceType type;
    private Double temperatureSetting;
    private Integer capacityLimit;
    private Boolean active;
    private StorageRoom parentRoom;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public Double getTemperatureSetting() {
        return temperatureSetting;
    }

    public void setTemperatureSetting(Double temperatureSetting) {
        this.temperatureSetting = temperatureSetting;
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

    public StorageRoom getParentRoom() {
        return parentRoom;
    }

    public void setParentRoom(StorageRoom parentRoom) {
        this.parentRoom = parentRoom;
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

    public String getTypeAsString() {
        return type != null ? type.getValue() : null;
    }
}
