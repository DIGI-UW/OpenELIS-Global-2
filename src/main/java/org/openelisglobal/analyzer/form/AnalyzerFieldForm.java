package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.openelisglobal.analyzer.valueholder.AnalyzerField.FieldType;

/**
 * Form object for AnalyzerField entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class AnalyzerFieldForm {

    private String id;

    @NotBlank(message = "Analyzer ID is required")
    private String analyzerId;

    @NotBlank(message = "Field name is required")
    @Size(min = 1, max = 255, message = "Field name must be between 1 and 255 characters")
    private String fieldName;

    @Size(max = 50, message = "ASTM reference must not exceed 50 characters")
    private String astmRef;

    @NotNull(message = "Field type is required")
    private FieldType fieldType;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    private Boolean isActive = true;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getAstmRef() {
        return astmRef;
    }

    public void setAstmRef(String astmRef) {
        this.astmRef = astmRef;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
