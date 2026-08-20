package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerTypeCatalogView(String schemaVersion, String catalogFingerprint, CatalogSummary summary,
        List<TypeSummary> types) {

    public AnalyzerTypeCatalogView {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public record CatalogSummary(int total, int inUse, int needsAttention, int deactivated) {
    }

    public record MappingSummary(int mapped, int total, String state) {
    }

    public record InstanceDefaults(String protocolVersion, String communicationMode, Integer port) {
    }

    public record TypeSummary(String profileId, int revision, String revisionFingerprint, String displayName,
            String manufacturer, String model, String source, String status, String protocol,
            InstanceDefaults instanceDefaults, String parentProfileId, Integer parentRevision, String siteBindingId,
            MappingSummary testMappings, MappingSummary resultMappings, long usedBy, String readiness,
            String publicationAction, String publicationActor, String publicationTime) {
    }
}
