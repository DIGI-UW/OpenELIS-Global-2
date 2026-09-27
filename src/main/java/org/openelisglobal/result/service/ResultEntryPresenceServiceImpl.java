package org.openelisglobal.result.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.validator.GenericValidator;
import org.springframework.stereotype.Service;

/**
 * In-memory implementation of the FR-O3 presence registry (OGC-1020). One claim
 * per session; a claim expires TTL_MILLIS after its last heartbeat, so
 * auto-logout, a closed tab, or a crashed browser can never block anyone. No
 * schema, no persistence — by design (see the multi-component FRS §O).
 */
@Service
public class ResultEntryPresenceServiceImpl implements ResultEntryPresenceService {

    /** A heartbeat every ~10s keeps a claim alive; 30s of silence evicts it. */
    private static final long DEFAULT_TTL_MILLIS = 30_000L;

    private final long ttlMillis;

    public ResultEntryPresenceServiceImpl() {
        this(DEFAULT_TTL_MILLIS);
    }

    ResultEntryPresenceServiceImpl(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    private static class Claim {
        final String analysisId;
        final String userDisplayName;
        final long lastHeartbeat;

        Claim(String analysisId, String userDisplayName, long lastHeartbeat) {
            this.analysisId = analysisId;
            this.userDisplayName = userDisplayName;
            this.lastHeartbeat = lastHeartbeat;
        }
    }

    private final ConcurrentHashMap<String, Claim> claimsBySession = new ConcurrentHashMap<>();

    @Override
    public void heartbeat(String sessionId, String userDisplayName, String analysisId) {
        if (GenericValidator.isBlankOrNull(sessionId)) {
            return;
        }
        if (GenericValidator.isBlankOrNull(analysisId)) {
            claimsBySession.remove(sessionId);
        } else {
            claimsBySession.put(sessionId, new Claim(analysisId, userDisplayName, System.currentTimeMillis()));
        }
    }

    @Override
    public void clearSession(String sessionId) {
        if (!GenericValidator.isBlankOrNull(sessionId)) {
            claimsBySession.remove(sessionId);
        }
    }

    @Override
    public Map<String, String> getPresence(List<String> analysisIds, String askingSessionId) {
        evictExpired();
        Map<String, String> presence = new HashMap<>();
        if (analysisIds == null || analysisIds.isEmpty()) {
            return presence;
        }
        for (Map.Entry<String, Claim> entry : claimsBySession.entrySet()) {
            if (entry.getKey().equals(askingSessionId)) {
                continue;
            }
            Claim claim = entry.getValue();
            if (analysisIds.contains(claim.analysisId)) {
                presence.put(claim.analysisId, claim.userDisplayName);
            }
        }
        return presence;
    }

    private void evictExpired() {
        long cutoff = System.currentTimeMillis() - ttlMillis;
        Iterator<Map.Entry<String, Claim>> iterator = claimsBySession.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().lastHeartbeat < cutoff) {
                iterator.remove();
            }
        }
    }
}
