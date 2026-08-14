package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly.Code;

public record AnalyzerProfileMigrationAnomalyDraft(Code code, String legacySourceKey, String legacyTestId,
        String detail) {

    public AnalyzerProfileMigrationAnomalyDraft {
        if (code == null) {
            throw new IllegalArgumentException("migration anomaly code is required");
        }
        legacySourceKey = trimToNull(legacySourceKey);
        legacyTestId = trimToNull(legacyTestId);
        detail = requireText(detail, "migration anomaly detail is required");
    }

    public String evidenceKey() {
        return evidenceKey(code, legacySourceKey, legacyTestId);
    }

    public static String evidenceKey(Code code, String legacySourceKey, String legacyTestId) {
        if (code == null) {
            throw new IllegalArgumentException("migration anomaly code is required");
        }
        return code.name() + "|" + evidencePart(legacySourceKey) + "|" + evidencePart(legacyTestId);
    }

    private static String evidencePart(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? "-" : normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
