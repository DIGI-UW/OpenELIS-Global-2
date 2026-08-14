package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingRevision;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;

public record AnalyzerSiteBindingSnapshot(AnalyzerSiteBinding binding, AnalyzerSiteBindingRevision revision,
        List<AnalyzerSiteBindingTest> tests) {

    public AnalyzerSiteBindingSnapshot {
        tests = List.copyOf(tests);
    }
}
