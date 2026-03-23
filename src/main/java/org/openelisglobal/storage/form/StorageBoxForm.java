package org.openelisglobal.storage.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageBox entity. Represents a gridded container (e.g.,
 * 96-well plate, sample box) within a rack.
 */
public class StorageBoxForm {

    private String id;

    @NotBlank(message = "Box label is required")
    @Size(max = 100, message = "Box label must not exceed 100 characters")
    private String label;

    @Size(max = 50, message = "Box type must not exceed 50 characters")
    private String type; // e.g., "96-well", "384-well", "9x9"

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Rows must be at least 1")
    private Integer rows;

    @NotNull(message = "Number of columns is required")
    @Min(value = 1, message = "Columns must be at least 1")
    private Integer columns;

    @Size(max = 50, message = "Position schema hint must not exceed 50 characters")
    private String positionSchemaHint; // e.g., "letter-number"

    @NotBlank(message = "Box code is required")
    @Size(max = 10, message = "Box code must not exceed 10 characters")
    private String code;

    private Boolean active;

    @NotBlank(message = "Parent rack ID is required")
    private String parentRackId;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getColumns() {
        return columns;
    }

    public void setColumns(Integer columns) {
        this.columns = columns;
    }

    public String getPositionSchemaHint() {
        return positionSchemaHint;
    }

    public void setPositionSchemaHint(String positionSchemaHint) {
        this.positionSchemaHint = positionSchemaHint;
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

    public String getParentRackId() {
        return parentRackId;
    }

    public void setParentRackId(String parentRackId) {
        this.parentRackId = parentRackId;
    }
}
