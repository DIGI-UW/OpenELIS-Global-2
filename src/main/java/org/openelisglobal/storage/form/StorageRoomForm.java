package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageRoom entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class StorageRoomForm {

    private String id;

    @NotBlank(message = "Room name is required")
    @Size(max = 255, message = "Room name must not exceed 255 characters")
    private String name;

    @Size(max = 50, message = "Room code must not exceed 50 characters")
    private String code; // Optional - will be auto-generated if not provided

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Boolean active = true;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
