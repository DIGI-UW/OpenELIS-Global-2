package org.openelisglobal.analyzer.service;

public record AnalyzerMigrationReferenceView(String sourceAnalyzerId, String bridgeConnectionId,
        ProfileReference profileRef) {

    public record ProfileReference(String profileId, int revision, String fingerprint) {
    }
}
