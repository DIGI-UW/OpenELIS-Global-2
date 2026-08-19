package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record BridgeProfileCatalog(String schemaVersion, String catalogFingerprint, List<ProfileRevision> profiles) {

    public BridgeProfileCatalog {
        profiles = profiles == null ? List.of() : List.copyOf(profiles);
    }

    public record ProfileRevision(JsonNode profile, JsonNode publication) {
    }
}
