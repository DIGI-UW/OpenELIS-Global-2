package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

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
        BridgeHttpClient.BridgeResponse response = get(bridgeUrl + "/api/profiles");

        BridgeProfileCatalog catalog;
        try {
            catalog = objectMapper.readValue(response.body, BridgeProfileCatalog.class);
        } catch (IOException e) {
            throw new BridgeProfileCatalogException("Bridge profile catalog response is invalid JSON", e);
        }

        validateContract(catalog);
        return catalog;
    }

    @Override
    public BridgeProfileCatalog.ProfileRevision getProfile(String profileId, int revision) {
        String normalizedProfileId = requireText(profileId, "Profile ID");
        if (revision < 1) {
            throw new BridgeProfileCatalogException("Profile revision must be at least 1");
        }
        String url = bridgeUrl + "/api/profiles/"
                + UriUtils.encodePathSegment(normalizedProfileId, StandardCharsets.UTF_8) + "?revision=" + revision;
        BridgeHttpClient.BridgeResponse response = get(url);

        BridgeProfileCatalog.ProfileRevision profileRevision;
        try {
            profileRevision = objectMapper.readValue(response.body, BridgeProfileCatalog.ProfileRevision.class);
        } catch (IOException e) {
            throw new BridgeProfileCatalogException("Bridge profile revision response is invalid JSON", e);
        }
        validateRevision(profileRevision);
        BridgeAnalyzerProfile profile = BridgeAnalyzerProfile.from(profileRevision.profile());
        if (!normalizedProfileId.equals(profile.profileId()) || revision != profile.revision()) {
            throw new BridgeProfileCatalogException("Bridge returned a different profile revision than requested");
        }
        return profileRevision;
    }

    private BridgeHttpClient.BridgeResponse get(String url) {
        if (bridgeUrl.isBlank()) {
            throw new BridgeProfileCatalogException("Bridge URL is not configured");
        }
        try {
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.get(url, REQUEST_TIMEOUT);
            if (!response.isSuccess()) {
                throw new BridgeProfileCatalogException(
                        "Bridge profile catalog request failed with HTTP " + response.status);
            }
            return response;
        } catch (IOException e) {
            throw new BridgeProfileCatalogException("Bridge profile catalog request failed", e);
        }
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
            validateRevision(revision);
        }
    }

    private static void validateRevision(BridgeProfileCatalog.ProfileRevision revision) {
        if (revision == null || revision.profile() == null || revision.publication() == null
                || !revision.publication().isObject()) {
            throw new BridgeProfileCatalogException("Bridge profile catalog contains an invalid revision");
        }
        validateControlRecognitionSummary(revision.controlRecognitionSummary());
        try {
            BridgeAnalyzerProfile.from(revision.profile());
        } catch (IllegalArgumentException e) {
            throw new BridgeProfileCatalogException("Bridge profile catalog contains an invalid profile", e);
        }
    }

    private static void validateControlRecognitionSummary(BridgeProfileCatalog.ControlRecognitionSummary summary) {
        if (summary == null || summary.recognitionFingerprint() == null
                || !summary.recognitionFingerprint().matches(FINGERPRINT_PATTERN) || isBlank(summary.description())) {
            throw invalidControlRecognitionSummary();
        }
        if ("NONE".equals(summary.mode())) {
            if (!summary.affirmedNoControlResults() || !summary.conditions().isEmpty()) {
                throw invalidControlRecognitionSummary();
            }
            return;
        }
        if (!"RULES".equals(summary.mode()) || summary.affirmedNoControlResults() || summary.conditions().isEmpty()
                || summary.conditions().stream().anyMatch(condition -> condition == null || isBlank(condition.key())
                        || isBlank(condition.description()))) {
            throw invalidControlRecognitionSummary();
        }
    }

    private static BridgeProfileCatalogException invalidControlRecognitionSummary() {
        return new BridgeProfileCatalogException(
                "Bridge profile catalog contains an invalid control recognition summary");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BridgeProfileCatalogException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String stripTrailingSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
