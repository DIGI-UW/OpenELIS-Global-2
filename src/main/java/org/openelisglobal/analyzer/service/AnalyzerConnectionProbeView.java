package org.openelisglobal.analyzer.service;

import java.util.List;
import java.util.Map;

/** Structured Bridge connection evidence returned to the analyzer setup UI. */
public record AnalyzerConnectionProbeView(String schemaVersion, String analyzerId, ProfileRef profileRef,
        String desiredStateFingerprint, Connection connection, String dataFlow, String outcome,
        ConfigureEndpoint configureEndpoint, boolean resultsOnlyAvailable, List<Check> checks) {

    public AnalyzerConnectionProbeView {
        checks = checks == null ? List.of() : List.copyOf(checks);
    }

    public record ProfileRef(String profileId, int revision) {
    }

    public record Connection(String mode, String role) {
    }

    public record ConfigureEndpoint(String kind, String host, Integer port, String path, String url) {
    }

    public record Check(String kind, String status, String code, long responseTimeMs, Map<String, Object> args) {
        public Check {
            args = args == null ? Map.of() : Map.copyOf(args);
        }
    }
}
