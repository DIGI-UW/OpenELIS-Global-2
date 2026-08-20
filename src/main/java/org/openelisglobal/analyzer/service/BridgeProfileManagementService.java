package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface BridgeProfileManagementService {

    JsonNode createDraft(String displayName, String actor);

    JsonNode getDraft(String draftId);

    JsonNode updateDraft(String draftId, JsonNode profile, String actor);

    JsonNode publishDraft(String draftId, String actor);

    JsonNode updateShared(String profileId, int sourceRevision, String actor);

    JsonNode duplicate(String profileId, int sourceRevision, String displayName, String actor);

    JsonNode deactivate(String profileId, String actor);

    JsonNode reactivate(String profileId, String actor);

    JsonNode history(String profileId);
}
