package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface BridgeProfileManagementService {

    JsonNode create(JsonNode profile, String actor);

    JsonNode updateShared(String profileId, JsonNode profile, String actor);

    JsonNode duplicate(String profileId, int sourceRevision, String targetProfileId, String displayName, String actor);

    JsonNode deactivate(String profileId, String actor);

    JsonNode reactivate(String profileId, String actor);

    JsonNode history(String profileId);
}
