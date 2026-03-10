package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for QualitativeResultMapping entity - used for REST API input
 * validation Following OpenELIS pattern: Form objects for transport, entities
 * for persistence
 */
public class QualitativeResultMappingForm {

    private String id;

    @NotBlank(message = "Analyzer field ID is required")
    private String analyzerFieldId;

    @NotBlank(message = "Analyzer value is required")
    @Size(min = 1, max = 100, message = "Analyzer value must be between 1 and 100 characters")
    private String analyzerValue;

    @NotBlank(message = "OpenELIS code is required")
    @Size(min = 1, max = 100, message = "OpenELIS code must be between 1 and 100 characters")
    private String openelisCode;

    private Boolean isDefault = false;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnalyzerFieldId() {
        return analyzerFieldId;
    }

    public void setAnalyzerFieldId(String analyzerFieldId) {
        this.analyzerFieldId = analyzerFieldId;
    }

    public String getAnalyzerValue() {
        return analyzerValue;
    }

    public void setAnalyzerValue(String analyzerValue) {
        this.analyzerValue = analyzerValue;
    }

    public String getOpenelisCode() {
        return openelisCode;
    }

    public void setOpenelisCode(String openelisCode) {
        this.openelisCode = openelisCode;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
