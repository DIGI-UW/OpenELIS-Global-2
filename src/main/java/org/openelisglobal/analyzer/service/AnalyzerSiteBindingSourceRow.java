package org.openelisglobal.analyzer.service;

public record AnalyzerSiteBindingSourceRow(String sourceRowKey, String rawValue) {

    public AnalyzerSiteBindingSourceRow {
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
