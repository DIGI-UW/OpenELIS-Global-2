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
    public void createDraftSuppliesAuthenticatedActorAndLetsBridgeGenerateIdentity() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/drafts"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(201,
                        "{\"draftId\":\"draft-1\",\"profile\":{\"profileMeta\":{\"id\":\"site.generated\"}}}"));

        service.createDraft("Site Mock Analyzer", "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/drafts"), body.capture(),
                any(Duration.class));
        JsonNode forwarded = objectMapper.readTree(body.getValue());
        assertEquals("17", forwarded.path("actor").asText());
        assertEquals("Site Mock Analyzer", forwarded.path("displayName").asText());
        assertEquals(false, forwarded.has("profileId"));
        assertEquals(false, forwarded.has("profile"));
    }

    @Test
    public void duplicateForwardsExplicitSourceRevisionAndLetsBridgeGenerateIdentity() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/site.mock/duplicate"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(201,
                        "{\"draftId\":\"draft-2\",\"profile\":{\"profileMeta\":{\"id\":\"site.generated\"}}}"));

        service.duplicate("site.mock", 3, "Mock Analyzer -1", "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/site.mock/duplicate"), body.capture(),
                any(Duration.class));
        JsonNode forwarded = objectMapper.readTree(body.getValue());
        assertEquals("17", forwarded.path("actor").asText());
        assertEquals(3, forwarded.path("sourceRevision").asInt());
        assertEquals("Mock Analyzer -1", forwarded.path("displayName").asText());
        assertEquals(false, forwarded.has("targetProfileId"));
    }

    @Test
    public void updateSharedStartsFromTheExactPublishedRevision() throws Exception {
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/site.mock/update"), any(String.class),
                any(Duration.class))).thenReturn(new BridgeHttpClient.BridgeResponse(201,
                        "{\"draftId\":\"draft-3\",\"kind\":\"UPDATE\"}"));

        service.updateShared("site.mock", 3, "17");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/site.mock/update"), body.capture(),
                any(Duration.class));
        JsonNode forwarded = objectMapper.readTree(body.getValue());
        assertEquals("17", forwarded.path("actor").asText());
        assertEquals(3, forwarded.path("sourceRevision").asInt());
        assertEquals(false, forwarded.has("profile"));
    }

    @Test
    public void updateAndPublishDraftUseTheBridgeDraftLifecycle() throws Exception {
        JsonNode profile = objectMapper.readTree("{\"profileMeta\":{\"displayName\":\"Site Mock Analyzer\"}}");
        when(bridgeHttpClient.put(eq("https://bridge.example/api/profiles/drafts/draft-1"), any(String.class),
                any(Duration.class))).thenReturn(
                        new BridgeHttpClient.BridgeResponse(200, "{\"draftId\":\"draft-1\",\"validationIssues\":[]}"));
        when(bridgeHttpClient.post(eq("https://bridge.example/api/profiles/drafts/draft-1/publish"), any(String.class),
                any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(201, "{\"profile\":{\"catalog\":{\"revision\":1}}}"));

        service.updateDraft("draft-1", profile, "17");
        service.publishDraft("draft-1", "17");

        ArgumentCaptor<String> updateBody = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).put(eq("https://bridge.example/api/profiles/drafts/draft-1"), updateBody.capture(),
                any(Duration.class));
        JsonNode update = objectMapper.readTree(updateBody.getValue());
        assertEquals("17", update.path("actor").asText());
        assertEquals("Site Mock Analyzer", update.path("profile").path("profileMeta").path("displayName").asText());

        ArgumentCaptor<String> publishBody = ArgumentCaptor.forClass(String.class);
        verify(bridgeHttpClient).post(eq("https://bridge.example/api/profiles/drafts/draft-1/publish"),
                publishBody.capture(), any(Duration.class));
        assertEquals("17", objectMapper.readTree(publishBody.getValue()).path("actor").asText());
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
    public void getDraftReadsTheExactBridgeOwnedDraft() throws Exception {
        when(bridgeHttpClient.get(eq("https://bridge.example/api/profiles/drafts/draft-1"), any(Duration.class)))
                .thenReturn(new BridgeHttpClient.BridgeResponse(200,
                        "{\"draftId\":\"draft-1\",\"kind\":\"DUPLICATE\"}"));

        JsonNode draft = service.getDraft("draft-1");

        assertEquals("draft-1", draft.path("draftId").asText());
        assertEquals("DUPLICATE", draft.path("kind").asText());
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

    @Test
    public void missingBridgeUrlProducesDeterministicServiceUnavailableFailure() {
        service = new BridgeProfileManagementServiceImpl(bridgeHttpClient, " ");

        BridgeProfileManagementException exception = assertThrows(BridgeProfileManagementException.class,
                () -> service.history("site.mock"));

        assertEquals(503, exception.getStatus());
        assertEquals("Bridge URL is not configured", exception.getMessage());
    }
}
