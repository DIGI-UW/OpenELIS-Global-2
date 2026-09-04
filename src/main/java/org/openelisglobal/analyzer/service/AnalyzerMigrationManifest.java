package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerMigrationManifest(String schemaVersion, String runId, Mode mode, String sourceSnapshotFingerprint,
        String startedAt, String completedAt, List<Outcome> outcomes) {

    public enum Mode {
        PLAN, APPLY, VERIFY
    }

    public enum Status {
        READY, MIGRATED, NEEDS_CORRECTION, INTENTIONALLY_EXCLUDED
    }

    public record ProfileReference(String profileId, int revision, String fingerprint) {
    }

    public record ProfileSelection(String method, ProfileReference profileRef, String selectedBy, String selectedAt) {
    }

    public record Outcome(String migrationKey, String sourceAnalyzerId, String sourceConfigFingerprint, Status outcome,
            ProfileSelection profileSelection, String bridgeConnectionId, Integer bridgeConfigRevision,
            String reasonCode) {
    }
}
