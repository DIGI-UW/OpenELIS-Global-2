package org.openelisglobal.analyzer.service;

import java.util.List;

public record AnalyzerTypeCatalogView(String schemaVersion, String catalogFingerprint, CatalogSummary summary,
        List<TypeSummary> types) {

    public AnalyzerTypeCatalogView {
        types = types == null ? List.of() : List.copyOf(types);
    }

    public record CatalogSummary(int total, int inUse, int needsAttention, int deactivated) {
    }

    public record MappingSummary(int mapped, int excluded, int total, String state) {
    }

    public record AffectedAnalyzer(String id, String name, boolean active, int pinnedProfileRevision,
            int pinnedMappingRevision, boolean updateAvailable) {
    }

    public record TypeSummary(String profileId, int revision, String revisionFingerprint, String displayName,
            String manufacturer, String model, String source, String status, String protocol, String protocolVersion,
            String communicationMode, String parentProfileId, Integer parentRevision, String siteBindingId,
            MappingSummary testMappings, MappingSummary resultMappings, long usedBy, String readiness,
            String publicationAction, String publicationActor, String publicationTime,
            List<AffectedAnalyzer> affectedAnalyzers) {

        public TypeSummary {
            affectedAnalyzers = affectedAnalyzers == null ? List.of() : List.copyOf(affectedAnalyzers);
        }

        public TypeSummary(String profileId, int revision, String revisionFingerprint, String displayName,
                String manufacturer, String model, String source, String status, String protocol,
                String protocolVersion, String communicationMode, String parentProfileId, Integer parentRevision,
                String siteBindingId, MappingSummary testMappings, MappingSummary resultMappings, long usedBy,
                String readiness, String publicationAction, String publicationActor, String publicationTime) {
            this(profileId, revision, revisionFingerprint, displayName, manufacturer, model, source, status, protocol,
                    protocolVersion, communicationMode, parentProfileId, parentRevision, siteBindingId, testMappings,
                    resultMappings, usedBy, readiness, publicationAction, publicationActor, publicationTime, List.of());
        }
    }
}
