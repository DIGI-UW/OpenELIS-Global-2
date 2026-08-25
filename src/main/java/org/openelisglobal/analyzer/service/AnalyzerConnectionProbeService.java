package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Probes the exact saved Bridge connection revision referenced by OpenELIS. */
@Service
public class AnalyzerConnectionProbeService {

    private final AnalyzerService analyzerService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final Supplier<String> requestIdSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalyzerConnectionProbeService(AnalyzerService analyzerService,
            BridgeAnalyzerConnectionClient bridgeClient) {
        this(analyzerService, bridgeClient, () -> UUID.randomUUID().toString());
    }

    AnalyzerConnectionProbeService(AnalyzerService analyzerService, BridgeAnalyzerConnectionClient bridgeClient,
            Supplier<String> requestIdSupplier) {
        this.analyzerService = analyzerService;
        this.bridgeClient = bridgeClient;
        this.requestIdSupplier = requestIdSupplier;
    }

    public AnalyzerConnectionProbeView probe(String analyzerId) {
        String exactAnalyzerId = requireText(analyzerId, "analyzer.testConnection.analyzerIdMissing");
        Analyzer analyzer = analyzerService.getWithType(exactAnalyzerId)
                .orElseThrow(() -> new AnalyzerConnectionProbeException("analyzer.testConnection.analyzerNotFound"));
        String connectionId = requireText(analyzer.getBridgeConnectionId(),
                "analyzer.testConnection.bridge.connectionMissing");
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        if (profile == null) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.profileMissing");
        }

        ObjectNode connection = bridgeCall(() -> bridgeClient.getConnection(connectionId));
        requireMatchingConnection(exactAnalyzerId, connectionId, profile, connection);
        int configRevision = connection.path("configRevision").asInt(0);
        String configFingerprint = connection.path("configFingerprint").asText(null);
        String requestId = requireText(requestIdSupplier.get(), "analyzer.testConnection.bridge.invalidEvidence");
        ObjectNode evidence = bridgeCall(() -> bridgeClient.probe(connectionId, configRevision, requestId));
        requireMatchingEvidence(connectionId, profile, configRevision, configFingerprint, requestId, evidence);
        try {
            return objectMapper.treeToValue(evidence, AnalyzerConnectionProbeView.class);
        } catch (JsonProcessingException exception) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.invalidEvidence", Map.of(),
                    exception);
        }
    }

    private static void requireMatchingConnection(String analyzerId, String connectionId,
            AnalyzerProfileBinding profile, ObjectNode connection) {
        JsonNode profileRef = connection.path("profileRef");
        if (!connectionId.equals(connection.path("connectionId").asText())
                || !analyzerId.equals(connection.path("clientAnalyzerId").asText())
                || !matchesProfile(profile, profileRef)) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.connectionMismatch");
        }
    }

    private static void requireMatchingEvidence(String connectionId, AnalyzerProfileBinding profile, int configRevision,
            String configFingerprint, String requestId, ObjectNode evidence) {
        if (!connectionId.equals(evidence.path("connectionId").asText())
                || !requestId.equals(evidence.path("requestId").asText())
                || configRevision != evidence.path("configRevision").asInt(0)
                || !Objects.equals(configFingerprint, evidence.path("configFingerprint").asText(null))
                || !matchesProfile(profile, evidence.path("profileRef"))
                || !evidence.path("nonMutating").asBoolean(false)) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.staleEvidence");
        }
    }

    private static boolean matchesProfile(AnalyzerProfileBinding profile, JsonNode profileRef) {
        return Objects.equals(profile.getProfileId(), profileRef.path("profileId").asText(null))
                && profile.getProfileRevision() == profileRef.path("revision").asInt(0)
                && Objects.equals(profile.getProfileFingerprint(), profileRef.path("fingerprint").asText(null));
    }

    private ObjectNode bridgeCall(Supplier<ObjectNode> call) {
        try {
            return call.get();
        } catch (BridgeAnalyzerConnectionException exception) {
            String messageKey = switch (exception.messageKey()) {
            case "analyzer.bridge.connection.notConfigured" -> "analyzer.testConnection.bridge.notConfigured";
            case "analyzer.bridge.connection.unreachable" -> "analyzer.testConnection.bridge.unreachable";
            case "analyzer.bridge.connection.httpStatus" -> "analyzer.testConnection.bridge.httpStatus";
            default -> "analyzer.testConnection.bridge.invalidEvidence";
            };
            throw new AnalyzerConnectionProbeException(messageKey, exception.messageArgs(), exception);
        }
    }

    private static String requireText(String value, String messageKey) {
        if (value == null || value.trim().isEmpty()) {
            throw new AnalyzerConnectionProbeException(messageKey);
        }
        return value.trim();
    }
}
