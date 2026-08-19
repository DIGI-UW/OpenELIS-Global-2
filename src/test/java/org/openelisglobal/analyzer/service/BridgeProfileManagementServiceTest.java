package org.openelisglobal.analyzer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BridgeProfileManagementServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private BridgeHttpClient bridgeHttpClient;

    private BridgeProfileManagementService service;

    @Before
    public void setUp() {
        service = new BridgeProfileManagementServiceImpl(bridgeHttpClient, "https://bridge.example/");
    }

    @Test
    public void createSuppliesAuthenticatedActorWithoutCopyingProfileLocally() throws Exception {
        JsonNode profile = objectMapper.readTree("{\"profileId\":\"site.mock\"}");
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles"), any(String.class), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(201, "{\"profile\":{\"profileId\":\"site.mock\"}}"));

        service.create(profile, "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles"), body.capture(), any(Duration.class));
        JsonNode forwarded = objectMapper.readTree(body.getValue());
        assertEquals("17", forwarded.path("actor").asText());
        assertEquals("site.mock", forwarded.path("profile").path("profileId").asText());
    }

    @Test
    public void duplicateForwardsExplicitSourceRevisionAndNewIdentity() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/site.mock/duplicate"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(201,
                        "{\"profile\":{\"profileId\":\"site.mock-1\"}}"));

        service.duplicate("site.mock", 3, "site.mock-1", "Mock Analyzer -1", "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/site.mock/duplicate"), body.capture(),
                any(Duration.class));
        JsonNode forwarded = objectMapper.readTree(body.getValue());
        assertEquals("17", forwarded.path("actor").asText());
        assertEquals(3, forwarded.path("sourceRevision").asInt());
        assertEquals("site.mock-1", forwarded.path("targetProfileId").asText());
        assertEquals("Mock Analyzer -1", forwarded.path("displayName").asText());
    }

    @Test
    public void deactivateForwardsActorAndHasNoDeletePath() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/site.mock/deactivate"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(200,
                        "{\"profile\":{\"status\":\"INACTIVE\"}}"));

        service.deactivate("site.mock", "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/site.mock/deactivate"), body.capture(),
                any(Duration.class));
        assertEquals("17", objectMapper.readTree(body.getValue()).path("actor").asText());
    }

    @Test
    public void historyReadsRetainedBridgeRevisions() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles/site.mock/history"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200, "[]"));

        JsonNode history = service.history("site.mock");

        assertEquals(true, history.isArray());
    }

    @Test
    public void bridgeValidationFailureRemainsAClientVisibleFailure() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/site.mock/reactivate"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(400,
                        "{\"error\":\"profile is already active\"}"));

        BridgeProfileManagementException exception = assertThrows(BridgeProfileManagementException.class,
                () -> service.reactivate("site.mock", "17"));

        assertEquals(400, exception.getStatus());
        assertEquals("profile is already active", exception.getMessage());
    }
}
