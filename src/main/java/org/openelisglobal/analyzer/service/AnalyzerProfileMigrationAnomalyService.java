package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;

public interface AnalyzerProfileMigrationAnomalyService {

    List<AnalyzerProfileMigrationAnomaly> findOpen(String analyzerId);

    List<AnalyzerProfileMigrationAnomaly> replaceOpen(Analyzer analyzer,
            List<AnalyzerProfileMigrationAnomalyDraft> findings, String actor);

    void resolveAll(String analyzerId, String actor);
}
