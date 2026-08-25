package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerInstanceServiceImpl implements AnalyzerInstanceService {

    private final AnalyzerInstanceLocalStateService localStateService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final Supplier<String> requestIdSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient) {
        this(localStateService, bridgeClient, () -> UUID.randomUUID().toString());
    }

    AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient, Supplier<String> requestIdSupplier) {
        this.localStateService = localStateService;
        this.bridgeClient = bridgeClient;
        this.requestIdSupplier = requestIdSupplier;
    }

    @Override
    public AnalyzerInstanceView create(AnalyzerInstanceRequest request, String actor) {
        AnalyzerInstanceState localState = localStateService.create(request, actor);
        try {
            ObjectNode connection = bridgeClient.createConnection(createConnectionRequest(localState, request));
            requireExactConnection(localState, connection);
            try {
                AnalyzerInstanceState connectedState = localStateService.attachBridgeConnection(localState.analyzerId(),
                        connection.path("connectionId").asText(), actor);
                return new AnalyzerInstanceView(connectedState, connection, null);
            } catch (RuntimeException exception) {
                return new AnalyzerInstanceView(localState, connection,
                        "analyzer.bridge.connection.referenceNotStored");
            }
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(localState, null, exception.messageKey());
        }
    }

    @Override
    public List<AnalyzerInstanceState> list() {
        return localStateService.list();
    }

    @Override
    public AnalyzerInstanceView get(String analyzerId) {
        AnalyzerInstanceState state = localStateService.get(analyzerId);
        if (state.bridgeConnectionId() == null) {
            return new AnalyzerInstanceView(state, null, null);
        }
        try {
            ObjectNode connection = bridgeClient.getConnection(state.bridgeConnectionId());
            requireExactConnection(state, connection);
            return new AnalyzerInstanceView(state, connection, null);
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(state, null, exception.messageKey());
        }
    }

    @Override
    public AnalyzerInstanceView update(String analyzerId, AnalyzerInstanceRequest request, String actor) {
        AnalyzerInstanceState state = localStateService.update(analyzerId, request, actor);
        if (state.bridgeConnectionId() == null) {
            return createMissingConnection(state, request, actor);
        }
        try {
            ObjectNode current = bridgeClient.getConnection(state.bridgeConnectionId());
            requireExactConnection(state, current);
            ObjectNode updated = bridgeClient.updateConnection(state.bridgeConnectionId(),
                    updateConnectionRequest(state, request, current));
            requireExactConnection(state, updated);
            return new AnalyzerInstanceView(state, updated, null);
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(state, null, exception.messageKey());
        }
    }

    private AnalyzerInstanceView createMissingConnection(AnalyzerInstanceState state, AnalyzerInstanceRequest request,
            String actor) {
        try {
            ObjectNode connection = bridgeClient.createConnection(createConnectionRequest(state, request));
            requireExactConnection(state, connection);
            AnalyzerInstanceState connected = localStateService.attachBridgeConnection(state.analyzerId(),
                    connection.path("connectionId").asText(), actor);
            return new AnalyzerInstanceView(connected, connection, null);
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(state, null, exception.messageKey());
        }
    }

    private ObjectNode createConnectionRequest(AnalyzerInstanceState state, AnalyzerInstanceRequest request) {
        ObjectNode bridgeRequest = objectMapper.createObjectNode();
        bridgeRequest.put("schemaVersion", "1.0");
        bridgeRequest.put("requestId", requireText(requestIdSupplier.get(), "Bridge request ID"));
        bridgeRequest.put("clientAnalyzerId", state.analyzerId());
        bridgeRequest.put("displayName", state.name());
        bridgeRequest.putObject("profileRef").put("profileId", state.profileId())
                .put("revision", state.profileRevision()).put("fingerprint", state.profileFingerprint());
        ObjectNode values = request.getConnectionValues();
        bridgeRequest.set("values", values == null ? objectMapper.createObjectNode() : values);
        return bridgeRequest;
    }

    private ObjectNode updateConnectionRequest(AnalyzerInstanceState state, AnalyzerInstanceRequest request,
            ObjectNode current) {
        ObjectNode bridgeRequest = objectMapper.createObjectNode();
        bridgeRequest.put("schemaVersion", "1.0");
        bridgeRequest.put("requestId", requireText(requestIdSupplier.get(), "Bridge request ID"));
        bridgeRequest.put("connectionId", state.bridgeConnectionId());
        bridgeRequest.put("expectedConfigRevision", current.path("configRevision").asInt());
        bridgeRequest.put("displayName", state.name());
        bridgeRequest.putObject("profileRef").put("profileId", state.profileId())
                .put("revision", state.profileRevision()).put("fingerprint", state.profileFingerprint());
        ObjectNode values = objectMapper.createObjectNode();
        for (JsonNode field : current.path("fields")) {
            String key = field.path("key").asText(null);
            if (key != null && !key.isBlank() && field.has("currentValue")) {
                values.set(key, field.path("currentValue").deepCopy());
            }
        }
        ObjectNode requestedValues = request.getConnectionValues();
        if (requestedValues != null) {
            requestedValues.fields().forEachRemaining(entry -> values.set(entry.getKey(), entry.getValue().deepCopy()));
        }
        bridgeRequest.set("values", values);
        return bridgeRequest;
    }

    private static void requireExactConnection(AnalyzerInstanceState state, ObjectNode connection) {
        JsonNode profileRef = connection.path("profileRef");
        if (!Objects.equals(state.analyzerId(), connection.path("clientAnalyzerId").asText(null))
                || !Objects.equals(state.profileId(), profileRef.path("profileId").asText(null))
                || state.profileRevision() != profileRef.path("revision").asInt(0)
                || !Objects.equals(state.profileFingerprint(), profileRef.path("fingerprint").asText(null))) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidEvidence");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BridgeAnalyzerConnectionException("analyzer.bridge.connection.invalidRequest");
        }
        return value.trim();
    }
}
