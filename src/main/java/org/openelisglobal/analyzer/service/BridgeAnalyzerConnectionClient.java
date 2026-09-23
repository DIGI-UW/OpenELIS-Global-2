package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/** Generic mediator for Bridge-owned durable analyzer connections. */
@Service
public class BridgeAnalyzerConnectionClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern FINGERPRINT = Pattern.compile("^sha256:[0-9a-f]{64}$");
    private static final Set<String> RUNTIME_ACTIONS = Set.of("ACTIVATE", "DEACTIVATE");

    private final BridgeHttpClient httpClient;
    private final String bridgeBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BridgeAnalyzerConnectionClient(BridgeHttpClient httpClient,
            @Value("${analyzer.bridge.url:}") String bridgeBaseUrl) {
        this.httpClient = httpClient;
        this.bridgeBaseUrl = stripTrailingSlashes(bridgeBaseUrl);
    }

    public ObjectNode createConnection(ObjectNode request) {
        ObjectNode body = requireObject(request, "analyzer.bridge.connection.invalidRequest");
        return invoke("POST", connectionsEndpoint(), body, null, null);
    }

    public ObjectNode getConnection(String connectionId) {
        String id = requireText(connectionId, "connection ID");
        return invoke("GET", connectionEndpoint(id), null, id, "CONNECTION");
    }

    public ObjectNode updateConnection(String connectionId, ObjectNode request) {
        String id = requireText(connectionId, "connection ID");
        ObjectNode body = requireObject(request, "analyzer.bridge.connection.invalidRequest");
        if (!id.equals(body.path("connectionId").asText())) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidRequest");
        }
        return invoke("PUT", connectionEndpoint(id), body, id, "CONNECTION");
    }

    public ObjectNode probe(String connectionId, int expectedConfigRevision, String requestId) {
        String id = requireText(connectionId, "connection ID");
        String exactRequestId = requireText(requestId, "request ID");
        requireRevision(expectedConfigRevision);
        ObjectNode request = objectMapper.createObjectNode();
        request.put("schemaVersion", "1.0");
        request.put("requestId", exactRequestId);
        request.put("connectionId", id);
        request.put("expectedConfigRevision", expectedConfigRevision);
        ObjectNode evidence = invoke("POST", connectionEndpoint(id) + "/probe", request, id, "PROBE");
        if (!exactRequestId.equals(evidence.path("requestId").asText())
                || expectedConfigRevision != evidence.path("configRevision").asInt(0)) {
            throw invalidEvidence();
        }
        return evidence;
    }

    public ObjectNode applyRuntimeCommand(String connectionId, int expectedConfigRevision, String action,
            String commandId) {
        String id = requireText(connectionId, "connection ID");
        String exactAction = requireText(action, "runtime action");
        String exactCommandId = requireText(commandId, "command ID");
        requireRevision(expectedConfigRevision);
        if (!RUNTIME_ACTIONS.contains(exactAction)) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidRequest");
        }
        ObjectNode command = objectMapper.createObjectNode();
        command.put("schemaVersion", "1.0");
        command.put("commandId", exactCommandId);
        command.put("connectionId", id);
        command.put("action", exactAction);
        command.put("expectedConfigRevision", expectedConfigRevision);
        ObjectNode acknowledgement = invoke("POST", connectionEndpoint(id) + "/runtime", command, id, "RUNTIME");
        if (!exactCommandId.equals(acknowledgement.path("commandId").asText())
                || !exactAction.equals(acknowledgement.path("action").asText())
                || expectedConfigRevision != acknowledgement.path("configRevision").asInt(0)) {
            throw invalidEvidence();
        }
        return acknowledgement;
    }

    private ObjectNode invoke(String method, String endpoint, ObjectNode body, String expectedConnectionId,
            String responseKind) {
        requireConfigured();
        BridgeHttpClient.BridgeResponse response;
        try {
            String json = body == null ? null : objectMapper.writeValueAsString(body);
            response = switch (method) {
            case "GET" -> httpClient.get(endpoint, REQUEST_TIMEOUT);
            case "POST" -> httpClient.post(endpoint, json, REQUEST_TIMEOUT);
            case "PUT" -> httpClient.put(endpoint, json, REQUEST_TIMEOUT);
            default -> throw new IllegalArgumentException("Unsupported Bridge method " + method);
            };
        } catch (IOException exception) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.unreachable",
                    Map.of("detail", String.valueOf(exception.getMessage())), exception);
        }
        if (!response.isSuccess()) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.httpStatus",
                    Map.of("status", response.status));
        }
        ObjectNode document;
        try {
            JsonNode parsed = objectMapper.readTree(response.body);
            if (!(parsed instanceof ObjectNode object)) {
                throw invalidEvidence();
            }
            document = object;
        } catch (IOException exception) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidEvidence", Map.of(),
                    exception);
        }
        validateCommon(document, expectedConnectionId);
        if (responseKind == null || "CONNECTION".equals(responseKind)) {
            validateConnection(document);
        } else if ("PROBE".equals(responseKind)) {
            validateProbe(document);
        } else if ("RUNTIME".equals(responseKind)) {
            validateRuntimeAcknowledgement(document);
        }
        return document.deepCopy();
    }

    private static void validateCommon(ObjectNode document, String expectedConnectionId) {
        if (!"1.0".equals(document.path("schemaVersion").asText())
                || !hasText(document.path("connectionId").asText(null)) || expectedConnectionId != null
                        && !expectedConnectionId.equals(document.path("connectionId").asText())) {
            throw invalidEvidence();
        }
    }

    private static void validateConnection(ObjectNode document) {
        if (!hasText(document.path("clientAnalyzerId").asText(null))
                || !hasText(document.path("displayName").asText(null)) || !validProfileRef(document.path("profileRef"))
                || document.path("configRevision").asInt(0) < 1
                || !validFingerprint(document.path("configFingerprint").asText(null))
                || !document.path("fields").isArray() || !document.path("readiness").isObject()
                || !hasText(document.path("desiredRuntimeState").asText(null))
                || !hasText(document.path("actualRuntimeState").asText(null))
                || !hasText(document.path("updatedAt").asText(null))) {
            throw invalidEvidence();
        }
    }

    private static void validateProbe(ObjectNode document) {
        if (!hasText(document.path("requestId").asText(null)) || !validProfileRef(document.path("profileRef"))
                || document.path("configRevision").asInt(0) < 1
                || !validFingerprint(document.path("configFingerprint").asText(null))
                || !document.path("nonMutating").asBoolean(false) || !hasText(document.path("status").asText(null))
                || !hasText(document.path("startedAt").asText(null))
                || !hasText(document.path("completedAt").asText(null)) || !document.path("checks").isArray()) {
            throw invalidEvidence();
        }
    }

    private static void validateRuntimeAcknowledgement(ObjectNode document) {
        if (!hasText(document.path("commandId").asText(null)) || !hasText(document.path("action").asText(null))
                || !hasText(document.path("outcome").asText(null)) || !validProfileRef(document.path("profileRef"))
                || document.path("configRevision").asInt(0) < 1
                || !validFingerprint(document.path("configFingerprint").asText(null))
                || document.path("runtimeRevision").asInt(0) < 1
                || !validFingerprint(document.path("runtimeFingerprint").asText(null))
                || !hasText(document.path("desiredRuntimeState").asText(null))
                || !hasText(document.path("actualRuntimeState").asText(null)) || !document.path("blockers").isArray()
                || !hasText(document.path("acknowledgedAt").asText(null))) {
            throw invalidEvidence();
        }
    }

    private static boolean validProfileRef(JsonNode profileRef) {
        return profileRef.isObject() && hasText(profileRef.path("profileId").asText(null))
                && profileRef.path("revision").asInt(0) > 0
                && validFingerprint(profileRef.path("fingerprint").asText(null));
    }

    private static boolean validFingerprint(String value) {
        return value != null && FINGERPRINT.matcher(value).matches();
    }

    private String connectionsEndpoint() {
        return bridgeBaseUrl + "/api/connections";
    }

    private String connectionEndpoint(String connectionId) {
        return connectionsEndpoint() + "/" + UriUtils.encodePathSegment(connectionId, StandardCharsets.UTF_8);
    }

    private void requireConfigured() {
        if (bridgeBaseUrl.isBlank()) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.notConfigured");
        }
    }

    private static ObjectNode requireObject(ObjectNode value, String messageKey) {
        if (value == null) {
            throw new BridgeAnalyzerConnectionException(messageKey);
        }
        return value.deepCopy();
    }

    private static void requireRevision(int revision) {
        if (revision < 1) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidRequest");
        }
    }

    private static String requireText(String value, String label) {
        if (!hasText(value)) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidRequest",
                    Map.of("field", label));
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static BridgeAnalyzerConnectionException invalidEvidence() {
        return new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidEvidence");
    }

    private static String stripTrailingSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
