package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BridgeAnalyzerProfileCatalogClient implements AnalyzerProfileCatalogClient {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final BridgeHttpClient bridgeHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final String bridgeBaseUrl;

    public BridgeAnalyzerProfileCatalogClient(BridgeHttpClient bridgeHttpClient,
            @Value("${analyzer.bridge.url:}") String bridgeBaseUrl) {
        this.bridgeHttpClient = bridgeHttpClient;
        this.bridgeBaseUrl = bridgeBaseUrl == null ? "" : bridgeBaseUrl.trim();
    }

    @Override
    public List<BridgeProfileCatalogEntry> list(AnalyzerProfileCatalogFilter filter) {
        if (bridgeBaseUrl.isBlank()) {
            throw new AnalyzerProfileCatalogException("Analyzer Bridge URL is not configured");
        }

        AnalyzerProfileCatalogFilter effectiveFilter = filter == null ? AnalyzerProfileCatalogFilter.empty() : filter;
        UriComponentsBuilder endpoint = UriComponentsBuilder.fromUriString(withoutTrailingSlash(bridgeBaseUrl))
                .path("/api/profiles");
        addQueryParameter(endpoint, "q", effectiveFilter.query());
        addQueryParameter(endpoint, "source", effectiveFilter.source());
        addQueryParameter(endpoint, "status", effectiveFilter.status());
        addQueryParameter(endpoint, "protocol", effectiveFilter.protocol());

        BridgeHttpClient.BridgeResponse response;
        try {
            response = bridgeHttpClient.get(endpoint.build().encode().toUriString(), READ_TIMEOUT);
        } catch (IOException e) {
            throw new AnalyzerProfileCatalogException("Unable to read the Analyzer Bridge profile catalog", e);
        }
        if (!response.isSuccess()) {
            throw new AnalyzerProfileCatalogException(
                    "Analyzer Bridge profile catalog returned HTTP " + response.status);
        }

        try {
            List<BridgeProfileCatalogEntry> entries = objectMapper.readValue(response.body,
                    new TypeReference<List<BridgeProfileCatalogEntry>>() {
                    });
            return List.copyOf(entries);
        } catch (JsonProcessingException e) {
            throw new AnalyzerProfileCatalogException("Analyzer Bridge returned an invalid profile catalog", e);
        }
    }

    @Override
    public BridgeProfileCatalogEntry get(String profileId, Integer revision) {
        UriComponentsBuilder endpoint = profileEndpoint(profileId);
        if (revision != null) {
            endpoint.queryParam("revision", revision);
        }
        return readEntry(get(endpoint.build().encode().toUriString(), "read Analyzer Bridge profile"));
    }

    @Override
    public List<BridgeProfileCatalogEntry> history(String profileId) {
        String endpoint = profileEndpoint(profileId).pathSegment("history").build().encode().toUriString();
        return readEntries(get(endpoint, "read Analyzer Bridge profile history"));
    }

    @Override
    public BridgeProfileCatalogEntry fork(String profileId, AnalyzerProfileForkRequest request, String actor) {
        if (request == null) {
            throw new AnalyzerProfileCatalogException("Analyzer profile fork request is required");
        }
        String endpoint = profileEndpoint(profileId).pathSegment("fork").build().encode().toUriString();
        return postEntry(endpoint,
                new ForkPayload(requireActor(actor), request.sourceRevision(), requireProfileId(request.profileId()),
                        required(request.displayName(), "Analyzer profile display name is required")),
                "fork Analyzer Bridge profile");
    }

    @Override
    public BridgeProfileCatalogEntry deactivate(String profileId, String actor) {
        return changeStatus(profileId, actor, "deactivate");
    }

    @Override
    public BridgeProfileCatalogEntry reactivate(String profileId, String actor) {
        return changeStatus(profileId, actor, "reactivate");
    }

    private BridgeProfileCatalogEntry changeStatus(String profileId, String actor, String action) {
        String endpoint = profileEndpoint(profileId).pathSegment(action).build().encode().toUriString();
        return postEntry(endpoint, new ActorPayload(requireActor(actor)), action + " Analyzer Bridge profile");
    }

    private BridgeProfileCatalogEntry postEntry(String endpoint, Object payload, String operation) {
        BridgeHttpClient.BridgeResponse response;
        try {
            response = bridgeHttpClient.post(endpoint, objectMapper.writeValueAsString(payload), READ_TIMEOUT);
        } catch (JsonProcessingException e) {
            throw new AnalyzerProfileCatalogException("Unable to serialize request to " + operation, e);
        } catch (IOException e) {
            throw new AnalyzerProfileCatalogException("Unable to " + operation, e);
        }
        ensureSuccess(response, operation);
        return readEntry(response);
    }

    private BridgeHttpClient.BridgeResponse get(String endpoint, String operation) {
        BridgeHttpClient.BridgeResponse response;
        try {
            response = bridgeHttpClient.get(endpoint, READ_TIMEOUT);
        } catch (IOException e) {
            throw new AnalyzerProfileCatalogException("Unable to " + operation, e);
        }
        ensureSuccess(response, operation);
        return response;
    }

    private BridgeProfileCatalogEntry readEntry(BridgeHttpClient.BridgeResponse response) {
        try {
            return objectMapper.readValue(response.body, BridgeProfileCatalogEntry.class);
        } catch (JsonProcessingException e) {
            throw new AnalyzerProfileCatalogException("Analyzer Bridge returned an invalid profile catalog entry", e);
        }
    }

    private List<BridgeProfileCatalogEntry> readEntries(BridgeHttpClient.BridgeResponse response) {
        try {
            List<BridgeProfileCatalogEntry> entries = objectMapper.readValue(response.body,
                    new TypeReference<List<BridgeProfileCatalogEntry>>() {
                    });
            return List.copyOf(entries);
        } catch (JsonProcessingException e) {
            throw new AnalyzerProfileCatalogException("Analyzer Bridge returned an invalid profile catalog", e);
        }
    }

    private static void ensureSuccess(BridgeHttpClient.BridgeResponse response, String operation) {
        if (!response.isSuccess()) {
            throw new AnalyzerProfileCatalogException(
                    "Unable to " + operation + ": Analyzer Bridge returned HTTP " + response.status);
        }
    }

    private UriComponentsBuilder profileEndpoint(String profileId) {
        requireConfigured();
        return UriComponentsBuilder.fromUriString(withoutTrailingSlash(bridgeBaseUrl)).pathSegment("api", "profiles",
                requireProfileId(profileId));
    }

    private void requireConfigured() {
        if (bridgeBaseUrl.isBlank()) {
            throw new AnalyzerProfileCatalogException("Analyzer Bridge URL is not configured");
        }
    }

    private static String requireProfileId(String profileId) {
        return required(profileId, "Analyzer profile ID is required");
    }

    private static String requireActor(String actor) {
        return required(actor, "Analyzer profile actor is required");
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AnalyzerProfileCatalogException(message);
        }
        return value.trim();
    }

    private static void addQueryParameter(UriComponentsBuilder endpoint, String name, String value) {
        if (value != null && !value.isBlank()) {
            endpoint.queryParam(name, value.trim());
        }
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record ActorPayload(String actor) {
    }

    private record ForkPayload(String actor, int sourceRevision, String profileId, String displayName) {
    }
}
