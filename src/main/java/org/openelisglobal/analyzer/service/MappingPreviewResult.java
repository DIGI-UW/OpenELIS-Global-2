package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result object for mapping preview operation
 * 
 */
public class MappingPreviewResult {
    private List<ParsedField> parsedFields;
    private List<AppliedMapping> appliedMappings;
    private EntityPreview entityPreview;
    private List<Map<String, Object>> transformResults;
    private Map<String, Object> qcRuleEvaluation;
    private List<Map<String, Object>> extractionApplied;
    private List<Map<String, Object>> flagMappings;
    private List<String> warnings;
    private List<String> errors;

    public MappingPreviewResult() {
        this.parsedFields = new ArrayList<>();
        this.appliedMappings = new ArrayList<>();
        this.transformResults = new ArrayList<>();
        this.qcRuleEvaluation = new HashMap<>();
        this.extractionApplied = new ArrayList<>();
        this.flagMappings = new ArrayList<>();
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

    public List<Map<String, Object>> getTransformResults() {
        return transformResults;
    }

    public void setTransformResults(List<Map<String, Object>> transformResults) {
        this.transformResults = transformResults;
    }

    public Map<String, Object> getQcRuleEvaluation() {
        return qcRuleEvaluation;
    }

    public void setQcRuleEvaluation(Map<String, Object> qcRuleEvaluation) {
        this.qcRuleEvaluation = qcRuleEvaluation;
    }

    public List<Map<String, Object>> getExtractionApplied() {
        return extractionApplied;
    }

    public void setExtractionApplied(List<Map<String, Object>> extractionApplied) {
        this.extractionApplied = extractionApplied;
    }

    public List<Map<String, Object>> getFlagMappings() {
        return flagMappings;
    }

    public void setFlagMappings(List<Map<String, Object>> flagMappings) {
        this.flagMappings = flagMappings;
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
