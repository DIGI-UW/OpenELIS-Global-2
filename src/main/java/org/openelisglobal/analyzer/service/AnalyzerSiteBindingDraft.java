package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerSiteBindingDraft(String bridgeProfileId, int bridgeProfileRevision,
        List<AnalyzerSiteBindingTestDraft> tests) {

    public AnalyzerSiteBindingDraft {
        bridgeProfileId = trimToNull(bridgeProfileId);
        tests = tests == null ? List.of() : List.copyOf(tests);
    }

    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
