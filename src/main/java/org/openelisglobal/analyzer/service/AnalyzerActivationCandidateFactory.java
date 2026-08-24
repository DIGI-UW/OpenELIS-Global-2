package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingConfirmation;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingMappingState;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingResult;
import org.openelisglobal.analyzer.valueholder.AnalyzerSiteBindingTest;
import org.openelisglobal.analyzer.valueholder.AnalyzerTransportMode;
import org.springframework.stereotype.Component;

@Component
public class AnalyzerActivationCandidateFactory {

    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final TypeReference<List<AnalyzerSiteBindingSourceRow>> ROW_LIST = new TypeReference<>() {
    };
    private static final Comparator<AnalyzerSiteBindingSourceRow> ROW_ORDER = Comparator
            .comparing(AnalyzerSiteBindingSourceRow::sourceRowKey)
            .thenComparing(AnalyzerSiteBindingSourceRow::rawValue, Comparator.nullsFirst(String::compareTo));
    private static final String EXCLUDED_REASON = "Not offered by this laboratory";

    private final Clock clock;
    private final ObjectMapper objectMapper;

    public AnalyzerActivationCandidateFactory() {
        this(Clock.systemUTC());
    }

    AnalyzerActivationCandidateFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
        this.objectMapper = new ObjectMapper();
    }

    public AnalyzerActivationDocuments create(Analyzer analyzer, AnalyzerSiteBindingSnapshot siteBinding,
            AnalyzerSiteBindingConfirmation confirmation, ObjectNode registration,
            BridgeRegisteredCandidate bridgeAcknowledgement) {
        AnalyzerProfileBinding profile = requireCandidateContext(analyzer, siteBinding, confirmation);
        ObjectNode exactRegistration = requireRegistration(analyzer, profile, registration);
        String desiredFingerprint = requireFingerprint(exactRegistration.path("desiredStateFingerprint").asText(null),
                "desired registration fingerprint");
        requireAcknowledgement(analyzer.getId(), profile, desiredFingerprint, bridgeAcknowledgement);
        RowDisposition rows = requireExactRows(siteBinding, confirmation);

        ObjectNode candidate = objectMapper.createObjectNode();
        candidate.put("schemaVersion", "1.0");
        candidate.put("oeAnalyzerId", requireText(analyzer.getId(), "analyzer ID"));
        candidate.set("profileRef", candidateProfileRef(profile));
        candidate.set("instance", instance(analyzer, exactRegistration));
        candidate.set("siteBinding", siteBinding(siteBinding, rows));
        candidate.set("verification", verification(confirmation));
        candidate.put("desiredRegistrationFingerprint", desiredFingerprint);
        candidate.set("bridgeAcknowledgement",
                bridgeAcknowledgement(profile, desiredFingerprint, clock.instant().toString()));
        return new AnalyzerActivationDocuments(candidate, exactRegistration);
    }

    private AnalyzerProfileBinding requireCandidateContext(Analyzer analyzer, AnalyzerSiteBindingSnapshot siteBinding,
            AnalyzerSiteBindingConfirmation confirmation) {
        if (analyzer == null || siteBinding == null || siteBinding.binding() == null || siteBinding.revision() == null
                || confirmation == null) {
            throw new IllegalArgumentException("Complete analyzer verification candidate is required");
        }
        requireText(analyzer.getId(), "analyzer ID");
        requireText(analyzer.getName(), "analyzer name");
        if (analyzer.getSiteBindingRevision() == null
                || !sameId(analyzer.getSiteBindingRevision().getId(), siteBinding.revision().getId())) {
            throw new IllegalArgumentException("Analyzer must reference the verified site-binding revision");
        }
        if (siteBinding.revision().getSiteBinding() == null
                || !sameId(siteBinding.revision().getSiteBinding().getId(), siteBinding.binding().getId())) {
            throw new IllegalArgumentException("Site-binding revision does not belong to the supplied binding");
        }
        AnalyzerProfileBinding profile = siteBinding.binding().getProfileBinding();
        if (profile == null || confirmation.getSiteBindingRevision() == null
                || !sameId(confirmation.getSiteBindingRevision().getId(), siteBinding.revision().getId())) {
            throw new IllegalArgumentException("Verification does not belong to the analyzer site binding");
        }
        String profileId = requireText(profile.getProfileId(), "profile ID");
        if (profile.getProfileRevision() < 1
                || !profileId.equals(requireText(confirmation.getProfileId(), "verified profile ID"))
                || profile.getProfileRevision() != confirmation.getProfileRevision()) {
            throw new IllegalArgumentException("Verification does not match the pinned profile revision");
        }
        String profileFingerprint = requireFingerprint(profile.getProfileFingerprint(), "profile fingerprint");
        if (!profileFingerprint.equals(
                requireFingerprint(confirmation.getProfileRevisionFingerprint(), "verified profile fingerprint"))) {
            throw new IllegalArgumentException("Verification does not match the pinned profile fingerprint");
        }
        String bindingFingerprint = requireFingerprint(siteBinding.revision().getBindingFingerprint(),
                "site-binding fingerprint");
        if (!bindingFingerprint.equals(
                requireFingerprint(confirmation.getBindingFingerprint(), "verified site-binding fingerprint"))) {
            throw new IllegalArgumentException("Verification does not match the site-binding fingerprint");
        }
        requireFingerprint(confirmation.getRecognitionFingerprint(), "recognition fingerprint");
        requireText(confirmation.getConfirmedBy(), "verifier");
        requireText(confirmation.getAuditEventId(), "verification audit event ID");
        if (confirmation.getConfirmedAt() == null) {
            throw new IllegalArgumentException("Verification time is required");
        }
        return profile;
    }

    private ObjectNode requireRegistration(Analyzer analyzer, AnalyzerProfileBinding profile, ObjectNode registration) {
        if (registration == null) {
            throw new IllegalArgumentException("Desired Bridge registration is required");
        }
        ObjectNode exact = registration.deepCopy();
        String claimedFingerprint = requireFingerprint(exact.path("desiredStateFingerprint").asText(null),
                "desired registration fingerprint");
        ObjectNode fingerprintInput = exact.deepCopy();
        fingerprintInput.remove("desiredStateFingerprint");
        if (!claimedFingerprint.equals(fingerprint(fingerprintInput))) {
            throw new IllegalArgumentException("Desired Bridge registration fingerprint does not match its content");
        }
        if (!requireText(analyzer.getName(), "analyzer name")
                .equals(requireText(exact.path("name").asText(null), "registered analyzer name"))) {
            throw new IllegalArgumentException("Desired Bridge registration does not match the analyzer name");
        }
        JsonNode profileRef = exact.path("profileRef");
        if (!requireText(profile.getProfileId(), "profile ID")
                .equals(requireText(profileRef.path("profileId").asText(null), "registered profile ID"))
                || profile.getProfileRevision() != profileRef.path("revision").asInt(0)) {
            throw new IllegalArgumentException("Desired Bridge registration does not match the pinned profile");
        }
        if (!"ACTIVE".equals(exact.path("desiredStatus").asText())) {
            throw new IllegalArgumentException("Activation candidate requires an active Bridge registration");
        }
        JsonNode connection = exact.path("connection");
        String mode = requireText(connection.path("mode").asText(null), "registered connection mode");
        if (analyzer.getTransportMode() == null || !analyzer.getTransportMode().name().equals(mode)) {
            throw new IllegalArgumentException("Desired Bridge registration does not match the analyzer transport");
        }
        if (!connection.path("settings").isObject()) {
            throw new IllegalArgumentException("Desired Bridge registration settings are required");
        }
        return exact;
    }

    private static void requireAcknowledgement(String analyzerId, AnalyzerProfileBinding profile,
            String desiredFingerprint, BridgeRegisteredCandidate acknowledgement) {
        if (acknowledgement == null
                || !requireText(analyzerId, "analyzer ID")
                        .equals(requireText(acknowledgement.analyzerId(), "acknowledged analyzer ID"))
                || !requireText(profile.getProfileId(), "profile ID")
                        .equals(requireText(acknowledgement.profileId(), "acknowledged profile ID"))
                || profile.getProfileRevision() != acknowledgement.profileRevision()
                || !desiredFingerprint.equals(requireFingerprint(acknowledgement.desiredStateFingerprint(),
                        "acknowledged desired-state fingerprint"))) {
            throw new IllegalArgumentException("Bridge acknowledgement does not match the activation candidate");
        }
    }

    private RowDisposition requireExactRows(AnalyzerSiteBindingSnapshot siteBinding,
            AnalyzerSiteBindingConfirmation confirmation) {
        Map<String, AnalyzerSiteBindingTest> tests = new LinkedHashMap<>();
        List<AnalyzerSiteBindingSourceRow> confirmed = new ArrayList<>();
        List<AnalyzerSiteBindingSourceRow> excluded = new ArrayList<>();
        for (AnalyzerSiteBindingTest test : siteBinding.tests()) {
            if (test == null || test.getId() == null || test.getSiteBindingRevision() == null
                    || !sameId(test.getId().getSiteBindingRevisionId(), siteBinding.revision().getId())
                    || !sameId(test.getSiteBindingRevision().getId(), siteBinding.revision().getId())) {
                throw new IllegalArgumentException("Test mapping does not belong to the candidate revision");
            }
            String sourceRowKey = requireText(test.getId().getSourceRowKey(), "test source row key");
            if (tests.put(sourceRowKey, test) != null) {
                throw new IllegalArgumentException("Duplicate test source row " + sourceRowKey);
            }
            addDisposition(test.getMappingState(), new AnalyzerSiteBindingSourceRow(sourceRowKey, null), confirmed,
                    excluded);
        }
        if (tests.isEmpty()) {
            throw new IllegalArgumentException("At least one analyzer test mapping is required");
        }

        Set<AnalyzerSiteBindingSourceRow> resultRows = new HashSet<>();
        for (AnalyzerSiteBindingResult result : siteBinding.results()) {
            if (result == null || result.getId() == null || result.getSiteBindingRevision() == null
                    || !sameId(result.getId().getSiteBindingRevisionId(), siteBinding.revision().getId())
                    || !sameId(result.getSiteBindingRevision().getId(), siteBinding.revision().getId())) {
                throw new IllegalArgumentException("Result mapping does not belong to the candidate revision");
            }
            String sourceRowKey = requireText(result.getId().getSourceRowKey(), "result source row key");
            String rawValue = requireText(result.getId().getRawValue(), "raw result value");
            if (!tests.containsKey(sourceRowKey)) {
                throw new IllegalArgumentException("Result mapping has no matching test row " + sourceRowKey);
            }
            AnalyzerSiteBindingSourceRow sourceRow = new AnalyzerSiteBindingSourceRow(sourceRowKey, rawValue);
            if (!resultRows.add(sourceRow)) {
                throw new IllegalArgumentException("Duplicate result source row " + sourceRowKey + ":" + rawValue);
            }
            addDisposition(result.getMappingState(), sourceRow, confirmed, excluded);
        }
        confirmed.sort(ROW_ORDER);
        excluded.sort(ROW_ORDER);
        if (!confirmed.equals(readRows(confirmation.getConfirmedRowsJson(), "confirmed"))
                || !excluded.equals(readRows(confirmation.getExcludedRowsJson(), "excluded"))) {
            throw new IllegalArgumentException("Verification rows do not match the activation candidate");
        }
        return new RowDisposition(tests, siteBinding.results());
    }

    private List<AnalyzerSiteBindingSourceRow> readRows(String json, String label) {
        try {
            List<AnalyzerSiteBindingSourceRow> rows = objectMapper.readValue(requireText(json, label + " rows"),
                    ROW_LIST);
            if (rows == null) {
                throw new IllegalArgumentException("Stored " + label + " verification rows are invalid");
            }
            List<AnalyzerSiteBindingSourceRow> normalized = rows.stream().sorted(ROW_ORDER).toList();
            if (new LinkedHashSet<>(normalized).size() != normalized.size()) {
                throw new IllegalArgumentException("Duplicate " + label + " verification row");
            }
            return normalized;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Stored " + label + " verification rows are invalid", exception);
        }
    }

    private static void addDisposition(AnalyzerSiteBindingMappingState state, AnalyzerSiteBindingSourceRow row,
            List<AnalyzerSiteBindingSourceRow> confirmed, List<AnalyzerSiteBindingSourceRow> excluded) {
        if (state == AnalyzerSiteBindingMappingState.BOUND) {
            confirmed.add(row);
        } else if (state == AnalyzerSiteBindingMappingState.EXCLUDED) {
            excluded.add(row);
        } else {
            throw new IllegalArgumentException("Every source row must be bound or excluded before activation");
        }
    }

    private ObjectNode candidateProfileRef(AnalyzerProfileBinding profile) {
        ObjectNode profileRef = objectMapper.createObjectNode();
        profileRef.put("id", profile.getProfileId());
        profileRef.put("revision", profile.getProfileRevision());
        return profileRef;
    }

    private ObjectNode instance(Analyzer analyzer, ObjectNode registration) {
        ObjectNode instance = objectMapper.createObjectNode();
        instance.put("name", requireText(analyzer.getName(), "analyzer name"));
        List<String> labUnitIds = analyzer.getTestUnitIds() == null ? List.of()
                : analyzer.getTestUnitIds().stream().map(id -> requireText(id, "lab unit ID")).distinct().sorted()
                        .toList();
        if (labUnitIds.isEmpty()) {
            throw new IllegalArgumentException("At least one lab unit is required");
        }
        var labUnits = instance.putArray("labUnitIds");
        labUnitIds.forEach(labUnits::add);
        instance.set("connection", candidateConnection(registration.path("connection")));
        return instance;
    }

    private ObjectNode candidateConnection(JsonNode registrationConnection) {
        ObjectNode connection = objectMapper.createObjectNode();
        AnalyzerTransportMode mode;
        try {
            mode = AnalyzerTransportMode
                    .valueOf(requireText(registrationConnection.path("mode").asText(null), "connection mode"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Connection mode is not supported", exception);
        }
        connection.put("mode", mode.name());
        ObjectNode settings = connection.putObject("settings");
        JsonNode source = registrationConnection.path("settings");
        copyText(source, "remoteHost", settings, "host");
        copyInteger(source, "remotePort", settings, "port");
        copyText(source, "device", settings, "serialDevice");
        copyInteger(source, "baudRate", settings, "baudRate");
        copyText(source, "directory", settings, "directory");
        copyText(source, "credentialsRef", settings, "credentialsRef");
        return connection;
    }

    private ObjectNode siteBinding(AnalyzerSiteBindingSnapshot siteBinding, RowDisposition rows) {
        ObjectNode binding = objectMapper.createObjectNode();
        binding.put("fingerprint", siteBinding.revision().getBindingFingerprint());
        ObjectNode tests = binding.putObject("tests");
        rows.tests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            AnalyzerSiteBindingTest source = entry.getValue();
            ObjectNode test = tests.putObject(entry.getKey());
            if (source.getMappingState() == AnalyzerSiteBindingMappingState.EXCLUDED) {
                test.put("status", "EXCLUDED");
                test.put("reason", EXCLUDED_REASON);
                return;
            }
            test.put("status", "BOUND");
            test.put("openelisTestId", requireText(source.getTestId(), "OpenELIS test ID"));
            ObjectNode resultValues = test.putObject("resultValues");
            rows.results.stream()
                    .filter(result -> entry.getKey().equals(result.getId().getSourceRowKey())
                            && result.getMappingState() == AnalyzerSiteBindingMappingState.BOUND)
                    .sorted(Comparator.comparing(result -> result.getId().getRawValue())).forEach(result -> {
                        ObjectNode resultValue = resultValues.putObject(result.getId().getRawValue());
                        resultValue.put("openelisResultOptionId",
                                requireText(result.getTestResultId(), "OpenELIS result option ID"));
                    });
        });
        return binding;
    }

    private ObjectNode verification(AnalyzerSiteBindingConfirmation confirmation) {
        ObjectNode verification = objectMapper.createObjectNode();
        verification.put("verifiedBy", confirmation.getConfirmedBy());
        verification.put("verifiedAt", confirmation.getConfirmedAt().toInstant().toString());
        verification.put("auditEventId", confirmation.getAuditEventId());
        verification.put("profileRevisionFingerprint", confirmation.getProfileRevisionFingerprint());
        verification.put("siteBindingFingerprint", confirmation.getBindingFingerprint());
        verification.put("recognitionFingerprint", confirmation.getRecognitionFingerprint());
        return verification;
    }

    private ObjectNode bridgeAcknowledgement(AnalyzerProfileBinding profile, String fingerprint,
            String acknowledgedAt) {
        ObjectNode acknowledgement = objectMapper.createObjectNode();
        acknowledgement.put("status", "APPLIED");
        acknowledgement.set("profileRef", candidateProfileRef(profile));
        acknowledgement.put("desiredStateFingerprint", fingerprint);
        acknowledgement.put("acknowledgedAt", acknowledgedAt);
        return acknowledgement;
    }

    private static void copyText(JsonNode source, String sourceName, ObjectNode target, String targetName) {
        String value = source.path(sourceName).asText(null);
        if (value != null && !value.trim().isEmpty()) {
            target.put(targetName, value.trim());
        }
    }

    private static void copyInteger(JsonNode source, String sourceName, ObjectNode target, String targetName) {
        if (source.path(sourceName).canConvertToInt()) {
            target.put(targetName, source.path(sourceName).asInt());
        }
    }

    private String fingerprint(JsonNode value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8);
            return "sha256:" + HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not fingerprint analyzer registration", exception);
        }
    }

    private static boolean sameId(String left, String right) {
        return left != null && left.equals(right);
    }

    private static String requireFingerprint(String value, String label) {
        String fingerprint = requireText(value, label);
        if (!FINGERPRINT_PATTERN.matcher(fingerprint).matches()) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return fingerprint;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private record RowDisposition(Map<String, AnalyzerSiteBindingTest> tests, List<AnalyzerSiteBindingResult> results) {
    }
}
