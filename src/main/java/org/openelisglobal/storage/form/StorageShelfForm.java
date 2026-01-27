package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageShelf entity
 */
public class StorageShelfForm {

    private String id;

    @NotBlank(message = "Shelf label is required")
    @Size(max = 100, message = "Shelf label must not exceed 100 characters")
    private String label;

    @Size(max = 10, message = "Shelf code must not exceed 10 characters")
    private String code; // Optional - will be auto-generated if not provided

    private Integer capacityLimit;

    private Boolean active = true;

    // Note: parentDeviceId is required for creation but optional for updates
    // (parent cannot be changed after creation, so backend ignores this on PUT)
    private String parentDeviceId;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParentDeviceId() {
        return parentDeviceId;
    }

    public void setParentDeviceId(String parentDeviceId) {
        this.parentDeviceId = parentDeviceId;
    }
}
