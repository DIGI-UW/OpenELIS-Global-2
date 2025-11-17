package org.openelisglobal.storage.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form object for StorageRack entity
 */
public class StorageRackForm {

    private String id;

    @NotBlank(message = "Rack label is required")
    @Size(max = 100, message = "Rack label must not exceed 100 characters")
    private String label;

    @NotNull(message = "Rows value is required")
    @Min(value = 0, message = "Rows must be non-negative")
    private Integer rows = 0;

    @NotNull(message = "Columns value is required")
    @Min(value = 0, message = "Columns must be non-negative")
    private Integer columns = 0;

    @Size(max = 50, message = "Position schema hint must not exceed 50 characters")
    private String positionSchemaHint;

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
