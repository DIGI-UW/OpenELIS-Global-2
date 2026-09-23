package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.prepost.PreAuthorize;

public interface BridgeProfileManagementService {

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode createDraft(String displayName, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode getDraft(String draftId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode getControlRecognition(String draftId);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode updateDraft(String draftId, JsonNode profile, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode updateControlRecognition(String draftId, AnalyzerControlRecognitionUpdate update, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode publishDraft(String draftId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode updateShared(String profileId, int sourceRevision, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode duplicate(String profileId, int sourceRevision, String displayName, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode deactivate(String profileId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode reactivate(String profileId, String actor);

    @PreAuthorize("hasAuthority('PRIV_ANALYZER_CONFIGURE')")
    JsonNode history(String profileId);
}
