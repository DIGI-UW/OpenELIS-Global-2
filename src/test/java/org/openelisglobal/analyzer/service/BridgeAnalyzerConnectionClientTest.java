package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeAnalyzerConnectionClientTest {

    private static final String BASE_URL = "https://bridge.example";
    private static final String CONNECTION_ID = "bridge-connection-7f3c";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private BridgeHttpClient httpClient;

    private BridgeAnalyzerConnectionClient client;

    @Before
    public void setUp() {
        client = new BridgeAnalyzerConnectionClient(httpClient, BASE_URL + "/");
    }

    @Test
    public void readsOneDurableBridgeConnection() throws Exception {
        when(httpClient.get(eq(BASE_URL + "/api/connections/" + CONNECTION_ID), eq(Duration.ofSeconds(10))))
                .thenReturn(success(fixture("analyzer-connection.json")));

        ObjectNode connection = client.getConnection(CONNECTION_ID);

        assertEquals(CONNECTION_ID, connection.path("connectionId").asText());
        assertEquals(4, connection.path("configRevision").asInt());
    }

    @Test
    public void createsAndUpdatesUsingOnlyGenericConnectionDocuments() throws Exception {
        ObjectNode create = fixture("connection-create.json");
        ObjectNode update = fixture("connection-update.json");
        ObjectNode response = fixture("analyzer-connection.json");
        when(httpClient.post(eq(BASE_URL + "/api/connections"), eq(JSON.writeValueAsString(create)),
                eq(Duration.ofSeconds(10)))).thenReturn(success(response));
        when(httpClient.put(eq(BASE_URL + "/api/connections/" + CONNECTION_ID), eq(JSON.writeValueAsString(update)),
                eq(Duration.ofSeconds(10)))).thenReturn(success(response));

        assertEquals(CONNECTION_ID, client.createConnection(create).path("connectionId").asText());
        assertEquals(CONNECTION_ID, client.updateConnection(CONNECTION_ID, update).path("connectionId").asText());
    }

    @Test
    public void probesTheExactSavedConfigRevision() throws Exception {
        ObjectNode response = fixture("connection-probe-result.json");
        when(httpClient.post(eq(BASE_URL + "/api/connections/" + CONNECTION_ID + "/probe"),
                org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(10)))).thenReturn(success(response));

        ObjectNode result = client.probe(CONNECTION_ID, 4, "probe-fixture-004");

        ArgumentCaptor<String> request = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(eq(BASE_URL + "/api/connections/" + CONNECTION_ID + "/probe"), request.capture(),
                eq(Duration.ofSeconds(10)));
        JsonNode body = JSON.readTree(request.getValue());
        assertEquals("1.0", body.path("schemaVersion").asText());
        assertEquals("probe-fixture-004", body.path("requestId").asText());
        assertEquals(CONNECTION_ID, body.path("connectionId").asText());
        assertEquals(4, body.path("expectedConfigRevision").asInt());
        assertEquals("SUCCEEDED", result.path("status").asText());
    }

    @Test
    public void appliesAnExactRuntimeCommand() throws Exception {
        ObjectNode response = fixture("connection-activate-ack.json");
        when(httpClient.post(eq(BASE_URL + "/api/connections/" + CONNECTION_ID + "/runtime"),
                org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(10)))).thenReturn(success(response));

        ObjectNode acknowledgement = client.applyRuntimeCommand(CONNECTION_ID, 4, "ACTIVATE", "activate-fixture-004");

        ArgumentCaptor<String> request = ArgumentCaptor.forClass(String.class);
        verify(httpClient).post(eq(BASE_URL + "/api/connections/" + CONNECTION_ID + "/runtime"), request.capture(),
                eq(Duration.ofSeconds(10)));
        JsonNode body = JSON.readTree(request.getValue());
        assertEquals("activate-fixture-004", body.path("commandId").asText());
        assertEquals("ACTIVATE", body.path("action").asText());
        assertEquals(4, body.path("expectedConfigRevision").asInt());
        assertEquals("APPLIED", acknowledgement.path("outcome").asText());
    }

    @Test
    public void rejectsEvidenceForAnotherConnection() throws Exception {
        ObjectNode response = fixture("connection-probe-result.json");
        response.put("connectionId", "another-connection");
        when(httpClient.post(eq(BASE_URL + "/api/connections/" + CONNECTION_ID + "/probe"),
                org.mockito.ArgumentMatchers.anyString(), eq(Duration.ofSeconds(10)))).thenReturn(success(response));

        BridgeAnalyzerConnectionException exception = assertThrows(BridgeAnalyzerConnectionException.class,
                () -> client.probe(CONNECTION_ID, 4, "probe-fixture-004"));

        assertEquals("analyzer.bridge.connection.invalidEvidence", exception.messageKey());
    }

    private static BridgeHttpClient.BridgeResponse success(ObjectNode body) throws Exception {
        return new BridgeHttpClient.BridgeResponse(200, JSON.writeValueAsString(body));
    }

    private static ObjectNode fixture(String name) throws Exception {
        String json = Files.readString(
                Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer", "v1", "fixtures", name));
        return (ObjectNode) JSON.readTree(json);
    }
}
