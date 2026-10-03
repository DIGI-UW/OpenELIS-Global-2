package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;

public record AnalyzerSiteBindingSnapshot(AnalyzerSiteBinding binding, AnalyzerSiteBindingRevision revision,
        List<AnalyzerSiteBindingTest> tests, List<AnalyzerSiteBindingResult> results) {

    public AnalyzerSiteBindingSnapshot {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
