package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.SerialPortConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Produces the frozen, profile-neutral input for the one-time M3 migration. */
@Service
public class AnalyzerMigrationSourceService {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper CANONICAL_JSON = canonicalMapper();

    private final AnalyzerService analyzerService;
    private final SerialPortService serialPortService;

    public AnalyzerMigrationSourceService(AnalyzerService analyzerService, SerialPortService serialPortService) {
        this.analyzerService = analyzerService;
        this.serialPortService = serialPortService;
    }

    @Transactional(readOnly = true)
    public ObjectNode snapshot() {
        ObjectNode snapshot = JSON.createObjectNode();
        snapshot.put("schemaVersion", "1.0");
        ArrayNode exported = snapshot.putArray("analyzers");
        List<Analyzer> analyzers = analyzerService.getAllWithTypes().stream()
                .sorted(Comparator.comparing(Analyzer::getId)).toList();
        analyzers.forEach(analyzer -> exported.add(export(analyzer)));
        return snapshot;
    }

    private ObjectNode export(Analyzer analyzer) {
        ObjectNode source = JSON.createObjectNode();
        source.put("sourceAnalyzerId", analyzer.getId());
        source.put("displayName", analyzer.getName());
        source.put("legacyStatus", analyzer.getStatus() == null ? null : analyzer.getStatus().name());
        source.put("legacyActive", analyzer.isActive());
        ObjectNode configuration = source.putObject("configuration");
        ArrayNode errors = source.putArray("sourceErrors");

        putText(configuration, "ipAddress", analyzer.getIpAddress());
        putNumber(configuration, "port", analyzer.getPort());
        putText(configuration, "protocolVersion",
                analyzer.getProtocolVersion() == null ? null : analyzer.getProtocolVersion().name());
        putText(configuration, "communicationMode",
                analyzer.getCommunicationMode() == null ? null : analyzer.getCommunicationMode().name());
        putText(configuration, "importDirectory", analyzer.getImportDirectory());
        putText(configuration, "filePattern", analyzer.getFilePattern());
        putColumnMappings(configuration, errors, analyzer.getColumnMappingsJson());
        putText(configuration, "fileFormat", analyzer.getFileFormat());
        putText(configuration, "delimiter", analyzer.getDelimiter());
        putBoolean(configuration, "hasHeader", analyzer.getHasHeader());
        putNumber(configuration, "skipRows", analyzer.getSkipRows());
        putText(configuration, "identifierPattern", analyzer.getIdentifierPattern());

        try {
            serialPortService.getByAnalyzerId(Integer.valueOf(analyzer.getId()))
                    .ifPresent(serial -> putSerial(configuration, serial));
        } catch (NumberFormatException exception) {
            errors.add("SERIAL_CONFIGURATION_LOOKUP_UNAVAILABLE");
        }

        source.put("sourceConfigFingerprint", fingerprint(source));
        return source;
    }

    private static void putColumnMappings(ObjectNode configuration, ArrayNode errors, String rawMappings) {
        if (rawMappings == null || rawMappings.isBlank()) {
            return;
        }
        try {
            JsonNode mappings = JSON.readTree(rawMappings);
            if (!mappings.isObject()) {
                errors.add("COLUMN_MAPPINGS_INVALID_JSON");
                return;
            }
            configuration.set("columnMappings", mappings);
        } catch (JsonProcessingException exception) {
            errors.add("COLUMN_MAPPINGS_INVALID_JSON");
        }
    }

    private static void putSerial(ObjectNode configuration, SerialPortConfiguration serial) {
        configuration.put("transport", "RS-232");
        putText(configuration, "portName", serial.getPortName());
        putNumber(configuration, "baudRate", serial.getBaudRate());
        putNumber(configuration, "dataBits", serial.getDataBits());
        putText(configuration, "stopBits", serial.getStopBits() == null ? null : serial.getStopBits().name());
        putText(configuration, "parity", serial.getParity() == null ? null : serial.getParity().name());
        putText(configuration, "flowControl", serial.getFlowControl() == null ? null : serial.getFlowControl().name());
    }

    private static void putText(ObjectNode target, String field, String value) {
        if (value != null && !value.isBlank()) {
            target.put(field, value);
        }
    }

    private static void putNumber(ObjectNode target, String field, Number value) {
        if (value != null) {
            target.put(field, value.intValue());
        }
    }

    private static void putBoolean(ObjectNode target, String field, Boolean value) {
        if (value != null) {
            target.put(field, value);
        }
    }

    private static String fingerprint(ObjectNode source) {
        ObjectNode material = source.deepCopy();
        material.remove("sourceConfigFingerprint");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(CANONICAL_JSON.writeValueAsBytes(material));
            return "sha256:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Cannot fingerprint analyzer migration source", exception);
        }
    }

    private static ObjectMapper canonicalMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return mapper;
    }
}
