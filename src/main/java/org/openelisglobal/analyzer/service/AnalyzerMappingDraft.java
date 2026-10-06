package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMappingDraft(List<AnalyzerMappingTestDraft> tests, List<AnalyzerMappingResultDraft> results) {

    public AnalyzerMappingDraft {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
