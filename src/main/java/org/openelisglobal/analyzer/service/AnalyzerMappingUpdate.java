package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMappingUpdate(String baseBindingFingerprint, List<AnalyzerMappingTestDraft> tests,
        List<AnalyzerMappingResultDraft> results) {

    public AnalyzerMappingUpdate {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }

    public AnalyzerMappingDraft toDraft() {
        return new AnalyzerMappingDraft(tests, results);
    }
}
