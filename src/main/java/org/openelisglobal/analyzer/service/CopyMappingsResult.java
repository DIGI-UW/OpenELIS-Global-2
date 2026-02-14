package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for copy mappings operation
 * 
 * 
 * Contains results of copying mappings from source to target analyzer
 * including: - Number of mappings copied - Number of mappings skipped -
 * Warnings and conflicts
 */
public class CopyMappingsResult {

    private Integer copiedCount;
    private Integer skippedCount;
    private List<String> warnings;
    private List<ConflictDetail> conflicts;

    public CopyMappingsResult() {
        this.copiedCount = 0;
        this.skippedCount = 0;
        this.warnings = new ArrayList<>();
        this.conflicts = new ArrayList<>();
    }

    public Integer getCopiedCount() {
        return copiedCount;
    }

    public void setCopiedCount(Integer copiedCount) {
        this.copiedCount = copiedCount;
    }

    public Integer getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<ConflictDetail> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictDetail> conflicts) {
        this.conflicts = conflicts;
    }

    /**
     * Conflict detail for copy operation
     */
    public static class ConflictDetail {
        private String fieldName;
        private String conflictType;
        private String message;

        public ConflictDetail(String fieldName, String conflictType, String message) {
            this.fieldName = fieldName;
            this.conflictType = conflictType;
            this.message = message;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getConflictType() {
            return conflictType;
        }

        public String getMessage() {
            return message;
        }
    }
}
