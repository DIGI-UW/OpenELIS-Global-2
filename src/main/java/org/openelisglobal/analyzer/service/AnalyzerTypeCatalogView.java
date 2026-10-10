package org.openelisglobal.analyzer.service;

import java.util.List;

/**
 * The analyzer types OE2 offers, with {@code issues}: the profile files the
 * Bridge set aside when it loaded, which it does not serve.
 */
public record AnalyzerTypeCatalogView(String schemaVersion, String catalogFingerprint, CatalogSummary summary,
        List<TypeSummary> types, List<BridgeProfileCatalog.CatalogIssue> issues) {

    public AnalyzerTypeCatalogView {
        types = types == null ? List.of() : List.copyOf(types);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public record CatalogSummary(int total, int inUse, int needsAttention, int deactivated) {
    }

    public record MappingSummary(int mapped, int excluded, int total, String state) {
    }

    /**
     * {@code newerProfileRevision}: the analyzer can adopt this revision.
     * {@code newerMappingRevision}: it has a saved mapping revision that is not in
     * force yet and waits for Confirm and Apply.
     */
    public record AffectedAnalyzer(String id, String name, boolean active, int pinnedProfileRevision,
            int pinnedMappingRevision, boolean newerProfileRevision, boolean newerMappingRevision) {
    }

    public record TypeSummary(String profileId, int revision, String revisionFingerprint, String displayName,
            String manufacturer, String model, String source, String status, String protocol, String protocolVersion,
            String communicationMode, String parentProfileId, Integer parentRevision, MappingSummary testMappings,
            MappingSummary resultMappings, long usedBy, String readiness, String publicationAction,
            String publicationActor, String publicationTime, List<AffectedAnalyzer> affectedAnalyzers) {

        public TypeSummary {
            affectedAnalyzers = affectedAnalyzers == null ? List.of() : List.copyOf(affectedAnalyzers);
        }

        public TypeSummary(String profileId, int revision, String revisionFingerprint, String displayName,
                String manufacturer, String model, String source, String status, String protocol,
                String protocolVersion, String communicationMode, String parentProfileId, Integer parentRevision,
                MappingSummary testMappings, MappingSummary resultMappings, long usedBy, String readiness,
                String publicationAction, String publicationActor, String publicationTime) {
            this(profileId, revision, revisionFingerprint, displayName, manufacturer, model, source, status, protocol,
                    protocolVersion, communicationMode, parentProfileId, parentRevision, testMappings, resultMappings,
                    usedBy, readiness, publicationAction, publicationActor, publicationTime, List.of());
        }
    }
}
