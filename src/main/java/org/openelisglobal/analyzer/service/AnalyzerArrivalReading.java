package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * How the profile a delivery was parsed under read it (spec point 13): the type
 * reading and every row reading. A held result keeps the part for its own
 * record, so it can later be compared with whatever reading a person confirms,
 * whether or not the Bridge still serves that profile.
 */
public record AnalyzerArrivalReading(JsonNode type, Map<AnalyzerMappingSourceRow, JsonNode> rows) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public AnalyzerArrivalReading {
        Objects.requireNonNull(type, "type");
        rows = Map.copyOf(rows);
    }

    /** The type reading and the readings of one record and its values, as JSON. */
    public String forRecord(String code, String subIdentity) {
        String sub = subIdentity == null ? "" : subIdentity;
        ObjectNode record = JSON.createObjectNode();
        record.set("type", type);
        ArrayNode readings = record.putArray("rows");
        rows.forEach((row, reading) -> {
            if (row.sourceRowKey().equals(code) && sub.equals(row.subIdentity())) {
                ObjectNode entry = readings.addObject();
                if (row.rawValue() != null) {
                    entry.put("value", row.rawValue());
                }
                entry.set("reading", reading);
            }
        });
        return record.toString();
    }

    /** A record's reading as {@link #forRecord} wrote it; empty when unreadable. */
    public static Optional<RecordReading> parseRecord(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = JSON.readTree(json);
            if (!node.has("type") || !node.path("rows").isArray()) {
                return Optional.empty();
            }
            Map<String, JsonNode> byValue = new LinkedHashMap<>();
            node.path("rows").forEach(entry -> byValue
                    .put(entry.hasNonNull("value") ? entry.path("value").asText() : null, entry.path("reading")));
            return Optional.of(new RecordReading(node.get("type"), byValue));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    /**
     * One record's reading: the type's, and the record's own keyed by value, the
     * record itself under null.
     */
    public record RecordReading(JsonNode type, Map<String, JsonNode> byValue) {
    }
}
