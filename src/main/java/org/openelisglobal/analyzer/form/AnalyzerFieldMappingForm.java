package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping.MappingType;
import org.openelisglobal.analyzer.valueholder.AnalyzerFieldMapping.OpenELISFieldType;

/**
 * Form object for AnalyzerFieldMapping entity - used for REST API input
 * validation Following OpenELIS pattern: Form objects for transport, entities
 * for persistence
 */
public class AnalyzerFieldMappingForm {

    private String id;

    @NotBlank(message = "Analyzer field ID is required")
    private String analyzerFieldId;

    @NotBlank(message = "OpenELIS field ID is required")
    private String openelisFieldId;

    @NotNull(message = "OpenELIS field type is required")
    private OpenELISFieldType openelisFieldType;

    @NotNull(message = "Mapping type is required")
    private MappingType mappingType;

    private Boolean isRequired = false;

    private Boolean isActive = false;

    @jakarta.validation.constraints.Size(max = 50, message = "Specimen type constraint must not exceed 50 characters")
    private String specimenTypeConstraint;

    @jakarta.validation.constraints.Size(max = 50, message = "Panel constraint must not exceed 50 characters")
    private String panelConstraint;

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

    public String getOpenelisFieldId() {
        return openelisFieldId;
    }

    public void setOpenelisFieldId(String openelisFieldId) {
        this.openelisFieldId = openelisFieldId;
    }

    public OpenELISFieldType getOpenelisFieldType() {
        return openelisFieldType;
    }

    public void setOpenelisFieldType(OpenELISFieldType openelisFieldType) {
        this.openelisFieldType = openelisFieldType;
    }

    public MappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(MappingType mappingType) {
        this.mappingType = mappingType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSpecimenTypeConstraint() {
        return specimenTypeConstraint;
    }

    public void setSpecimenTypeConstraint(String specimenTypeConstraint) {
        this.specimenTypeConstraint = specimenTypeConstraint;
    }

    public String getPanelConstraint() {
        return panelConstraint;
    }

    public void setPanelConstraint(String panelConstraint) {
        this.panelConstraint = panelConstraint;
    }
}
