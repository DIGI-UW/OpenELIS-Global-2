package org.openelisglobal.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses report definition JSON (definition_json) into a list of ReportColumn
 * for use by report builders. Expects structure:
 * {"columns":[{"key":"...","header":"...","type":"..."}, ...]}
 */
public final class ReportDefinitionColumnParser {

    private static final Logger logger = LoggerFactory.getLogger(ReportDefinitionColumnParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportDefinitionColumnParser() {
    }

    /**
     * Parse definition JSON into report columns. Returns empty list if json is
     * null/blank or invalid.
     *
     * @param definitionJson JSON string with "columns" array of {key, header, type}
     * @return list of ReportColumn, or empty list on parse failure
     */
    public static List<ReportColumn> parseColumns(String definitionJson) {
        List<ReportColumn> columns = new ArrayList<>();
        if (definitionJson == null || definitionJson.isBlank()) {
            return columns;
        }
        try {
            JsonNode root = MAPPER.readTree(definitionJson);
            JsonNode cols = root.get("columns");
            if (cols == null || !cols.isArray()) {
                return columns;
            }
            for (JsonNode c : cols) {
                String key = c.has("key") ? c.get("key").asText() : "";
                String header = c.has("header") ? c.get("header").asText() : key;
                String type = c.has("type") ? c.get("type").asText() : "String";
                columns.add(new ReportColumn(key, header, type));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse report definition JSON for columns: {}", e.getMessage());
        }
        return columns;
    }
}
