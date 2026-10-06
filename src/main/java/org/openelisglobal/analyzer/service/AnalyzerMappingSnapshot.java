package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;

public record AnalyzerMappingSnapshot(AnalyzerSiteBinding binding, AnalyzerMapping revision,
        List<AnalyzerMappingTest> tests, List<AnalyzerMappingResult> results) {

    public AnalyzerMappingSnapshot {
        tests = tests == null ? List.of() : List.copyOf(tests);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
