package org.openelisglobal.analyzer.service;

public interface AnalyzerProfileMigrationService {

    AnalyzerProfileMigrationResult migrate(String analyzerId, String profileId, int profileRevision, String actor);
}
