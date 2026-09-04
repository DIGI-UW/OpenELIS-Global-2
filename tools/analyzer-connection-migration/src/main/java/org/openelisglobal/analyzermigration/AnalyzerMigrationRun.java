package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Builds the deterministic PLAN report for one frozen OpenELIS snapshot. */
public final class AnalyzerMigrationRun {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper CANONICAL_JSON = canonicalMapper();
    private static final Pattern FINGERPRINT = Pattern.compile("^sha256:[0-9a-f]{64}$");

    private final AnalyzerMigrationPlanner planner;
    private final BridgeGateway bridge;
    private final OpenElisGateway openElis;

    @FunctionalInterface
    public interface BridgeGateway {
        ObjectNode createConnection(ObjectNode request);

        default ObjectNode getConnection(String connectionId) {
            throw new UnsupportedOperationException("Bridge read is not configured");
        }
    }

    @FunctionalInterface
    public interface OpenElisGateway {
        void attachBridgeConnection(String sourceAnalyzerId, ObjectNode profileRef, String bridgeConnectionId,
                String actor);

        default ObjectNode getAnalyzerReference(String sourceAnalyzerId) {
            throw new UnsupportedOperationException("OpenELIS read is not configured");
        }
    }

    public AnalyzerMigrationRun(AnalyzerMigrationPlanner planner) {
        this(planner, null, null);
    }

    public AnalyzerMigrationRun(AnalyzerMigrationPlanner planner, BridgeGateway bridge, OpenElisGateway openElis) {
        this.planner = planner;
        this.bridge = bridge;
        this.openElis = openElis;
    }

    public ObjectNode plan(String runId, Instant startedAt, Instant completedAt, ObjectNode sourceSnapshot,
            ObjectNode selections, ArrayNode profiles) {
        List<ObjectNode> sources = sortedSources(sourceSnapshot);
        Map<String, ObjectNode> profilesByReference = profilesByReference(profiles);

        ObjectNode manifest = JSON.createObjectNode();
        manifest.put("schemaVersion", "1.0");
        manifest.put("runId", runId);
        manifest.put("mode", "PLAN");
        manifest.put("sourceSnapshotFingerprint", fingerprint(sources));
        manifest.put("startedAt", startedAt.toString());
        manifest.put("completedAt", completedAt.toString());
        ArrayNode outcomes = manifest.putArray("outcomes");

        for (ObjectNode source : sources) {
            String sourceAnalyzerId = source.path("sourceAnalyzerId").asText();
            ObjectNode selection = object(selections, sourceAnalyzerId);
            ObjectNode outcome = outcomeIdentity(source);
            if ("EXCLUDE".equals(selection.path("method").asText())) {
                String reasonCode = selection.path("reasonCode").asText();
                if (reasonCode.isBlank()) {
                    outcome.put("outcome", "NEEDS_CORRECTION");
                    outcome.put("reasonCode", "EXCLUSION_REASON_REQUIRED");
                } else {
                    outcome.put("outcome", "INTENTIONALLY_EXCLUDED");
                    outcome.put("reasonCode", reasonCode);
                }
                outcomes.add(outcome);
                continue;
            }

            if (selection.isEmpty()) {
                outcome.put("outcome", "NEEDS_CORRECTION");
                outcome.put("reasonCode", "EXPLICIT_PROFILE_SELECTION_REQUIRED");
                outcomes.add(outcome);
                continue;
            }

            ObjectNode profile = profilesByReference.get(profileReferenceKey(selection.path("profileRef")));
            if (profile == null) {
                outcome.put("outcome", "NEEDS_CORRECTION");
                outcome.put("reasonCode", "SELECTED_PROFILE_REVISION_NOT_FOUND");
                outcomes.add(outcome);
                continue;
            }

            AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection, profile);
            outcome.put("outcome", decision.outcome().name());
            if (decision.outcome() == AnalyzerMigrationPlanner.Outcome.READY) {
                outcome.set("profileSelection", reportSelection(selection));
            } else {
                outcome.put("reasonCode", decision.reasonCodes().get(0));
            }
            outcomes.add(outcome);
        }
        return manifest;
    }

    public ObjectNode apply(String runId, Instant startedAt, Instant completedAt, ObjectNode sourceSnapshot,
            ObjectNode selections, ArrayNode profiles) {
        if (bridge == null || openElis == null) {
            throw new IllegalStateException("APPLY gateways are required");
        }
        ObjectNode manifest = plan(runId, startedAt, completedAt, sourceSnapshot, selections, profiles);
        manifest.put("mode", "APPLY");
        Map<String, ObjectNode> sourcesById = new LinkedHashMap<>();
        sortedSources(sourceSnapshot)
                .forEach(source -> sourcesById.put(source.path("sourceAnalyzerId").asText(), source));
        Map<String, ObjectNode> profilesByReference = profilesByReference(profiles);

        for (JsonNode node : manifest.path("outcomes")) {
            ObjectNode outcome = (ObjectNode) node;
            if (!"READY".equals(outcome.path("outcome").asText())) {
                continue;
            }
            String sourceAnalyzerId = outcome.path("sourceAnalyzerId").asText();
            ObjectNode source = sourcesById.get(sourceAnalyzerId);
            ObjectNode selection = object(selections, sourceAnalyzerId);
            ObjectNode profile = profilesByReference.get(profileReferenceKey(selection.path("profileRef")));
            AnalyzerMigrationPlanner.Decision decision = planner.plan(source, selection, profile);
            try {
                ObjectNode response = bridge.createConnection(
                        createConnectionRequest(outcome, source, selection, decision.connectionValues()));
                if (!matchesCreateEvidence(sourceAnalyzerId, selection.path("profileRef"), response)) {
                    markCorrection(outcome, "BRIDGE_CONNECTION_EVIDENCE_MISMATCH");
                    continue;
                }
                openElis.attachBridgeConnection(sourceAnalyzerId, (ObjectNode) selection.path("profileRef"),
                        response.path("connectionId").asText(), selection.path("selectedBy").asText());
                outcome.put("outcome", "MIGRATED");
                outcome.put("bridgeConnectionId", response.path("connectionId").asText());
                outcome.put("bridgeConfigRevision", response.path("configRevision").asInt());
            } catch (RuntimeException exception) {
                markCorrection(outcome, "APPLY_FAILED");
            }
        }
        return manifest;
    }

    public ObjectNode verify(String runId, Instant startedAt, Instant completedAt, ObjectNode sourceSnapshot,
            ObjectNode applyManifest) {
        if (bridge == null || openElis == null) {
            throw new IllegalStateException("VERIFY gateways are required");
        }
        if (applyManifest == null || !"APPLY".equals(applyManifest.path("mode").asText())) {
            throw new IllegalArgumentException("A completed APPLY manifest is required");
        }

        ObjectNode manifest = applyManifest.deepCopy();
        manifest.put("runId", runId);
        manifest.put("mode", "VERIFY");
        manifest.put("startedAt", startedAt.toString());
        manifest.put("completedAt", completedAt.toString());
        String sourceFingerprint = fingerprint(sortedSources(sourceSnapshot));
        if (!sourceFingerprint.equals(applyManifest.path("sourceSnapshotFingerprint").asText())) {
            throw new IllegalArgumentException("VERIFY requires the unchanged frozen source snapshot");
        }
        manifest.put("sourceSnapshotFingerprint", sourceFingerprint);
        Map<String, ObjectNode> sourcesById = new LinkedHashMap<>();
        sortedSources(sourceSnapshot)
                .forEach(source -> sourcesById.put(source.path("sourceAnalyzerId").asText(), source));

        for (JsonNode node : manifest.path("outcomes")) {
            ObjectNode outcome = (ObjectNode) node;
            if (!"MIGRATED".equals(outcome.path("outcome").asText())) {
                continue;
            }
            ObjectNode source = sourcesById.get(outcome.path("sourceAnalyzerId").asText());
            if (source == null
                    || !source.path("sourceConfigFingerprint").equals(outcome.path("sourceConfigFingerprint"))) {
                markCorrection(outcome, "SOURCE_SNAPSHOT_CHANGED");
                continue;
            }
            try {
                String connectionId = outcome.path("bridgeConnectionId").asText();
                JsonNode profileRef = outcome.path("profileSelection").path("profileRef");
                ObjectNode connection = bridge.getConnection(connectionId);
                if (!matchesVerifyBridge(outcome, profileRef, connection)) {
                    markCorrection(outcome, "BRIDGE_CONNECTION_EVIDENCE_MISMATCH");
                    continue;
                }
                ObjectNode reference = openElis.getAnalyzerReference(outcome.path("sourceAnalyzerId").asText());
                if (!matchesOpenElisReference(outcome, profileRef, reference)) {
                    markCorrection(outcome, "OPENELIS_REFERENCE_MISMATCH");
                }
            } catch (RuntimeException exception) {
                markCorrection(outcome, "VERIFY_FAILED");
            }
        }
        return manifest;
    }

    private static ObjectNode createConnectionRequest(ObjectNode outcome, ObjectNode source, ObjectNode selection,
            ObjectNode values) {
        ObjectNode request = JSON.createObjectNode();
        request.put("schemaVersion", "1.0");
        request.put("requestId", "migration:" + outcome.path("migrationKey").asText());
        request.put("clientAnalyzerId", source.path("sourceAnalyzerId").asText());
        request.set("profileRef", selection.path("profileRef").deepCopy());
        request.put("displayName", source.path("displayName").asText());
        request.set("values", values.deepCopy());
        return request;
    }

    private static boolean matchesCreateEvidence(String sourceAnalyzerId, JsonNode profileRef, ObjectNode response) {
        return response != null && "1.0".equals(response.path("schemaVersion").asText())
                && hasText(response.path("connectionId").asText(null))
                && sourceAnalyzerId.equals(response.path("clientAnalyzerId").asText())
                && profileRef.equals(response.path("profileRef")) && response.path("configRevision").asInt() > 0;
    }

    private static boolean matchesVerifyBridge(ObjectNode outcome, JsonNode profileRef, ObjectNode connection) {
        return connection != null && "1.0".equals(connection.path("schemaVersion").asText())
                && outcome.path("bridgeConnectionId").equals(connection.path("connectionId"))
                && outcome.path("sourceAnalyzerId").equals(connection.path("clientAnalyzerId"))
                && profileRef.equals(connection.path("profileRef"))
                && outcome.path("bridgeConfigRevision").asInt() == connection.path("configRevision").asInt();
    }

    private static boolean matchesOpenElisReference(ObjectNode outcome, JsonNode profileRef, ObjectNode reference) {
        return reference != null && outcome.path("sourceAnalyzerId").equals(reference.path("sourceAnalyzerId"))
                && outcome.path("bridgeConnectionId").equals(reference.path("bridgeConnectionId"))
                && profileRef.equals(reference.path("profileRef"));
    }

    private static void markCorrection(ObjectNode outcome, String reasonCode) {
        outcome.put("outcome", "NEEDS_CORRECTION");
        outcome.put("reasonCode", reasonCode);
        outcome.remove("profileSelection");
        outcome.remove("bridgeConnectionId");
        outcome.remove("bridgeConfigRevision");
    }

    private static ObjectNode outcomeIdentity(ObjectNode source) {
        ObjectNode outcome = JSON.createObjectNode();
        String sourceAnalyzerId = source.path("sourceAnalyzerId").asText();
        String sourceFingerprint = source.path("sourceConfigFingerprint").asText();
        String fingerprintPrefix = sourceFingerprint.startsWith("sha256:") && sourceFingerprint.length() >= 15
                ? sourceFingerprint.substring(7, 15)
                : sourceFingerprint;
        outcome.put("migrationKey", sourceAnalyzerId + ":" + fingerprintPrefix);
        outcome.put("sourceAnalyzerId", sourceAnalyzerId);
        outcome.put("sourceConfigFingerprint", sourceFingerprint);
        return outcome;
    }

    private static ObjectNode reportSelection(ObjectNode selection) {
        ObjectNode reportSelection = JSON.createObjectNode();
        reportSelection.put("method", "EXPLICIT");
        reportSelection.set("profileRef", selection.path("profileRef").deepCopy());
        reportSelection.put("selectedBy", selection.path("selectedBy").asText());
        reportSelection.put("selectedAt", selection.path("selectedAt").asText());
        return reportSelection;
    }

    private static List<ObjectNode> sortedSources(ObjectNode sourceSnapshot) {
        List<ObjectNode> sources = new ArrayList<>();
        JsonNode analyzers = sourceSnapshot == null ? null : sourceSnapshot.path("analyzers");
        if (analyzers != null && analyzers.isArray()) {
            analyzers.forEach(source -> {
                if (source instanceof ObjectNode object) {
                    sources.add(object.deepCopy());
                }
            });
        }
        sources.sort(Comparator.comparing(source -> source.path("sourceAnalyzerId").asText()));
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("Source snapshot must contain at least one analyzer");
        }
        Set<String> sourceIds = new HashSet<>();
        for (ObjectNode source : sources) {
            String sourceId = source.path("sourceAnalyzerId").asText();
            if (sourceId.isBlank() || !sourceIds.add(sourceId)) {
                throw new IllegalArgumentException("Source analyzer IDs must be non-empty and unique");
            }
            if (!FINGERPRINT.matcher(source.path("sourceConfigFingerprint").asText()).matches()) {
                throw new IllegalArgumentException("Source configuration fingerprint is invalid: " + sourceId);
            }
            if (source.path("displayName").asText().isBlank() || !source.path("configuration").isObject()) {
                throw new IllegalArgumentException("Source analyzer is incomplete: " + sourceId);
            }
        }
        return sources;
    }

    private static Map<String, ObjectNode> profilesByReference(ArrayNode profiles) {
        Map<String, ObjectNode> byReference = new LinkedHashMap<>();
        if (profiles != null) {
            profiles.forEach(profile -> {
                if (profile instanceof ObjectNode object) {
                    String key = profileReferenceKey(profileReference(object));
                    byReference.put(key, object);
                }
            });
        }
        return byReference;
    }

    private static ObjectNode profileReference(ObjectNode profile) {
        ObjectNode reference = JSON.createObjectNode();
        reference.put("profileId", profile.path("profileMeta").path("id").asText());
        reference.put("revision", profile.path("catalog").path("revision").asInt());
        reference.put("fingerprint", profile.path("catalog").path("revisionFingerprint").asText());
        return reference;
    }

    private static String profileReferenceKey(JsonNode reference) {
        return reference.path("profileId").asText() + "@" + reference.path("revision").asInt() + "@"
                + reference.path("fingerprint").asText();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String fingerprint(List<ObjectNode> sources) {
        ArrayNode canonicalSources = CANONICAL_JSON.createArrayNode();
        sources.forEach(canonicalSources::add);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(CANONICAL_JSON.writeValueAsBytes(canonicalSources));
            return "sha256:" + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot fingerprint analyzer source snapshot", e);
        }
    }

    private static ObjectNode object(ObjectNode parent, String field) {
        JsonNode value = parent == null ? null : parent.path(field);
        return value instanceof ObjectNode object ? object : JSON.createObjectNode();
    }

    private static ObjectMapper canonicalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }
}
