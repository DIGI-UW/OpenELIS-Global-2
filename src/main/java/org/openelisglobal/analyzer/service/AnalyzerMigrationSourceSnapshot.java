package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMigrationSourceSnapshot(String fingerprint, List<AnalyzerSource> analyzers) {

    public record AnalyzerSource(String analyzerId, String fingerprint) {
    }
}
