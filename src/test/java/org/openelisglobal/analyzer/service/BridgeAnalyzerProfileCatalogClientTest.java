package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class BridgeAnalyzerProfileCatalogClientTest {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Path CATALOG_FIXTURE = Path.of("tools", "openelis-analyzer-bridge", "contracts", "analyzer",
            "v1", "fixtures", "profile-catalog-entry.json");

    private BridgeHttpClient bridgeHttpClient;
    private String catalogEntryJson;

    @Before
    public void setUp() throws Exception {
        bridgeHttpClient = mock(BridgeHttpClient.class);
        catalogEntryJson = Files.readString(CATALOG_FIXTURE);
    }

    @Test
    public void listsVersionedProfilesUsingBridgeFilters() throws Exception {
        String responseBody = "[" + catalogEntryJson + "]";
        when(bridgeHttpClient.get(anyString(), eq(READ_TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, responseBody));
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient,
                "https://bridge.test/");

        List<BridgeProfileCatalogEntry> entries = client
                .list(new AnalyzerProfileCatalogFilter("Gene Xpert", "SHIPPED", "ACTIVE", "ASTM"));

        assertEquals(1, entries.size());
        BridgeProfileCatalogEntry entry = entries.get(0);
        assertEquals("site.mock-hematology", entry.profile().path("profileId").asText());
        assertEquals(1, entry.profile().path("revision").asInt());
        assertEquals("SITE", entry.profile().path("source").asText());
        assertEquals("ACTIVE", entry.profile().path("status").asText());
        assertEquals("CREATED", entry.audit().action());
        assertEquals("oe-user", entry.audit().actor());
        assertEquals(Instant.parse("2026-08-14T02:00:00Z"), entry.audit().markedAt());
        assertEquals("sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", entry.fingerprint());

        ArgumentCaptor<String> endpoint = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).get(endpoint.capture(), eq(READ_TIMEOUT));
        assertEquals("https://bridge.test/api/profiles?q=Gene%20Xpert&source=SHIPPED&status=ACTIVE&protocol=ASTM",
                endpoint.getValue());
    }

    @Test
    public void failsClosedWhenBridgeIsNotConfigured() throws Exception {
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient, " ");

        AnalyzerProfileCatalogException exception = assertThrows(AnalyzerProfileCatalogException.class,
                () -> client.list(AnalyzerProfileCatalogFilter.empty()));

        assertEquals("Analyzer Bridge URL is not configured", exception.getMessage());
        verify(bridgeHttpClient, never()).get(anyString(), eq(READ_TIMEOUT));
    }

    @Test
    public void rejectsNonSuccessfulBridgeResponses() throws Exception {
        when(bridgeHttpClient.get(anyString(), eq(READ_TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(503, "unavailable"));
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient,
                "https://bridge.test");

        AnalyzerProfileCatalogException exception = assertThrows(AnalyzerProfileCatalogException.class,
                () -> client.list(AnalyzerProfileCatalogFilter.empty()));

        assertEquals("Analyzer Bridge profile catalog returned HTTP 503", exception.getMessage());
    }

    @Test
    public void getsVersionedProfileAndHistory() throws Exception {
        when(bridgeHttpClient.get("https://bridge.test/api/profiles/site.mock-hematology?revision=1", READ_TIMEOUT))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, catalogEntryJson));
        when(bridgeHttpClient.get("https://bridge.test/api/profiles/site.mock-hematology/history", READ_TIMEOUT))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "[" + catalogEntryJson + "]"));
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient,
                "https://bridge.test");

        BridgeProfileCatalogEntry entry = client.get("site.mock-hematology", 1);
        List<BridgeProfileCatalogEntry> history = client.history("site.mock-hematology");

        assertEquals("site.mock-hematology", entry.profile().path("profileId").asText());
        assertEquals(1, history.size());
        assertEquals(1, history.get(0).profile().path("revision").asInt());
    }

    @Test
    public void sendsActorForDeactivateAndReactivate() throws Exception {
        when(bridgeHttpClient.post(anyString(), anyString(), eq(READ_TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, catalogEntryJson));
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient,
                "https://bridge.test");

        client.deactivate("site.mock-hematology", "42");
        client.reactivate("site.mock-hematology", "42");

        ArgumentCaptor<String> endpoint = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient, org.mockito.Mockito.times(2)).post(endpoint.capture(), body.capture(), eq(READ_TIMEOUT));
        assertEquals(List.of("https://bridge.test/api/profiles/site.mock-hematology/deactivate",
                "https://bridge.test/api/profiles/site.mock-hematology/reactivate"), endpoint.getAllValues());
        ObjectMapper objectMapper = new ObjectMapper();
        for (String requestBody : body.getAllValues()) {
            assertEquals("42", objectMapper.readTree(requestBody).path("actor").asText());
        }
    }

    @Test
    public void forksFromExplicitRevisionWithNewPublicIdentity() throws Exception {
        when(bridgeHttpClient.post(anyString(), anyString(), eq(READ_TIMEOUT)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(201, catalogEntryJson));
        AnalyzerProfileCatalogClient client = new BridgeAnalyzerProfileCatalogClient(bridgeHttpClient,
                "https://bridge.test");

        client.fork("site.mock-hematology",
                new AnalyzerProfileForkRequest(1, "site.mock-hematology-1", "Mock Hematology -1"), "42");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.test/api/profiles/site.mock-hematology/fork"),
                body.capture(), eq(READ_TIMEOUT));
        JsonNode request = new ObjectMapper().readTree(body.getValue());
        assertEquals("42", request.path("actor").asText());
        assertEquals(1, request.path("sourceRevision").asInt());
        assertEquals("site.mock-hematology-1", request.path("profileId").asText());
        assertEquals("Mock Hematology -1", request.path("displayName").asText());
    }
}
