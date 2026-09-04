package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest;

public interface AnalyzerMigrationService {

    AnalyzerMigrationManifest plan(AnalyzerMigrationPlanRequest request, String actor);

    AnalyzerMigrationManifest apply(AnalyzerMigrationManifest plan, String actor);

    AnalyzerMigrationManifest verify(AnalyzerMigrationManifest apply);
}
