package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;

public record BridgeProfileCatalogEntry(JsonNode profile, BridgeProfileAudit audit, String fingerprint) {
}
