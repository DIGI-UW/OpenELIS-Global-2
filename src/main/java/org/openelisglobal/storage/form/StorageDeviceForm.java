package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageDevice entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class StorageDeviceForm {

    private String id;

    @NotBlank(message = "Device name is required")
    @Size(max = 255, message = "Device name must not exceed 255 characters")
    private String name;

    @Size(max = 50, message = "Device code must not exceed 50 characters")
    private String code; // Optional - will be auto-generated if not provided

    @NotBlank(message = "Device type is required")
    @Pattern(regexp = "freezer|refrigerator|cabinet|other", message = "Device type must be one of: freezer, refrigerator, cabinet, other")
    private String type;

    private Double temperatureSetting;

    private Integer capacityLimit;

    private Boolean active = true;

    @NotBlank(message = "Parent room ID is required")
    private String parentRoomId;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getParentRoomId() {
        return parentRoomId;
    }

    public void setParentRoomId(String parentRoomId) {
        this.parentRoomId = parentRoomId;
    }
}
