package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyzerInstanceServiceImpl implements AnalyzerInstanceService {

    private final AnalyzerInstanceLocalStateService localStateService;
    private final BridgeAnalyzerConnectionClient bridgeClient;
    private final AnalyzerActivationService activationService;
    private final Supplier<String> requestIdSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationService activationService) {
        this(localStateService, bridgeClient, activationService, () -> UUID.randomUUID().toString());
    }

    AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationService activationService,
            Supplier<String> requestIdSupplier) {
        this.localStateService = localStateService;
        this.bridgeClient = bridgeClient;
        this.activationService = activationService;
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
        return compose(state);
    }

    @Override
    @Transactional
    public AnalyzerInstanceView applyMapping(String analyzerId, String mappingId, int revision,
            String mappingFingerprint, String actor) {
        AnalyzerInstanceState before = localStateService.get(analyzerId);
        AnalyzerInstanceState state = localStateService.applyMapping(analyzerId, mappingId, revision,
                mappingFingerprint, actor);
        if (state.bridgeConnectionId() == null) {
            return compose(state);
        }
        boolean revisionChanged = before.profileRevision() != state.profileRevision();
        // OE2 and the Bridge switch together: any Bridge failure below rolls this
        // transaction back, and a connection that may have moved is put back first.
        // An active connection keeps running its old configuration until it is
        // re-activated on the new one.
        ObjectNode current = bridgeClient.getConnection(state.bridgeConnectionId());
        boolean moving = !pinnedTo(state, current);
        try {
            ObjectNode connection = moving
                    ? bridgeClient.updateConnection(state.bridgeConnectionId(),
                            updateConnectionRequest(state, pin(state), objectMapper.createObjectNode(), current))
                    : current;
            requireExactConnection(state, connection);
            if (state.status() == Analyzer.AnalyzerStatus.ACTIVE && (moving || revisionChanged)) {
                String blocker = reactivate(state, actor);
                if (blocker != null) {
                    throw new BridgeAnalyzerConnectionException(blocker);
                }
            }
            return new AnalyzerInstanceView(state, connection, null);
        } catch (RuntimeException exception) {
            if (moving) {
                putBack(state, current, exception);
            }
            throw exception;
        }
    }

    /** Null once the connection runs again, otherwise why it does not. */
    private String reactivate(AnalyzerInstanceState state, String actor) {
        AnalyzerActivationResult reactivated = activationService.reactivate(state.analyzerId(), actor);
        if (reactivated.activated()) {
            return null;
        }
        return reactivated.blockers().isEmpty() ? "analyzer.activation.blocker.bridgeAcknowledgement"
                : reactivated.blockers().get(0).code();
    }

    /**
     * Puts the connection back on {@code previous}'s pin if this request moved it.
     * The Bridge can make a change whose answer is then lost, so the connection is
     * read again rather than trusted. A pin that is neither this request's nor the
     * previous one was set by someone else and is left alone. When the connection
     * is not back on its previous pin, OE2 and the Bridge may disagree, and that is
     * what the operator is told.
     */
    private void putBack(AnalyzerInstanceState state, ObjectNode previous, RuntimeException original) {
        ObjectNode now;
        try {
            now = bridgeClient.getConnection(state.bridgeConnectionId());
        } catch (RuntimeException rereadFailure) {
            throw reconcileRequired(original, rereadFailure);
        }
        if (now.path("profileRef").equals(previous.path("profileRef"))) {
            return;
        }
        if (!pinnedTo(state, now)) {
            throw reconcileRequired(original, null);
        }
        try {
            bridgeClient.updateConnection(state.bridgeConnectionId(),
                    updateConnectionRequest(state, previous.path("profileRef"), objectMapper.createObjectNode(), now));
        } catch (RuntimeException putBackFailure) {
            throw reconcileRequired(original, putBackFailure);
        }
    }

    private static BridgeAnalyzerConnectionException reconcileRequired(RuntimeException original,
            RuntimeException failure) {
        BridgeAnalyzerConnectionException reconcile = new BridgeAnalyzerConnectionException(
                "analyzer.bridge.connection.reconcileRequired", Map.of(), original);
        if (failure != null) {
            reconcile.addSuppressed(failure);
        }
        return reconcile;
    }

    private ObjectNode connectionValues(AnalyzerInstanceRequest request) {
        return request.getConnectionValues() == null ? objectMapper.createObjectNode()
                : request.getConnectionValues().deepCopy();
    }

    private ObjectNode pin(AnalyzerInstanceState state) {
        ObjectNode profileRef = objectMapper.createObjectNode();
        profileRef.put("profileId", state.profileId()).put("revision", state.profileRevision()).put("fingerprint",
                state.profileFingerprint());
        return profileRef;
    }

    private static boolean pinnedTo(AnalyzerInstanceState state, ObjectNode connection) {
        JsonNode profileRef = connection.path("profileRef");
        return Objects.equals(state.profileId(), profileRef.path("profileId").asText(null))
                && state.profileRevision() == profileRef.path("revision").asInt(0)
                && Objects.equals(state.profileFingerprint(), profileRef.path("fingerprint").asText(null));
    }

    private AnalyzerInstanceView compose(AnalyzerInstanceState state) {
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
                    updateConnectionRequest(state, pin(state), connectionValues(request), current));
            requireExactConnection(state, updated);
            return new AnalyzerInstanceView(state, updated, null);
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(state, null, exception.messageKey());
        }
    }

    @Override
    public AnalyzerInstanceView ensureConnection(String analyzerId, ObjectNode values, String actor) {
        AnalyzerInstanceState state = localStateService.get(analyzerId);
        if (state.bridgeConnectionId() != null)
            return compose(state);
        AnalyzerInstanceRequest request = new AnalyzerInstanceRequest();
        request.setConnectionValues(values);
        return createMissingConnection(state, request, actor);
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
        bridgeRequest.set("values", connectionValues(request));
        return bridgeRequest;
    }

    private ObjectNode updateConnectionRequest(AnalyzerInstanceState state, JsonNode profileRef, ObjectNode values,
            ObjectNode current) {
        ObjectNode bridgeRequest = objectMapper.createObjectNode();
        bridgeRequest.put("schemaVersion", "1.0");
        bridgeRequest.put("requestId", requireText(requestIdSupplier.get(), "Bridge request ID"));
        bridgeRequest.put("connectionId", state.bridgeConnectionId());
        bridgeRequest.put("expectedConfigRevision", current.path("configRevision").asInt());
        bridgeRequest.put("displayName", state.name());
        bridgeRequest.set("profileRef", profileRef.deepCopy());
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
