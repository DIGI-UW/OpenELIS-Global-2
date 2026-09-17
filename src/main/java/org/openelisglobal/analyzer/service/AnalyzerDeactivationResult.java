package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.valueholder.Analyzer;

public record AnalyzerDeactivationResult(String analyzerId, Analyzer.AnalyzerStatus status, boolean deactivated,
        String failure) {

    static AnalyzerDeactivationResult deactivated(Analyzer analyzer) {
        return new AnalyzerDeactivationResult(analyzer.getId(), analyzer.getStatus(), true, null);
    }

    static AnalyzerDeactivationResult failed(Analyzer analyzer, String failure) {
        return new AnalyzerDeactivationResult(analyzer.getId(), analyzer.getStatus(), false, failure);
    }
}
