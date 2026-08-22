package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record BridgeProfileCatalog(String schemaVersion, String catalogFingerprint, List<ProfileRevision> profiles) {

    public BridgeProfileCatalog {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }

    public record ProfileRevision(JsonNode profile, JsonNode publication,
            ControlRecognitionSummary controlRecognitionSummary) {

        public ProfileRevision(JsonNode profile, JsonNode publication) {
            this(profile, publication, null);
        }
    }

    public record ControlRecognitionSummary(String mode, String description, boolean affirmedNoControlResults,
            List<Condition> conditions) {

        public ControlRecognitionSummary {
            conditions = conditions == null ? List.of() : List.copyOf(conditions);
        }

        public record Condition(String key, String description, String controlLevel, String controlType) {
        }
    }
}
