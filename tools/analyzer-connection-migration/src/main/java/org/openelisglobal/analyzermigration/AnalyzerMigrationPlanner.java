package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts the finite released OpenELIS connection shape into values declared
 * by one explicitly selected Bridge profile revision.
 */
public final class AnalyzerMigrationPlanner {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, String> CONNECTION_FIELD_ALIASES = connectionFieldAliases();
    private static final Set<String> PROFILE_OWNED_SOURCE_FIELDS = Set.of("fileFormat", "columnMappings", "delimiter",
            "hasHeader", "skipRows", "protocolVersion", "communicationMode", "identifierPattern", "baudRate",
            "dataBits", "stopBits", "parity", "flowControl");

    public enum Outcome {
        READY, NEEDS_CORRECTION
    }

    public record Decision(Outcome outcome, ObjectNode connectionValues, List<String> reasonCodes) {
        public Decision {
            connectionValues = connectionValues.deepCopy();
            reasonCodes = List.copyOf(reasonCodes);
        }
    }

    public Decision plan(ObjectNode source, ObjectNode selection, ObjectNode profile) {
        ObjectNode values = JSON.createObjectNode();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (!hasExplicitSelection(selection, profile)) {
            reasons.add("EXPLICIT_PROFILE_SELECTION_REQUIRED");
            return decision(values, reasons);
        }
        if (!hasValue(selection.path("selectedBy")) || !validInstant(selection.path("selectedAt").asText(null))) {
            reasons.add("SELECTION_AUDIT_REQUIRED");
        }
        for (JsonNode sourceError : source.path("sourceErrors")) {
            if (sourceError.isTextual() && !sourceError.asText().isBlank()) {
                reasons.add("SOURCE_EXPORT_ERROR:" + sourceError.asText());
            }
        }

        ObjectNode sourceConfiguration = object(source, "configuration");
        ObjectNode defaults = object(profile, "configDefaults");
        Map<String, JsonNode> descriptors = descriptors(profile);
        ObjectNode explicitValues = object(selection, "connectionValues");

        Map<String, String> sourceFieldByTarget = new LinkedHashMap<>();
        sourceConfiguration.fields().forEachRemaining(entry -> {
            String sourceField = entry.getKey();
            JsonNode sourceValue = entry.getValue();
            if (!hasValue(sourceValue)) {
                return;
            }
            String targetField = CONNECTION_FIELD_ALIASES.get(sourceField);
            if (targetField != null) {
                if (descriptors.containsKey(targetField)) {
                    values.set(targetField, sourceValue.deepCopy());
                    sourceFieldByTarget.put(targetField, sourceField);
                } else if (!sameValue(defaults.path(targetField), sourceValue)) {
                    reasons.add("SOURCE_VALUE_NOT_REPRESENTED:" + sourceField);
                }
            } else if (PROFILE_OWNED_SOURCE_FIELDS.contains(sourceField)) {
                if (!sameProfileOwnedValue(profile, sourceField, sourceValue)) {
                    reasons.add("SOURCE_VALUE_NOT_REPRESENTED:" + sourceField);
                }
            } else {
                reasons.add("SOURCE_FIELD_UNSUPPORTED:" + sourceField);
            }
        });

        explicitValues.fields().forEachRemaining(entry -> {
            if (descriptors.containsKey(entry.getKey()) && hasValue(entry.getValue())) {
                values.set(entry.getKey(), entry.getValue().deepCopy());
            } else {
                reasons.add("UNDECLARED_CONNECTION_OVERRIDE:" + entry.getKey());
            }
        });

        ObjectNode effectiveValues = defaults.deepCopy();
        copyFields(values, effectiveValues);
        for (Map.Entry<String, JsonNode> entry : descriptors.entrySet()) {
            String fieldKey = entry.getKey();
            JsonNode descriptor = entry.getValue();
            if (!isVisible(descriptor, effectiveValues, descriptors, new HashSet<>()) && values.has(fieldKey)) {
                String sourceField = sourceFieldByTarget.get(fieldKey);
                reasons.add(sourceField == null ? "CONNECTION_OVERRIDE_NOT_APPLICABLE:" + fieldKey
                        : "SOURCE_VALUE_NOT_REPRESENTED:" + sourceField);
                values.remove(fieldKey);
            }
        }

        effectiveValues = defaults.deepCopy();
        copyFields(values, effectiveValues);
        for (Map.Entry<String, JsonNode> entry : descriptors.entrySet()) {
            JsonNode descriptor = entry.getValue();
            String fieldKey = entry.getKey();
            if (descriptor.path("required").asBoolean(false)
                    && isVisible(descriptor, effectiveValues, descriptors, new HashSet<>())
                    && !hasValue(effectiveValues.path(fieldKey))) {
                reasons.add("REQUIRED_CONNECTION_VALUE_MISSING:" + fieldKey);
            }
        }

        return decision(values, reasons);
    }

    private static Decision decision(ObjectNode values, LinkedHashSet<String> reasons) {
        Outcome outcome = reasons.isEmpty() ? Outcome.READY : Outcome.NEEDS_CORRECTION;
        return new Decision(outcome, values, new ArrayList<>(reasons));
    }

    private static boolean hasExplicitSelection(ObjectNode selection, ObjectNode profile) {
        if (selection == null || !"EXPLICIT".equals(selection.path("method").asText())) {
            return false;
        }
        JsonNode profileRef = selection.path("profileRef");
        return profileRef.isObject()
                && profileRef.path("profileId").asText().equals(profile.path("profileMeta").path("id").asText())
                && profileRef.path("revision").asInt() == profile.path("catalog").path("revision").asInt()
                && profileRef.path("fingerprint").asText()
                        .equals(profile.path("catalog").path("revisionFingerprint").asText());
    }

    private static Map<String, JsonNode> descriptors(ObjectNode profile) {
        Map<String, JsonNode> descriptors = new LinkedHashMap<>();
        for (JsonNode field : profile.path("connectionFields")) {
            descriptors.put(field.path("key").asText(), field);
        }
        return descriptors;
    }

    private static boolean isVisible(JsonNode descriptor, ObjectNode values, Map<String, JsonNode> descriptors,
            Set<String> visiting) {
        String key = descriptor.path("key").asText();
        if (!visiting.add(key)) {
            return false;
        }
        try {
            JsonNode condition = descriptor.path("visibleWhen");
            if (!condition.isObject()) {
                return true;
            }
            String controllingKey = condition.path("fieldKey").asText();
            JsonNode controllingDescriptor = descriptors.get(controllingKey);
            if (controllingDescriptor != null && !isVisible(controllingDescriptor, values, descriptors, visiting)) {
                return false;
            }
            JsonNode actual = values.path(controllingKey);
            JsonNode expected = condition.path("value");
            return switch (condition.path("operator").asText()) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "IN" -> expected.isArray() && contains(expected, actual);
            case "NOT_IN" -> expected.isArray() && !contains(expected, actual);
            default -> false;
            };
        } finally {
            visiting.remove(key);
        }
    }

    private static boolean contains(JsonNode values, JsonNode sought) {
        for (JsonNode value : values) {
            if (value.equals(sought)) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode profileOwnedValue(ObjectNode profile, String sourceField) {
        return switch (sourceField) {
        case "fileFormat" -> profile.path("configDefaults").path("fileFormat");
        case "columnMappings" -> profile.path("column_mapping");
        case "delimiter" -> profile.path("configDefaults").path("delimiter");
        case "hasHeader" -> profile.path("configDefaults").path("hasHeader");
        case "skipRows" -> profile.path("configDefaults").path("skipRows");
        case "communicationMode" -> profile.path("communication").path("mode");
        case "identifierPattern" -> profile.path("identifier_pattern");
        case "baudRate" -> profile.path("transport_config").path("RS-232").path("default_baud_rate");
        case "dataBits" -> profile.path("transport_config").path("RS-232").path("data_bits");
        case "parity" -> profile.path("transport_config").path("RS-232").path("parity");
        case "flowControl" -> profile.path("transport_config").path("RS-232").path("flow_control");
        default -> JSON.missingNode();
        };
    }

    private static boolean sameProfileOwnedValue(ObjectNode profile, String sourceField, JsonNode sourceValue) {
        return switch (sourceField) {
        case "protocolVersion" -> sameProtocol(profile.path("protocol"), sourceValue.asText());
        case "stopBits" -> sameStopBits(profile.path("transport_config").path("RS-232").path("stop_bits"), sourceValue);
        default -> sameValue(profileOwnedValue(profile, sourceField), sourceValue);
        };
    }

    private static boolean sameProtocol(JsonNode protocol, String releasedProtocol) {
        String name = protocol.path("name").asText();
        String version = protocol.path("version").asText();
        return switch (releasedProtocol) {
        case "ASTM_LIS2_A2" -> "ASTM".equals(name) && ("E-1394-97".equals(version) || "LIS2-A2".equals(version));
        case "HL7_V2_3_1" -> "HL7".equals(name) && version.startsWith("2.3.1");
        case "HL7_V2_5" -> "HL7".equals(name) && version.startsWith("2.5");
        default -> false;
        };
    }

    private static boolean sameStopBits(JsonNode profileValue, JsonNode sourceValue) {
        String released = sourceValue.asText();
        double normalized = switch (released) {
        case "ONE" -> 1.0;
        case "ONE_POINT_FIVE" -> 1.5;
        case "TWO" -> 2.0;
        default -> Double.NaN;
        };
        return !Double.isNaN(normalized) && Double.compare(normalized, profileValue.asDouble(Double.NaN)) == 0;
    }

    private static boolean sameValue(JsonNode left, JsonNode right) {
        return !left.isMissingNode() && left.equals(right);
    }

    private static boolean hasValue(JsonNode value) {
        return value != null && !value.isMissingNode() && !value.isNull()
                && (!value.isTextual() || !value.asText().isBlank());
    }

    private static boolean validInstant(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Instant.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException exception) {
            return false;
        }
    }

    private static void copyFields(ObjectNode source, ObjectNode target) {
        source.fields().forEachRemaining(entry -> target.set(entry.getKey(), entry.getValue().deepCopy()));
    }

    private static ObjectNode object(ObjectNode parent, String field) {
        JsonNode value = parent == null ? null : parent.path(field);
        return value instanceof ObjectNode object ? object : JSON.createObjectNode();
    }

    private static Map<String, String> connectionFieldAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("ipAddress", "host");
        aliases.put("port", "port");
        aliases.put("importDirectory", "directory");
        aliases.put("filePattern", "filePattern");
        aliases.put("portName", "serialPort");
        aliases.put("serialPort", "serialPort");
        aliases.put("transport", "transport");
        aliases.put("connectionRole", "connectionRole");
        return Map.copyOf(aliases);
    }
}
