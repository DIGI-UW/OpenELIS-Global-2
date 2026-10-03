package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;

/** Exact non-mutating evidence from probing one saved Bridge connection. */
public record AnalyzerConnectionProbeView(String schemaVersion, String requestId, String connectionId,
        ProfileRef profileRef, int configRevision, String configFingerprint, boolean nonMutating, String status,
        String startedAt, String completedAt, List<Check> checks) {

    public AnalyzerConnectionProbeView {
        checks = checks == null ? List.of() : List.copyOf(checks);
    }

    public record ProfileRef(String profileId, int revision, String fingerprint) {
    }

    public record Check(String key, String status, String messageKey, long durationMillis,
            Map<String, Object> details) {
        public Check {
            details = details == null ? Map.of() : Map.copyOf(details);
        }
    }
}
