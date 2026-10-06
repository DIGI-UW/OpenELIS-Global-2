package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final String parentProfileId;
    private final Integer parentRevision;
    private final List<TestDefinition> testDefinitions;

    private BridgeAnalyzerProfile(JsonNode document, String profileId, int revision, String revisionFingerprint,
            String displayName, String manufacturer, String model, String source, String status, String protocol,
            String protocolVersion, String communicationMode, String parentProfileId, Integer parentRevision,
            List<TestDefinition> testDefinitions) {
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
        this.parentProfileId = parentProfileId;
        this.parentRevision = parentRevision;
        this.testDefinitions = List.copyOf(testDefinitions);
    }

    public static BridgeAnalyzerProfile from(JsonNode document) {
        if (document == null || !document.isObject()) {
            throw new IllegalArgumentException("Bridge analyzer profile must be an object");
        }
        JsonNode profileMeta = document.path("profileMeta");
        JsonNode catalog = document.path("catalog");
        JsonNode protocol = document.path("protocol");
        String profileId = requiredText(profileMeta, "id");
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
            List<String> values = values(mapping.path("values"));
            JsonNode coding = mapping.path("normalized_coding");
            NormalizedCoding normalizedCoding = coding.isMissingNode() || coding.isNull() ? null
                    : new NormalizedCoding(requiredText(coding, "system"), requiredText(coding, "code"),
                            nullableText(coding, "display"));
            tests.add(new TestDefinition(analyzerCode, aliases, nullableText(mapping, "test_name_hint"),
                    requiredText(mapping, "loinc"), nullableText(mapping, "unit"), nullableText(mapping, "result_type"),
                    values, normalizedCoding, valueCodes(mapping.path("value_codes"), values),
                    nullableText(mapping, "call_component"), components(mapping.path("components")),
                    translations(mapping.path("translations"), values)));
        }

        JsonNode lineage = catalog.path("lineage");
        JsonNode communication = document.path("communication");
        return new BridgeAnalyzerProfile(document, profileId, revision, fingerprint,
                requiredText(profileMeta, "displayName"), firstText(document, profileMeta, "manufacturer"),
                nullableText(document, "model"), requiredText(catalog, "source"), requiredText(catalog, "status"),
                requiredText(protocol, "name"), nullableText(protocol, "version"), nullableText(communication, "mode"),
                nullableText(lineage, "parentProfileId"), nullableInteger(lineage, "parentRevision"), tests);
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

    public String parentProfileId() {
        return parentProfileId;
    }

    public Integer parentRevision() {
        return parentRevision;
    }

    public List<TestDefinition> testDefinitions() {
        return testDefinitions;
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

    private static Map<String, NormalizedCoding> valueCodes(JsonNode codes, List<String> values) {
        if (codes.isMissingNode() || codes.isNull()) {
            return Map.of();
        }
        if (!codes.isObject()) {
            throw new IllegalArgumentException("Bridge analyzer value codes must be an object");
        }
        Map<String, NormalizedCoding> result = new LinkedHashMap<>();
        codes.fields().forEachRemaining(entry -> {
            if (!values.contains(entry.getKey())) {
                throw new IllegalArgumentException("Bridge analyzer value code must name a declared raw value");
            }
            JsonNode coding = entry.getValue();
            result.put(entry.getKey(), new NormalizedCoding(requiredText(coding, "system"),
                    requiredText(coding, "code"), nullableText(coding, "display")));
        });
        return Map.copyOf(result);
    }

    /**
     * A profile test. Its values belong to its main record; when it names a
     * {@code callComponent}, those values are the main record's call and land on
     * that component. Each component with a sub-identity receives one more record
     * of the test.
     */
    public record TestDefinition(String analyzerCode, List<String> aliases, String testNameHint, String loinc,
            String unit, String resultType, List<String> resultValues, NormalizedCoding normalizedCoding,
            Map<String, NormalizedCoding> valueCodes, String callComponent, List<ComponentDefinition> components,
            Map<String, List<String>> translations) {
        public TestDefinition {
            translations = translations == null ? Map.of() : Map.copyOf(translations);
            valueCodes = valueCodes == null ? Map.of() : Map.copyOf(valueCodes);
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            resultValues = resultValues == null ? List.of() : List.copyOf(resultValues);
            components = components == null ? List.of() : List.copyOf(components);
        }

        public TestDefinition(String analyzerCode, List<String> aliases, String testNameHint, String loinc, String unit,
                String resultType, List<String> resultValues, NormalizedCoding normalizedCoding,
                Map<String, NormalizedCoding> valueCodes) {
            this(analyzerCode, aliases, testNameHint, loinc, unit, resultType, resultValues, normalizedCoding,
                    valueCodes, null, List.of(), Map.of());
        }

        /** The components that receive a record of their own, in profile order. */
        public List<ComponentDefinition> recordComponents() {
            return components.stream().filter(component -> component.subIdentity() != null).toList();
        }
    }

    /**
     * A component the profile declares for a test, named by the stable code a local
     * component carries; {@code subIdentity} is the record it receives, in the
     * vendor's sub-ID notation (HIV-1&Ct), or null for the call component.
     */
    public record ComponentDefinition(String code, String label, String resultType, String unit, String subIdentity,
            List<String> resultValues, Map<String, NormalizedCoding> valueCodes,
            Map<String, List<String>> translations) {
        public ComponentDefinition {
            resultValues = resultValues == null ? List.of() : List.copyOf(resultValues);
            valueCodes = valueCodes == null ? Map.of() : Map.copyOf(valueCodes);
            translations = translations == null ? Map.of() : Map.copyOf(translations);
        }
    }

    /**
     * The vendor's translations of declared values (Cepheid 303-0251 §3): each key
     * a declared value, each entry the same result in another language.
     */
    private static Map<String, List<String>> translations(JsonNode node, List<String> values) {
        if (node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("Bridge analyzer translations must be an object");
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (!values.contains(entry.getKey())) {
                throw new IllegalArgumentException("Bridge analyzer translation must name a declared raw value");
            }
            result.put(entry.getKey(), values(entry.getValue()));
        });
        return result;
    }

    private static List<String> values(JsonNode node) {
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new IllegalArgumentException("Bridge analyzer profile result value must be nonblank text");
            }
            values.add(value.asText());
        }
        return values;
    }

    private static List<ComponentDefinition> components(JsonNode node) {
        List<ComponentDefinition> components = new ArrayList<>();
        for (JsonNode component : node) {
            List<String> values = values(component.path("values"));
            components.add(new ComponentDefinition(requiredText(component, "code"), nullableText(component, "label"),
                    nullableText(component, "result_type"), nullableText(component, "unit"),
                    nullableText(component, "sub_identity"), values, valueCodes(component.path("value_codes"), values),
                    translations(component.path("translations"), values)));
        }
        return components;
    }

    public record NormalizedCoding(String system, String code, String display) {
    }

}
