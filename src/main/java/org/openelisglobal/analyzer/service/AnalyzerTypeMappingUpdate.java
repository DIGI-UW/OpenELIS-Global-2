package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerTypeMappingUpdate(String baseBindingFingerprint, List<AnalyzerSiteBindingTestDraft> tests,
        List<AnalyzerSiteBindingResultDraft> results) {

    public AnalyzerTypeMappingUpdate {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }

    public AnalyzerSiteBindingDraft toDraft() {
        return new AnalyzerSiteBindingDraft(tests, results);
    }
}
