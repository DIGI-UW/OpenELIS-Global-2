package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BridgeProfileCatalogServiceImpl implements BridgeProfileCatalogService {

    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final BridgeHttpClient bridgeHttpClient;
    private final String bridgeUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BridgeProfileCatalogServiceImpl(BridgeHttpClient bridgeHttpClient,
            @Value("${analyzer.bridge.url:}") String bridgeUrl) {
        this.bridgeHttpClient = bridgeHttpClient;
        this.bridgeUrl = stripTrailingSlashes(bridgeUrl);
    }

    @Override
    public BridgeProfileCatalog getCatalog() {
        if (bridgeUrl.isBlank()) {
            throw new BridgeProfileCatalogException("Bridge URL is not configured");
        }

        BridgeHttpClient.BridgeResponse response;
        try {
            response = bridgeHttpClient.get(bridgeUrl + "/api/profiles", REQUEST_TIMEOUT);
        } catch (IOException e) {
            throw new BridgeProfileCatalogException("Bridge profile catalog request failed", e);
        }

        if (!response.isSuccess()) {
            throw new BridgeProfileCatalogException(
                    "Bridge profile catalog request failed with HTTP " + response.status);
        }

        BridgeProfileCatalog catalog;
        try {
            catalog = objectMapper.readValue(response.body, BridgeProfileCatalog.class);
        } catch (IOException e) {
            throw new BridgeProfileCatalogException("Bridge profile catalog response is invalid JSON", e);
        }

        validateContract(catalog);
        return catalog;
    }

    private static void validateContract(BridgeProfileCatalog catalog) {
        if (!SUPPORTED_SCHEMA_VERSION.equals(catalog.schemaVersion())) {
            throw new BridgeProfileCatalogException(
                    "Unsupported Bridge profile catalog schema version: " + catalog.schemaVersion());
        }
        if (catalog.catalogFingerprint() == null || !catalog.catalogFingerprint().matches(FINGERPRINT_PATTERN)) {
            throw new BridgeProfileCatalogException("Bridge profile catalog fingerprint is invalid");
        }
        for (BridgeProfileCatalog.ProfileRevision revision : catalog.profiles()) {
            if (revision == null || revision.profile() == null || revision.publication() == null) {
                throw new BridgeProfileCatalogException("Bridge profile catalog contains an invalid revision");
            }
        }
    }

    private static String stripTrailingSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
