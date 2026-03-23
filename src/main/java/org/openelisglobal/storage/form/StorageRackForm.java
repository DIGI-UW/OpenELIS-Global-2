package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageRack entity. Note: Racks are simple containers - grid
 * dimensions are now on StorageBox (the gridded container).
 */
public class StorageRackForm {

    private String id;

    @NotBlank(message = "Rack label is required")
    @Size(max = 100, message = "Rack label must not exceed 100 characters")
    private String label;

    @Size(max = 10, message = "Short code must not exceed 10 characters")
    private String code; // Renamed from shortCode per spec Session 2025-11-16 simplification

    private Boolean active = true;

    @NotBlank(message = "Parent shelf ID is required")
    private String parentShelfId;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getParentShelfId() {
        return parentShelfId;
    }

    public void setParentShelfId(String parentShelfId) {
        this.parentShelfId = parentShelfId;
    }
}
