package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record BridgeProfileCatalog(String schemaVersion, String catalogFingerprint, List<ProfileRevision> profiles,
        List<CatalogIssue> issues) {

    public BridgeProfileCatalog {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public BridgeProfileCatalog(String schemaVersion, String catalogFingerprint, List<ProfileRevision> profiles) {
        this(schemaVersion, catalogFingerprint, profiles, List.of());
    }

    /** A profile file or draft the Bridge set aside when it loaded, and why. */
    public record CatalogIssue(String source, String reason) {
    }

    public record ProfileRevision(JsonNode profile, JsonNode publication,
            ControlRecognitionSummary controlRecognitionSummary) {

        public ProfileRevision(JsonNode profile, JsonNode publication) {
            this(profile, publication, null);
        }
    }

    public record ControlRecognitionSummary(String recognitionFingerprint, String mode, String description,
            boolean affirmedNoControlResults, List<Condition> conditions) {

        public ControlRecognitionSummary(String mode, String description, boolean affirmedNoControlResults,
                List<Condition> conditions) {
            this(null, mode, description, affirmedNoControlResults, conditions);
        }

        public ControlRecognitionSummary {
            conditions = conditions == null ? null : List.copyOf(conditions);
        }

        public record Condition(String key, String kind, String sourceLabel, String value, String description,
                String controlLevel, String controlType) {
        }
    }
}
