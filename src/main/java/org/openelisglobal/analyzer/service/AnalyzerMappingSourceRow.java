package org.openelisglobal.analyzer.service;

/**
 * A row an operator confirms or excludes: a record (code and sub-identity,
 * empty for the main result) or one of its answers.
 */
public record AnalyzerMappingSourceRow(String sourceRowKey, String rawValue, String subIdentity) {

    public AnalyzerMappingSourceRow(String sourceRowKey, String rawValue) {
        this(sourceRowKey, rawValue, "");
    }

    public AnalyzerMappingSourceRow {
        subIdentity = subIdentity == null ? "" : subIdentity;
        if (sourceRowKey == null || sourceRowKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Source row key is required");
        }
        sourceRowKey = sourceRowKey.trim();
        if (rawValue != null) {
            if (rawValue.trim().isEmpty()) {
                throw new IllegalArgumentException("Raw result value cannot be blank");
            }
            rawValue = rawValue.trim();
        }
    }
}
