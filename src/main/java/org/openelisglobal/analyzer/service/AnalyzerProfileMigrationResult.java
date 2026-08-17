package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileMigrationAnomaly;

public record AnalyzerProfileMigrationResult(Status status, String analyzerId, String profileId, int profileRevision,
        String siteBindingRevisionId, List<AnalyzerProfileMigrationAnomaly> anomalies) {

    public enum Status {
        MIGRATED, BLOCKED, UNCHANGED
    }

    public AnalyzerProfileMigrationResult {
        anomalies = anomalies == null ? List.of() : List.copyOf(anomalies);
    }

    public static AnalyzerProfileMigrationResult migrated(String analyzerId, String profileId, int profileRevision,
            String siteBindingRevisionId) {
        return new AnalyzerProfileMigrationResult(Status.MIGRATED, analyzerId, profileId, profileRevision,
                siteBindingRevisionId, List.of());
    }

    public static AnalyzerProfileMigrationResult blocked(String analyzerId, String profileId, int profileRevision,
            List<AnalyzerProfileMigrationAnomaly> anomalies) {
        return new AnalyzerProfileMigrationResult(Status.BLOCKED, analyzerId, profileId, profileRevision, null,
                anomalies);
    }

    public static AnalyzerProfileMigrationResult unchanged(String analyzerId, String profileId, int profileRevision,
            String siteBindingRevisionId) {
        return new AnalyzerProfileMigrationResult(Status.UNCHANGED, analyzerId, profileId, profileRevision,
                siteBindingRevisionId, List.of());
    }
}
