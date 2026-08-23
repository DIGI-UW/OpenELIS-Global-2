package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Typed view of the established Bridge-owned analyzer profile contract. */
public final class BridgeAnalyzerProfile {

    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";

    private final JsonNode document;
    private final String profileId;
    private final int revision;
    private final String revisionFingerprint;
    private final String displayName;
    private final String manufacturer;
    private final String model;
    private final String source;
    private final String status;
    private final String protocol;
    private final String protocolVersion;
    private final String communicationMode;
    private final Boolean supportsLisInitiated;
    private final String parentProfileId;
    private final Integer parentRevision;
    private final List<TestDefinition> testDefinitions;
    private final InstanceDefaults instanceDefaults;

    private BridgeAnalyzerProfile(JsonNode document, String profileId, int revision, String revisionFingerprint,
            String displayName, String manufacturer, String model, String source, String status, String protocol,
            String protocolVersion, String communicationMode, Boolean supportsLisInitiated, String parentProfileId,
            Integer parentRevision, List<TestDefinition> testDefinitions, InstanceDefaults instanceDefaults) {
        this.document = document.deepCopy();
        this.profileId = profileId;
        this.revision = revision;
        this.revisionFingerprint = revisionFingerprint;
        this.displayName = displayName;
        this.manufacturer = manufacturer;
        this.model = model;
        this.source = source;
        this.status = status;
        this.protocol = protocol;
        this.protocolVersion = protocolVersion;
        this.communicationMode = communicationMode;
        this.supportsLisInitiated = supportsLisInitiated;
        this.parentProfileId = parentProfileId;
        this.parentRevision = parentRevision;
        this.testDefinitions = List.copyOf(testDefinitions);
        this.instanceDefaults = instanceDefaults;
    }

    public static BridgeAnalyzerProfile from(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("Bridge analyzer profile must be an object");
        }
        JsonNode profileMeta = document.path("profileMeta");
        JsonNode catalog = document.path("catalog");
        JsonNode protocol = document.path("protocol");
        JsonNode configDefaults = document.path("configDefaults");
        String profileId = requiredText(profileMeta, "id");
        String protocolName = requiredText(protocol, "name");
        int revision = catalog.path("revision").asInt(-1);
        if (revision < 1) {
            throw new IllegalArgumentException("Bridge analyzer profile revision must be at least 1");
        }
        String fingerprint = requiredText(catalog, "revisionFingerprint");
        if (!fingerprint.matches(FINGERPRINT_PATTERN)) {
            throw new IllegalArgumentException("Bridge analyzer profile revision fingerprint is invalid");
        }

        List<TestDefinition> tests = new ArrayList<>();
        for (JsonNode mapping : document.path("default_test_mappings")) {
            String analyzerCode = requiredText(mapping, "test_code");
            List<String> aliases = textList(mapping.path("aliases"), "profile analyzer alias");
            List<String> values = new ArrayList<>();
            for (JsonNode value : mapping.path("values")) {
                if (!value.isTextual() || value.asText().isBlank()) {
                    throw new IllegalArgumentException("Bridge analyzer profile result value must be nonblank text");
                }
                values.add(value.asText());
            }
            JsonNode coding = mapping.path("normalized_coding");
            NormalizedCoding normalizedCoding = coding.isMissingNode() || coding.isNull() ? null
                    : new NormalizedCoding(requiredText(coding, "system"), requiredText(coding, "code"),
                            nullableText(coding, "display"));
            tests.add(new TestDefinition(analyzerCode, aliases, nullableText(mapping, "test_name_hint"),
                    requiredText(mapping, "loinc"), nullableText(mapping, "unit"), nullableText(mapping, "result_type"),
                    values, normalizedCoding));
        }

        JsonNode lineage = catalog.path("lineage");
        JsonNode communication = document.path("communication");
        Boolean supportsLisInitiated = nullableBoolean(communication, "supports_lis_initiated");
        if (!"FILE".equals(protocolName) && supportsLisInitiated == null) {
            throw new IllegalArgumentException(
                    "Bridge analyzer profile supports_lis_initiated is required for " + protocolName);
        }
        InstanceDefaults defaults = new InstanceDefaults(nullableText(configDefaults, "transport"),
                nullableText(configDefaults, "connectionRole"), nullableInteger(configDefaults, "port"));
        return new BridgeAnalyzerProfile(document, profileId, revision, fingerprint,
                requiredText(profileMeta, "displayName"), firstText(document, profileMeta, "manufacturer"),
                nullableText(document, "model"), requiredText(catalog, "source"), requiredText(catalog, "status"),
                protocolName, nullableText(protocol, "version"), nullableText(communication, "mode"),
                supportsLisInitiated, nullableText(lineage, "parentProfileId"),
                nullableInteger(lineage, "parentRevision"), tests, defaults);
    }

    public JsonNode document() {
        return document.deepCopy();
    }

    public String profileId() {
        return profileId;
    }

    public int revision() {
        return revision;
    }

    public String revisionFingerprint() {
        return revisionFingerprint;
    }

    public String displayName() {
        return displayName;
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String model() {
        return model;
    }

    public String source() {
        return source;
    }

    public String status() {
        return status;
    }

    public String protocol() {
        return protocol;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public String communicationMode() {
        return communicationMode;
    }

    public Boolean supportsLisInitiated() {
        return supportsLisInitiated;
    }

    public String parentProfileId() {
        return parentProfileId;
    }

    public Integer parentRevision() {
        return parentRevision;
    }

    public List<TestDefinition> testDefinitions() {
        return testDefinitions;
    }

    public InstanceDefaults instanceDefaults() {
        return instanceDefaults;
    }

    private static String requiredText(JsonNode node, String field) {
        String value = nullableText(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Bridge analyzer profile " + field + " is required");
        }
        return value;
    }

    private static String firstText(JsonNode primary, JsonNode secondary, String field) {
        String value = nullableText(primary, field);
        return value == null ? nullableText(secondary, field) : value;
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private static Integer nullableInteger(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isIntegralNumber() && value.canConvertToInt() ? value.asInt() : null;
    }

    private static Boolean nullableBoolean(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isBoolean()) {
            throw new IllegalArgumentException("Bridge analyzer profile " + field + " must be a boolean");
        }
        return value.asBoolean();
    }

    private static List<String> textList(JsonNode values, String label) {
        List<String> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException("Bridge analyzer " + label + " must be nonblank text");
            }
            result.add(value.asText());
        }
        return List.copyOf(result);
    }

    public record TestDefinition(String analyzerCode, List<String> aliases, String testNameHint, String loinc,
            String unit, String resultType, List<String> resultValues, NormalizedCoding normalizedCoding) {
        public TestDefinition {
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            resultValues = resultValues == null ? List.of() : List.copyOf(resultValues);
        }
    }

    public record NormalizedCoding(String system, String code, String display) {
    }

    public record InstanceDefaults(String transport, String connectionRole, Integer port) {
    }
}
