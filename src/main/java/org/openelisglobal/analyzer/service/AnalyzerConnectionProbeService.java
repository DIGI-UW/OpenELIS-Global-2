package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/**
 * Synchronizes one analyzer candidate and validates its Bridge probe evidence.
 */
@Service
public class AnalyzerConnectionProbeService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";
    private static final Set<String> CONNECTION_MODES = Set.of("TCP", "MLLP", "SERIAL", "FILE", "HTTP");
    private static final Set<String> CONNECTION_ROLES = Set.of("RECEIVER", "INITIATOR");
    private static final Set<String> DATA_FLOWS = Set.of("RESULTS_ONLY", "TWO_WAY");
    private static final Set<String> OUTCOMES = Set.of("SUCCESS", "FAILURE", "MISSING_CONFIGURATION", "TIMEOUT");
    private static final Set<String> CHECK_KINDS = Set.of("LISTENER", "REMOTE_PROTOCOL", "DIRECTORY", "SERIAL_DEVICE",
            "HTTP_ENDPOINT");
    private static final Set<String> CHECK_STATUSES = Set.of("PASSED", "FAILED", "MISSING_CONFIGURATION", "TIMED_OUT");

    private final BridgeRegistrationService registrationService;
    private final BridgeHttpClient bridgeHttpClient;
    private final String bridgeBaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalyzerConnectionProbeService(BridgeRegistrationService registrationService,
            BridgeHttpClient bridgeHttpClient, @Value("${analyzer.bridge.url:}") String bridgeBaseUrl) {
        this.registrationService = registrationService;
        this.bridgeHttpClient = bridgeHttpClient;
        this.bridgeBaseUrl = stripTrailingSlashes(bridgeBaseUrl);
    }

    public AnalyzerConnectionProbeView probe(String analyzerId) {
        String normalizedId = requireText(analyzerId);
        if (bridgeBaseUrl.isBlank()) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.notConfigured");
        }
        BridgeRegistrationResult registration = registrationService.synchronize();
        BridgeRegisteredCandidate candidate = registration.candidate(normalizedId).orElseThrow(
                () -> new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.notSynchronized",
                        Map.of("detail", registration.failure() == null ? "" : registration.failure())));

        String endpoint = bridgeBaseUrl + "/api/analyzers/"
                + UriUtils.encodePathSegment(normalizedId, StandardCharsets.UTF_8) + "/probe";
        BridgeHttpClient.BridgeResponse response;
        try {
            response = bridgeHttpClient.post(endpoint, null, REQUEST_TIMEOUT);
        } catch (IOException exception) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.unreachable",
                    Map.of("detail", String.valueOf(exception.getMessage())), exception);
        }
        if (!response.isSuccess()) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.httpStatus",
                    Map.of("status", response.status));
        }

        AnalyzerConnectionProbeView evidence;
        try {
            evidence = objectMapper.readValue(response.body, AnalyzerConnectionProbeView.class);
        } catch (IOException exception) {
            throw invalidEvidence(exception);
        }
        validate(evidence);
        if (!normalizedId.equals(evidence.analyzerId())
                || !candidate.profileId().equals(evidence.profileRef().profileId())
                || candidate.profileRevision() != evidence.profileRef().revision()
                || !candidate.desiredStateFingerprint().equals(evidence.desiredStateFingerprint())) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.staleEvidence");
        }
        return evidence;
    }

    private static void validate(AnalyzerConnectionProbeView evidence) {
        if (evidence == null || !"1.0".equals(evidence.schemaVersion()) || isBlank(evidence.analyzerId())
                || evidence.profileRef() == null || isBlank(evidence.profileRef().profileId())
                || evidence.profileRef().revision() < 1 || evidence.desiredStateFingerprint() == null
                || !evidence.desiredStateFingerprint().matches(FINGERPRINT_PATTERN) || evidence.connection() == null
                || !CONNECTION_MODES.contains(evidence.connection().mode())
                || !CONNECTION_ROLES.contains(evidence.connection().role()) || !DATA_FLOWS.contains(evidence.dataFlow())
                || !OUTCOMES.contains(evidence.outcome()) || evidence.checks().isEmpty()
                || evidence.checks().stream().anyMatch(AnalyzerConnectionProbeService::invalidCheck)
                || invalidEndpoint(evidence.configureEndpoint())) {
            throw invalidEvidence(null);
        }
    }

    private static boolean invalidCheck(AnalyzerConnectionProbeView.Check check) {
        return check == null || !CHECK_KINDS.contains(check.kind()) || !CHECK_STATUSES.contains(check.status())
                || isBlank(check.code()) || check.responseTimeMs() < 0;
    }

    private static boolean invalidEndpoint(AnalyzerConnectionProbeView.ConfigureEndpoint endpoint) {
        if (endpoint == null) {
            return false;
        }
        return switch (endpoint.kind()) {
        case "NETWORK" -> isBlank(endpoint.host()) || endpoint.port() == null || endpoint.port() < 1
                || endpoint.port() > 65535 || endpoint.path() != null || endpoint.url() != null;
        case "DIRECTORY", "DEVICE" ->
            isBlank(endpoint.path()) || endpoint.host() != null || endpoint.port() != null || endpoint.url() != null;
        case "HTTP" ->
            isBlank(endpoint.url()) || endpoint.host() != null || endpoint.port() != null || endpoint.path() != null;
        default -> true;
        };
    }

    private static AnalyzerConnectionProbeException invalidEvidence(Throwable cause) {
        return new AnalyzerConnectionProbeException("analyzer.testConnection.bridge.invalidEvidence", Map.of(), cause);
    }

    private static String requireText(String value) {
        if (isBlank(value)) {
            throw new AnalyzerConnectionProbeException("analyzer.testConnection.analyzerIdMissing");
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String stripTrailingSlashes(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
