package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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
    private final AnalyzerMappingEditorService mappingEditorService;
    private final Supplier<String> requestIdSupplier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationService activationService,
            AnalyzerMappingEditorService mappingEditorService) {
        this(localStateService, bridgeClient, activationService, mappingEditorService,
                () -> UUID.randomUUID().toString());
    }

    AnalyzerInstanceServiceImpl(AnalyzerInstanceLocalStateService localStateService,
            BridgeAnalyzerConnectionClient bridgeClient, AnalyzerActivationService activationService,
            AnalyzerMappingEditorService mappingEditorService, Supplier<String> requestIdSupplier) {
        this.localStateService = localStateService;
        this.bridgeClient = bridgeClient;
        this.activationService = activationService;
        this.mappingEditorService = mappingEditorService;
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
        ObjectNode codes = instrumentCodes(state);
        if (before.profileRevision() == state.profileRevision()) {
            return syncConnection(state, codes, actor);
        }
        // An adopted revision moves the pin, and OE2 and the Bridge switch together:
        // any Bridge failure below rolls this transaction back, and a connection that
        // already moved is pinned back first. An active connection keeps receiving on
        // the old revision until it is re-activated on the new one.
        ObjectNode current = bridgeClient.getConnection(state.bridgeConnectionId());
        ObjectNode moved = inForce(state, current, codes) ? current
                : bridgeClient.updateConnection(state.bridgeConnectionId(),
                        updateConnectionRequest(state, pin(state), withCodes(codes), current));
        try {
            requireExactConnection(state, moved);
            if (state.status() == Analyzer.AnalyzerStatus.ACTIVE) {
                String blocker = reactivate(state, actor);
                if (blocker != null) {
                    throw new BridgeAnalyzerConnectionException(blocker);
                }
            }
            return new AnalyzerInstanceView(state, moved, null);
        } catch (RuntimeException exception) {
            if (moved != current) {
                pinBack(state, current, moved, exception);
            }
            throw exception;
        }
    }

    /**
     * Brings the connection to the applied pin and instrument codes. An active
     * connection keeps its old codes until it is re-activated on the new ones.
     */
    private AnalyzerInstanceView syncConnection(AnalyzerInstanceState state, ObjectNode codes, String actor) {
        try {
            ObjectNode current = bridgeClient.getConnection(state.bridgeConnectionId());
            if (inForce(state, current, codes)) {
                requireExactConnection(state, current);
                return new AnalyzerInstanceView(state, current, null);
            }
            ObjectNode synced = bridgeClient.updateConnection(state.bridgeConnectionId(),
                    updateConnectionRequest(state, pin(state), withCodes(codes), current));
            requireExactConnection(state, synced);
            String blocker = state.status() == Analyzer.AnalyzerStatus.ACTIVE ? reactivate(state, actor) : null;
            return new AnalyzerInstanceView(state, synced, blocker);
        } catch (BridgeAnalyzerConnectionException exception) {
            return new AnalyzerInstanceView(state, null, exception.messageKey());
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

    private void pinBack(AnalyzerInstanceState state, ObjectNode previous, ObjectNode moved,
            RuntimeException original) {
        try {
            bridgeClient.updateConnection(state.bridgeConnectionId(),
                    updateConnectionRequest(state, previous.path("profileRef"), withCodes(codesOf(previous)), moved));
        } catch (RuntimeException pinBackFailure) {
            original.addSuppressed(pinBackFailure);
        }
    }

    private boolean inForce(AnalyzerInstanceState state, ObjectNode connection, ObjectNode codes) {
        return pinnedTo(state, connection) && codesOf(connection).equals(codes);
    }

    /**
     * The applied mapping's instrument codes, which the Bridge holds as
     * codeOverrides.
     */
    private ObjectNode instrumentCodes(AnalyzerInstanceState state) {
        ObjectNode codes = objectMapper.createObjectNode();
        mappingEditorService.appliedInstrumentCodes(state.analyzerId()).forEach(codes::put);
        return codes;
    }

    private ObjectNode codesOf(ObjectNode connection) {
        JsonNode codes = connection.path("codeOverrides");
        return codes.isObject() ? (ObjectNode) codes : objectMapper.createObjectNode();
    }

    private ObjectNode withCodes(ObjectNode codes) {
        ObjectNode values = objectMapper.createObjectNode();
        values.set("codeOverrides", codes.deepCopy());
        return values;
    }

    /**
     * The values the lab entered, with the instrument codes OE2 owns in place of
     * any supplied.
     */
    private ObjectNode connectionValues(AnalyzerInstanceState state, AnalyzerInstanceRequest request) {
        ObjectNode values = request.getConnectionValues() == null ? objectMapper.createObjectNode()
                : request.getConnectionValues().deepCopy();
        values.set("codeOverrides", instrumentCodes(state));
        return values;
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
                    updateConnectionRequest(state, pin(state), connectionValues(state, request), current));
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
        bridgeRequest.set("values", connectionValues(state, request));
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
