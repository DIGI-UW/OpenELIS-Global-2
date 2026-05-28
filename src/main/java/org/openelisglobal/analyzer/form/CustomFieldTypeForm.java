package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form DTO for CustomFieldType CRUD operations
 * 
 */
public class CustomFieldTypeForm {

    private String id;

    @NotNull(message = "Type name is required")
    @Size(min = 1, max = 50, message = "Type name must be between 1 and 50 characters")
    private String typeName;

    @NotNull(message = "Display name is required")
    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 255, message = "Validation pattern must not exceed 255 characters")
    private String validationPattern;

    private String valueRangeMin;

    private String valueRangeMax;

    @Size(max = 255, message = "Allowed characters must not exceed 255 characters")
    private String allowedCharacters;

    private Boolean isActive = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getValidationPattern() {
        return validationPattern;
    }

    public void setValidationPattern(String validationPattern) {
        this.validationPattern = validationPattern;
    }

    public String getValueRangeMin() {
        return valueRangeMin;
    }

    public void setValueRangeMin(String valueRangeMin) {
        this.valueRangeMin = valueRangeMin;
    }

    public String getValueRangeMax() {
        return valueRangeMax;
    }

    public void setValueRangeMax(String valueRangeMax) {
        this.valueRangeMax = valueRangeMax;
    }

    public String getAllowedCharacters() {
        return allowedCharacters;
    }

    public void setAllowedCharacters(String allowedCharacters) {
        this.allowedCharacters = allowedCharacters;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
