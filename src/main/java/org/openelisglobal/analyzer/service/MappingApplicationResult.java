package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for mapping application operations
 * 
 */
public class MappingApplicationResult {

    private List<String> transformedLines;
    private List<String> unmappedFields;
    private List<String> errors;
    private boolean hasMappings;
    private boolean success;

    public MappingApplicationResult() {
        this.transformedLines = new ArrayList<>();
        this.unmappedFields = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.hasMappings = false;
        this.success = false;
    }

    public List<String> getTransformedLines() {
        return transformedLines;
    }

    public void setTransformedLines(List<String> transformedLines) {
        this.transformedLines = transformedLines;
    }

    public List<String> getUnmappedFields() {
        return unmappedFields;
    }

    public void setUnmappedFields(List<String> unmappedFields) {
        this.unmappedFields = unmappedFields;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public boolean hasMappings() {
        return hasMappings;
    }

    public void setHasMappings(boolean hasMappings) {
        this.hasMappings = hasMappings;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
