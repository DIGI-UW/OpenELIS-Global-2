package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.form.AnalyzerInstanceRequest;
import org.openelisglobal.analyzer.valueholder.Analyzer;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerInstanceServiceTest {

    private static final String PROFILE_FINGERPRINT = "sha256:" + "1".repeat(64);

    private static final String ADOPTED_FINGERPRINT = "sha256:" + "5".repeat(64);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private AnalyzerInstanceLocalStateService localStateService;

    @Mock
    private BridgeAnalyzerConnectionClient bridgeClient;

    @Mock
    private AnalyzerActivationService activationService;

    @Mock
    private AnalyzerMappingEditorService mappingEditorService;

    private AnalyzerInstanceService service;
    private AnalyzerInstanceRequest request;
    private AnalyzerInstanceState localState;
    private ObjectNode bridgeConnection;

    @Before
    public void setUp() {
        request = new AnalyzerInstanceRequest();
        request.setName("Synthetic bench 1");
        request.setProfileId("fixture.synthetic-connection");
        request.setProfileRevision(3);
        request.setTestUnitIds(List.of("7"));

        localState = new AnalyzerInstanceState("42", "Synthetic bench 1", List.of("7"), "fixture.synthetic-connection",
                3, PROFILE_FINGERPRINT, null, Analyzer.AnalyzerStatus.SETUP, 0L);
        bridgeConnection = connection("bridge-connection-42");
        when(localStateService.create(request, "17")).thenReturn(localState);
        when(localStateService.get("42")).thenReturn(localState.withBridgeConnectionId("bridge-connection-42"));
        when(localStateService.update("42", request, "17"))
                .thenReturn(localState.withBridgeConnectionId("bridge-connection-42"));
        when(bridgeClient.createConnection(any(ObjectNode.class))).thenReturn(bridgeConnection);
        when(bridgeClient.getConnection("bridge-connection-42")).thenReturn(bridgeConnection);
        when(localStateService.attachBridgeConnection("42", "bridge-connection-42", "17"))
                .thenReturn(localState.withBridgeConnectionId("bridge-connection-42"));

        Supplier<String> requestIds = () -> "create-connection-42";
        service = new AnalyzerInstanceServiceImpl(localStateService, bridgeClient, activationService,
                mappingEditorService, requestIds);
    }

    @Test
    public void createsTheLocalIdentityBeforeRequestingAProfileDefaultedBridgeConnection() {
        AnalyzerInstanceView result = service.create(request, "17");

        ArgumentCaptor<ObjectNode> bridgeRequest = ArgumentCaptor.forClass(ObjectNode.class);
        InOrder order = inOrder(localStateService, bridgeClient);
        order.verify(localStateService).create(request, "17");
        order.verify(bridgeClient).createConnection(bridgeRequest.capture());
        order.verify(localStateService).attachBridgeConnection("42", "bridge-connection-42", "17");

        ObjectNode sent = bridgeRequest.getValue();
        assertEquals("1.0", sent.path("schemaVersion").asText());
        assertEquals("create-connection-42", sent.path("requestId").asText());
        assertEquals("42", sent.path("clientAnalyzerId").asText());
        assertEquals("Synthetic bench 1", sent.path("displayName").asText());
        assertEquals("fixture.synthetic-connection", sent.path("profileRef").path("profileId").asText());
        assertEquals(3, sent.path("profileRef").path("revision").asInt());
        assertEquals(PROFILE_FINGERPRINT, sent.path("profileRef").path("fingerprint").asText());
        assertEquals("a new analyzer has applied no instrument codes",
                JSON.createObjectNode().set("codeOverrides", JSON.createObjectNode()), sent.path("values"));
        assertEquals("bridge-connection-42", result.state().bridgeConnectionId());
        assertEquals(bridgeConnection, result.connection());
        assertNull(result.connectionErrorKey());
    }

    @Test
    public void forwardsSyntheticConnectionValuesWithoutInterpretingThem() {
        ObjectNode values = JSON.createObjectNode();
        values.put("futureTextField", "configured by the lab");
        values.put("futureNumberField", 4317);
        request.setConnectionValues(values);

        service.create(request, "17");

        ArgumentCaptor<ObjectNode> bridgeRequest = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridgeClient).createConnection(bridgeRequest.capture());
        ObjectNode expected = values.deepCopy();
        expected.putObject("codeOverrides");
        assertEquals(expected, bridgeRequest.getValue().path("values"));
    }

    @Test
    public void keepsTheCommittedLocalSetupAvailableWhenBridgeIsUnavailable() {
        when(bridgeClient.createConnection(any(ObjectNode.class)))
                .thenThrow(new BridgeAnalyzerConnectionException("analyzer.bridge.connection.unreachable"));

        AnalyzerInstanceView result = service.create(request, "17");

        assertEquals(localState, result.state());
        assertNull(result.connection());
        assertEquals("analyzer.bridge.connection.unreachable", result.connectionErrorKey());
        verify(localStateService, never()).attachBridgeConnection(any(), any(), any());
        assertFalse(result.connected());
    }

    @Test
    public void completingARetainedDraftCreatesAndStoresItsBridgeConnection() {
        when(localStateService.update("42", request, "17")).thenReturn(localState);

        AnalyzerInstanceView result = service.update("42", request, "17");

        ArgumentCaptor<ObjectNode> bridgeRequest = ArgumentCaptor.forClass(ObjectNode.class);
        InOrder order = inOrder(localStateService, bridgeClient);
        order.verify(localStateService).update("42", request, "17");
        order.verify(bridgeClient).createConnection(bridgeRequest.capture());
        order.verify(localStateService).attachBridgeConnection("42", "bridge-connection-42", "17");
        assertEquals("42", bridgeRequest.getValue().path("clientAnalyzerId").asText());
        assertEquals("fixture.synthetic-connection",
                bridgeRequest.getValue().path("profileRef").path("profileId").asText());
        assertEquals("bridge-connection-42", result.state().bridgeConnectionId());
        assertEquals(bridgeConnection, result.connection());
    }

    @Test
    public void reloadsTheReferencedBridgeConnectionWithoutPersistingItsValuesLocally() {
        AnalyzerInstanceView result = service.get("42");

        assertTrue(result.connected());
        assertEquals(bridgeConnection, result.connection());
        verify(localStateService).get("42");
        verify(bridgeClient).getConnection("bridge-connection-42");
    }

    @Test
    public void updatesOnlyTheRequestedSyntheticFieldsWithoutReplayingBridgeState() {
        bridgeConnection.withArray("fields").addObject().put("key", "futureTextField").put("currentValue", "original");
        bridgeConnection.withArray("fields").addObject().put("key", "futureNumberField").put("currentValue", 4100);
        ObjectNode requestedValues = JSON.createObjectNode();
        requestedValues.put("futureNumberField", 4317);
        request.setConnectionValues(requestedValues);
        ObjectNode updatedConnection = bridgeConnection.deepCopy();
        updatedConnection.put("configRevision", 2);
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(updatedConnection);

        AnalyzerInstanceView result = service.update("42", request, "17");

        ArgumentCaptor<ObjectNode> updateRequest = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridgeClient).updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                updateRequest.capture());
        ObjectNode sent = updateRequest.getValue();
        assertEquals(1, sent.path("expectedConfigRevision").asInt());
        assertFalse(sent.path("values").has("futureTextField"));
        assertEquals(4317, sent.path("values").path("futureNumberField").asInt());
        assertEquals(updatedConnection, result.connection());
        verify(localStateService).update("42", request, "17");
    }

    @Test
    public void appliesTheReviewedMappingWithoutRewritingBridgeConfiguration() {
        AnalyzerInstanceState connectedState = localState.withBridgeConnectionId("bridge-connection-42");
        when(localStateService.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17"))
                .thenReturn(connectedState);

        AnalyzerInstanceView result = service.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17");

        assertTrue(result.connected());
        assertEquals(bridgeConnection, result.connection());
        verify(bridgeClient).getConnection("bridge-connection-42");
        verify(bridgeClient, never()).updateConnection(any(), any());
    }

    @Test
    public void applyingAnAdoptedRevisionRepinsTheBridgeConnectionCarryingItsValues() {
        AnalyzerInstanceState adopted = new AnalyzerInstanceState("42", "Synthetic bench 1", List.of("7"),
                "fixture.synthetic-connection", 4, ADOPTED_FINGERPRINT, "bridge-connection-42",
                Analyzer.AnalyzerStatus.SETUP, 0L);
        when(localStateService.applyMapping("42", "13", 3, "sha256:" + "4".repeat(64), "17")).thenReturn(adopted);
        ObjectNode repinned = bridgeConnection.deepCopy();
        repinned.with("profileRef").put("revision", 4).put("fingerprint", ADOPTED_FINGERPRINT);
        repinned.put("configRevision", 2);
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(repinned);

        AnalyzerInstanceView result = service.applyMapping("42", "13", 3, "sha256:" + "4".repeat(64), "17");

        ArgumentCaptor<ObjectNode> updateRequest = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridgeClient).updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                updateRequest.capture());
        ObjectNode sent = updateRequest.getValue();
        assertEquals(4, sent.path("profileRef").path("revision").asInt());
        assertEquals(ADOPTED_FINGERPRINT, sent.path("profileRef").path("fingerprint").asText());
        assertEquals(1, sent.path("expectedConfigRevision").asInt());
        assertEquals("the Bridge carries the connection's other values forward", List.of("codeOverrides"),
                fieldNames(sent.path("values")));
        assertTrue(result.connected());
        assertEquals(repinned, result.connection());
    }

    @Test
    public void savingTheConnectionSendsTheAppliedInstrumentCodesInPlaceOfAnySupplied() {
        when(mappingEditorService.appliedInstrumentCodes("42")).thenReturn(java.util.Map.of("HIVVL", "HIVU"));
        ObjectNode requestedValues = JSON.createObjectNode();
        requestedValues.put("host", "192.0.2.10");
        requestedValues.putObject("codeOverrides").put("FLUA", "FLU-A");
        request.setConnectionValues(requestedValues);
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(withCodes(bridgeConnection, "HIVVL", "HIVU"));

        service.update("42", request, "17");

        ObjectNode sent = sentUpdate();
        assertEquals("192.0.2.10", sent.path("values").path("host").asText());
        assertEquals(codes("HIVVL", "HIVU"), sent.path("values").path("codeOverrides"));
    }

    @Test
    public void applyingSendsTheBridgeTheInstrumentCodesTheMappingNowHolds() {
        AnalyzerInstanceState connectedState = localState.withBridgeConnectionId("bridge-connection-42");
        when(localStateService.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17"))
                .thenReturn(connectedState);
        when(mappingEditorService.appliedInstrumentCodes("42")).thenReturn(java.util.Map.of("HIVVL", "HIVU"));
        ObjectNode renamed = withCodes(bridgeConnection, "HIVVL", "HIVU");
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(renamed);

        AnalyzerInstanceView result = service.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17");

        ObjectNode sent = sentUpdate();
        assertEquals(3, sent.path("profileRef").path("revision").asInt());
        assertEquals(codes("HIVVL", "HIVU"), sent.path("values").path("codeOverrides"));
        assertEquals(renamed, result.connection());
        assertNull(result.connectionErrorKey());
        verify(activationService, never()).reactivate(any(), any());
    }

    @Test
    public void applyingClearsCodesTheMappingNoLongerHolds() {
        AnalyzerInstanceState connectedState = localState.withBridgeConnectionId("bridge-connection-42");
        when(localStateService.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17"))
                .thenReturn(connectedState);
        when(bridgeClient.getConnection("bridge-connection-42"))
                .thenReturn(withCodes(bridgeConnection, "HIVVL", "HIVU"));
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(bridgeConnection);

        service.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17");

        assertEquals(JSON.createObjectNode(), sentUpdate().path("values").path("codeOverrides"));
    }

    @Test
    public void applyingNewCodesToARunningAnalyzerRestartsItOnThem() {
        AnalyzerInstanceState active = new AnalyzerInstanceState("42", "Synthetic bench 1", List.of("7"),
                "fixture.synthetic-connection", 3, PROFILE_FINGERPRINT, "bridge-connection-42",
                Analyzer.AnalyzerStatus.ACTIVE, 0L);
        when(localStateService.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17")).thenReturn(active);
        when(mappingEditorService.appliedInstrumentCodes("42")).thenReturn(java.util.Map.of("HIVVL", "HIVU"));
        when(bridgeClient.updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class))).thenReturn(withCodes(bridgeConnection, "HIVVL", "HIVU"));
        when(activationService.reactivate("42", "17"))
                .thenReturn(new AnalyzerActivationResult("42", Analyzer.AnalyzerStatus.ACTIVE, true, true, List.of()));

        AnalyzerInstanceView result = service.applyMapping("42", "12", 2, "sha256:" + "3".repeat(64), "17");

        InOrder order = inOrder(bridgeClient, activationService);
        order.verify(bridgeClient).updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                any(ObjectNode.class));
        order.verify(activationService).reactivate("42", "17");
        assertNull(result.connectionErrorKey());
    }

    private ObjectNode sentUpdate() {
        ArgumentCaptor<ObjectNode> updateRequest = ArgumentCaptor.forClass(ObjectNode.class);
        verify(bridgeClient).updateConnection(org.mockito.ArgumentMatchers.eq("bridge-connection-42"),
                updateRequest.capture());
        return updateRequest.getValue();
    }

    private static ObjectNode codes(String profileCode, String instrumentCode) {
        ObjectNode codes = JSON.createObjectNode();
        codes.put(profileCode, instrumentCode);
        return codes;
    }

    private static ObjectNode withCodes(ObjectNode connection, String profileCode, String instrumentCode) {
        ObjectNode renamed = connection.deepCopy();
        renamed.set("codeOverrides", codes(profileCode, instrumentCode));
        renamed.put("configRevision", connection.path("configRevision").asInt() + 1);
        return renamed;
    }

    private static List<String> fieldNames(com.fasterxml.jackson.databind.JsonNode node) {
        List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static ObjectNode connection(String connectionId) {
        ObjectNode connection = JSON.createObjectNode();
        connection.put("schemaVersion", "1.0");
        connection.put("connectionId", connectionId);
        connection.put("clientAnalyzerId", "42");
        connection.put("displayName", "Synthetic bench 1");
        connection.putObject("profileRef").put("profileId", "fixture.synthetic-connection").put("revision", 3)
                .put("fingerprint", PROFILE_FINGERPRINT);
        connection.put("configRevision", 1);
        connection.put("configFingerprint", "sha256:" + "2".repeat(64));
        connection.putArray("fields");
        connection.putObject("readiness").put("ready", true).putArray("blockers");
        connection.put("desiredRuntimeState", "INACTIVE");
        connection.put("actualRuntimeState", "INACTIVE");
        connection.put("updatedAt", "2026-08-24T20:00:00Z");
        return connection;
    }
}
