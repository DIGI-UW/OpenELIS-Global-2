package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerSiteBindingDraft(List<AnalyzerSiteBindingTestDraft> tests,
        List<AnalyzerSiteBindingResultDraft> results) {

    public AnalyzerSiteBindingDraft {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
