package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;

public record AnalyzerActivationResult(String analyzerId, Analyzer.AnalyzerStatus status, boolean ready,
        boolean activated, List<AnalyzerActivationBlocker> blockers) {

    public AnalyzerActivationResult {
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }

    static AnalyzerActivationResult activated(Analyzer analyzer) {
        return new AnalyzerActivationResult(analyzer.getId(), analyzer.getStatus(), true, true, List.of());
    }

    static AnalyzerActivationResult ready(Analyzer analyzer) {
        return new AnalyzerActivationResult(analyzer.getId(), analyzer.getStatus(), true,
                analyzer.getStatus() == Analyzer.AnalyzerStatus.ACTIVE, List.of());
    }

    static AnalyzerActivationResult blocked(Analyzer analyzer, List<AnalyzerActivationBlocker> blockers) {
        return new AnalyzerActivationResult(analyzer.getId(), analyzer.getStatus(), false, false, blockers);
    }
}
