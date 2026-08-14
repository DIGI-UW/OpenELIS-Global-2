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
        return null;
    }

    @Override
    public List<BridgeProfileCatalogEntry> history(String profileId) {
        return List.of();
    }

    @Override
    public BridgeProfileCatalogEntry fork(String profileId, AnalyzerProfileForkRequest request, String actor) {
        return null;
    }

    @Override
    public BridgeProfileCatalogEntry deactivate(String profileId, String actor) {
        return null;
    }

    @Override
    public BridgeProfileCatalogEntry reactivate(String profileId, String actor) {
        return null;
    }

    private static void addQueryParameter(UriComponentsBuilder endpoint, String name, String value) {
        if (value != null && !value.isBlank()) {
            endpoint.queryParam(name, value.trim());
        }
    }

    private static String withoutTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
