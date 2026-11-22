package org.openelisglobal.result.service;

import java.util.List;
import java.util.Map;

/**
 * Session-bound "in review by X" presence for the unified Results worklist
 * (OGC-1020, FR-O3). Presence is advisory only — it never blocks Edit or Save.
 * Entries live in memory only (never persisted) and expire when the heartbeat
 * stops, so an abandoned panel or a dead session can never leave a ghost
 * indicator.
 */
public interface ResultEntryPresenceService {

    /**
     * Records that the given session/user currently has the given analysis open in
     * Edit. A null or blank analysisId only refreshes/clears this session's
     * previous claim.
     */
    void heartbeat(String sessionId, String userDisplayName, String analysisId);

    /** Drops every claim held by the given session (logout / session end). */
    void clearSession(String sessionId);

    /**
     * Returns analysisId → other user's display name for the requested analyses.
     * Claims held by the asking session are excluded; expired claims are evicted.
     */
    Map<String, String> getPresence(List<String> analysisIds, String askingSessionId);
}
