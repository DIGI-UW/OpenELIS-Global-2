package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for StoragePosition entity
 */
public class StoragePositionForm {

    private String id;

    @NotBlank(message = "Position coordinate is required")
    @Size(max = 50, message = "Position coordinate must not exceed 50 characters")
    private String coordinate;

    private Integer rowIndex;

    private Integer columnIndex;

    @NotBlank(message = "Parent rack ID is required")
    private String parentRackId;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getParentRackId() {
        return parentRackId;
    }

    public void setParentRackId(String parentRackId) {
        this.parentRackId = parentRackId;
    }
}
