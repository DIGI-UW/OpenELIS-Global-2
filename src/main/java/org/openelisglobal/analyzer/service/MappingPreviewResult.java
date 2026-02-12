package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for mapping preview operation
 * 
 */
public class MappingPreviewResult {
    private List<ParsedField> parsedFields;
    private List<AppliedMapping> appliedMappings;
    private EntityPreview entityPreview;
    private List<String> warnings;
    private List<String> errors;

    public MappingPreviewResult() {
        this.parsedFields = new ArrayList<>();
        this.appliedMappings = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public List<ParsedField> getParsedFields() {
        return parsedFields;
    }

    public void setParsedFields(List<ParsedField> parsedFields) {
        this.parsedFields = parsedFields;
    }

    public List<AppliedMapping> getAppliedMappings() {
        return appliedMappings;
    }

    public void setAppliedMappings(List<AppliedMapping> appliedMappings) {
        this.appliedMappings = appliedMappings;
    }

    public EntityPreview getEntityPreview() {
        return entityPreview;
    }

    public void setEntityPreview(EntityPreview entityPreview) {
        this.entityPreview = entityPreview;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
