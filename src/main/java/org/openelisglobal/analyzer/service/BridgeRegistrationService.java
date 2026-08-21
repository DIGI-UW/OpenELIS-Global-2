package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerConnectionRole;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Publishes OpenELIS-owned analyzer instance state to the Analyzer Bridge. */
@Service
public class BridgeRegistrationService {

    private static final String CLASS_NAME = "BridgeRegistrationService";
    private static final Duration SYNC_TIMEOUT = Duration.ofSeconds(30);

    private final AnalyzerService analyzerService;
    private final BridgeHttpClient bridgeHttpClient;
    private final String bridgeBaseUrl;
    private final Clock clock;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public BridgeRegistrationService(AnalyzerService analyzerService, BridgeHttpClient bridgeHttpClient,
            @Value("${analyzer.bridge.url:}") String bridgeBaseUrl) {
        this(analyzerService, bridgeHttpClient, bridgeBaseUrl, Clock.systemUTC());
    }

    BridgeRegistrationService(AnalyzerService analyzerService, BridgeHttpClient bridgeHttpClient, String bridgeBaseUrl,
            Clock clock) {
        this.analyzerService = analyzerService;
        this.bridgeHttpClient = bridgeHttpClient;
        this.bridgeBaseUrl = bridgeBaseUrl == null ? "" : bridgeBaseUrl.replaceAll("/+$", "");
        this.clock = clock;
    }

    /**
     * Builds the complete desired Bridge state from immutable profile pins and
     * site-owned instance values. Profile runtime/default documents never cross
     * this boundary.
     */
    ObjectNode buildDesiredState() {
        ObjectNode analyzersNode = objectMapper.createObjectNode();
        List<Analyzer> analyzers = analyzerService.getAllWithTypes();
        if (analyzers != null) {
            analyzers.stream().filter(analyzer -> analyzer.getStatus() != Analyzer.AnalyzerStatus.DELETED)
                    .sorted(Comparator.comparing(Analyzer::getId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .forEach(analyzer -> {
                        ObjectNode registration = buildRegistration(analyzer);
                        if (registration != null) {
                            analyzersNode.set(analyzer.getId(), registration);
                        }
                    });
        }

        ObjectNode desiredState = objectMapper.createObjectNode();
        desiredState.put("schemaVersion", "1.0");
        desiredState.put("desiredStateRevision", fingerprint(analyzersNode));
        desiredState.put("generatedAt", clock.instant().toString());
        desiredState.set("analyzers", analyzersNode);
        return desiredState;
    }

    /** Sends one idempotent, versioned full-state reconciliation request. */
    public BridgeRegistrationResult synchronize() {
        ObjectNode desiredState = buildDesiredState();
        if (bridgeBaseUrl.isBlank()) {
            return incomplete("Analyzer Bridge URL is not configured");
        }

        try {
            BridgeHttpClient.BridgeResponse response = bridgeHttpClient.put(bridgeBaseUrl + "/api/analyzers/sync",
                    objectMapper.writeValueAsString(desiredState), SYNC_TIMEOUT);
            if (!response.isSuccess()) {
                return incomplete("Analyzer Bridge sync returned HTTP " + response.status);
            }
            return validateAcknowledgement(desiredState, objectMapper.readTree(response.body));
        } catch (Exception exception) {
            LogEvent.logWarn(CLASS_NAME, "synchronize", "Analyzer Bridge sync failed: " + exception.getMessage());
            return incomplete("Analyzer Bridge sync failed: " + exception.getMessage());
        }
    }

    private ObjectNode buildRegistration(Analyzer analyzer) {
        boolean activationRequired = analyzer.getStatus() == Analyzer.AnalyzerStatus.ACTIVE;
        boolean runtimeEnabled = isBridgeRuntimeEnabled(analyzer.getStatus());
        String analyzerId = text(analyzer.getId());
        AnalyzerProfileBinding profile = analyzer.getPinnedProfileBinding();
        String name = text(analyzer.getName());
        String protocol = normalizedProtocol(analyzer.getType());

        if (analyzerId == null || profile == null || text(profile.getProfileId()) == null
                || profile.getProfileRevision() < 1 || name == null || protocol == null) {
            return incompleteAnalyzer(activationRequired, analyzerId,
                    profile == null ? "profile pin" : "identity, name, or protocol");
        }

        ObjectNode connection = buildConnection(analyzer, protocol);
        if (connection == null) {
            return incompleteAnalyzer(activationRequired, analyzerId, "connection settings");
        }

        String sourceId = "FILE".equals(protocol) ? text(analyzer.getImportDirectory()) : text(analyzer.getIpAddress());
        ObjectNode registration = objectMapper.createObjectNode();
        registration.put("sourceId", sourceId);
        registration.put("name", name);
        ObjectNode profileRef = registration.putObject("profileRef");
        profileRef.put("profileId", profile.getProfileId());
        profileRef.put("revision", profile.getProfileRevision());
        registration.put("protocol", protocol);
        registration.put("desiredStatus", runtimeEnabled ? "ACTIVE" : "INACTIVE");
        registration.set("connection", connection);
        registration.put("desiredStateFingerprint", fingerprint(registration));
        return registration;
    }

    private static boolean isBridgeRuntimeEnabled(Analyzer.AnalyzerStatus status) {
        return status == Analyzer.AnalyzerStatus.SETUP || status == Analyzer.AnalyzerStatus.VALIDATION
                || status == Analyzer.AnalyzerStatus.ACTIVE || status == Analyzer.AnalyzerStatus.ERROR_PENDING
                || status == Analyzer.AnalyzerStatus.OFFLINE;
    }

    private ObjectNode buildConnection(Analyzer analyzer, String protocol) {
        ObjectNode connection = objectMapper.createObjectNode();
        ObjectNode settings;
        AnalyzerTransportMode transport = analyzer.getTransportMode();
        AnalyzerConnectionRole role = analyzer.getConnectionRole();
        if (transport == null || role == null || !isCompatible(protocol, transport)) {
            return null;
        }
        if (transport == AnalyzerTransportMode.FILE) {
            String directory = text(analyzer.getImportDirectory());
            if (directory == null) {
                return null;
            }
            connection.put("mode", transport.name());
            connection.put("role", role.name());
            settings = connection.putObject("settings");
            settings.put("directory", directory);
            return connection;
        }

        if (transport != AnalyzerTransportMode.TCP && transport != AnalyzerTransportMode.MLLP) {
            return null;
        }
        String host = text(analyzer.getIpAddress());
        if (host == null) {
            return null;
        }
        connection.put("mode", transport.name());
        CommunicationMode communication = analyzer.getCommunicationMode();
        connection.put("role", role.name());
        settings = connection.putObject("settings");
        if (role == AnalyzerConnectionRole.INITIATOR || communication == CommunicationMode.BOTH) {
            if (analyzer.getPort() == null) {
                return null;
            }
            settings.put("remoteHost", host);
            settings.put("remotePort", analyzer.getPort());
        }
        return connection;
    }

    private static boolean isCompatible(String protocol, AnalyzerTransportMode transport) {
        return switch (protocol) {
        case "ASTM" -> transport == AnalyzerTransportMode.TCP || transport == AnalyzerTransportMode.SERIAL;
        case "HL7" -> transport == AnalyzerTransportMode.TCP || transport == AnalyzerTransportMode.MLLP
                || transport == AnalyzerTransportMode.HTTP;
        case "FILE" -> transport == AnalyzerTransportMode.FILE;
        default -> false;
        };
    }

    private BridgeRegistrationResult validateAcknowledgement(ObjectNode request, JsonNode response) {
        String expectedRevision = request.path("desiredStateRevision").asText();
        if (!"1.0".equals(response.path("schemaVersion").asText())
                || !expectedRevision.equals(response.path("appliedStateRevision").asText())) {
            return incomplete("Analyzer Bridge did not acknowledge desired state " + expectedRevision);
        }
        if (!response.path("errors").isArray() || !response.path("errors").isEmpty()
                || response.path("counts").path("rejected").asInt(-1) != 0) {
            return incomplete("Analyzer Bridge rejected one or more registrations");
        }

        Set<String> acknowledged = new LinkedHashSet<>();
        List<String> failures = new ArrayList<>();
        request.path("analyzers").fields().forEachRemaining(entry -> {
            JsonNode expected = entry.getValue();
            JsonNode actual = response.path("registrations").path(entry.getKey());
            String status = actual.path("status").asText();
            boolean exact = ("APPLIED".equals(status) || "UNCHANGED".equals(status))
                    && expected.path("profileRef").equals(actual.path("profileRef"))
                    && expected.path("desiredStateFingerprint").asText()
                            .equals(actual.path("desiredStateFingerprint").asText());
            if (exact) {
                acknowledged.add(entry.getKey());
            } else {
                failures.add(entry.getKey());
            }
        });
        if (!failures.isEmpty() || acknowledged.size() != request.path("analyzers").size()) {
            return incomplete("Analyzer Bridge acknowledgement mismatch for " + String.join(", ", failures));
        }
        return new BridgeRegistrationResult(true, acknowledged, null);
    }

    private ObjectNode incompleteAnalyzer(boolean active, String analyzerId, String missing) {
        if (active) {
            throw new BridgeRegistrationException(
                    "Active analyzer " + (analyzerId == null ? "<unknown>" : analyzerId) + " requires " + missing);
        }
        return null;
    }

    private static String normalizedProtocol(String value) {
        String protocol = text(value);
        if (protocol == null) {
            return null;
        }
        protocol = protocol.toUpperCase(Locale.ROOT);
        return Set.of("ASTM", "HL7", "FILE").contains(protocol) ? protocol : null;
    }

    private static String text(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String fingerprint(JsonNode value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
            return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new BridgeRegistrationException("Could not fingerprint analyzer registration", exception);
        }
    }

    private static BridgeRegistrationResult incomplete(String message) {
        return new BridgeRegistrationResult(false, Set.of(), message);
    }
}
