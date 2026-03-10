package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.analyzer.service.FileImportService;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * FileAnalyzerReader - Reads CSV/TXT files for analyzer result import.
 * 
 * Extends AnalyzerReader to support file-based analyzer integration (M3). Uses
 * Apache Commons CSV for parsing.
 */
public class FileAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private List<Map<String, String>> parsedRecords; // Store parsed CSV records for duplicate checking
    private AnalyzerLineInserter inserter;
    private String error;
    private FileImportConfiguration configuration;
    private static final List<String> PREFERRED_FIELD_ORDER = List.of("sampleId", "testCode", "result",
            "interpretation", "position", "testDate", "testTime");

    public FileAnalyzerReader() {
        this.lines = new ArrayList<>();
        this.parsedRecords = new ArrayList<>();
    }

    public FileAnalyzerReader(FileImportConfiguration configuration) {
        this();
        this.configuration = configuration;
    }

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();
        parsedRecords = new ArrayList<>();

        if (configuration == null) {
            error = "FileImportConfiguration not provided";
            return false;
        }

        try {
            CSVFormat csvFormat = CSVFormat.DEFAULT
                    .withDelimiter(configuration.getDelimiter() != null && !configuration.getDelimiter().isEmpty()
                            ? configuration.getDelimiter().charAt(0)
                            : ',');

            if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                csvFormat = csvFormat.withFirstRecordAsHeader();
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                    CSVParser parser = csvFormat.parse(reader)) {

                Map<String, String> columnMappings = configuration.getColumnMappings();

                for (CSVRecord record : parser) {
                    Map<String, String> parsedRecord = new HashMap<>();
                    if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                        for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                            String csvColumn = mapping.getKey();
                            String value = record.get(csvColumn);
                            if (value != null && !value.isEmpty()) {
                                parsedRecord.put(mapping.getValue(), value); // Store with internal field name
                                parsedRecord.put(csvColumn, value); // Also store with CSV column name
                            }
                        }
                    } else {
                        for (int i = 0; i < record.size(); i++) {
                            parsedRecord.put("column_" + i, record.get(i));
                        }
                    }
                    parsedRecords.add(parsedRecord);

                    StringBuilder lineBuilder = new StringBuilder();

                    if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                        buildLineFromRecord(record, columnMappings, lineBuilder);
                    } else {
                        for (int i = 0; i < record.size(); i++) {
                            lineBuilder.append(record.get(i)).append("\t");
                        }
                    }

                    if (lineBuilder.length() > 0 && lineBuilder.charAt(lineBuilder.length() - 1) == '\t') {
                        lineBuilder.setLength(lineBuilder.length() - 1);
                    }

                    String line = lineBuilder.toString();
                    if (!line.isEmpty()) {
                        lines.add(line);
                    }
                }
            }

            if (!lines.isEmpty()) {
                setInserter();
                if (inserter == null) {
                    error = "Unable to understand which analyzer sent the file";
                    return false;
                }
                return true;
            } else {
                error = "Empty file or no valid records found";
                return false;
            }
        } catch (IOException e) {
            error = "Unable to read CSV file: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream",
                    "Error reading CSV file: " + e.getMessage());
            return false;
        } catch (Exception e) {
            error = "Error parsing CSV file: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream",
                    "Error parsing CSV file: " + e.getMessage());
            return false;
        }
    }

    private void setInserter() {
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        if (configuration != null && configuration.getAnalyzerId() != null) {
            AnalyzerImporterPlugin configuredPlugin = pluginService
                    .getPluginByAnalyzerId(configuration.getAnalyzerId().toString());
            if (configuredPlugin != null) {
                inserter = configuredPlugin.getAnalyzerLineInserter();
                return;
            }
        }
        for (AnalyzerImporterPlugin plugin : pluginService.getAnalyzerPlugins()) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    inserter = plugin.getAnalyzerLineInserter();
                    return;
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }

        // No matching plugin — report error to caller
        error = "No matching analyzer plugin found for file format";
    }

    private void buildLineFromRecord(CSVRecord record, Map<String, String> columnMappings, StringBuilder lineBuilder) {
        Map<String, List<String>> internalToCsv = new HashMap<>();
        for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
            internalToCsv.computeIfAbsent(mapping.getValue(), key -> new ArrayList<>()).add(mapping.getKey());
        }

        Set<String> usedColumns = new HashSet<>();
        for (String internalField : PREFERRED_FIELD_ORDER) {
            List<String> csvColumns = internalToCsv.get(internalField);
            if (csvColumns != null) {
                appendFirstNonEmptyValue(record, csvColumns, lineBuilder, usedColumns);
            }
        }

        for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
            String csvColumn = mapping.getKey();
            if (usedColumns.contains(csvColumn)) {
                continue;
            }
            String value = record.get(csvColumn);
            if (value != null && !value.isEmpty()) {
                lineBuilder.append(value).append("\t");
            }
        }
    }

    private void appendFirstNonEmptyValue(CSVRecord record, List<String> csvColumns, StringBuilder lineBuilder,
            Set<String> usedColumns) {
        for (String csvColumn : csvColumns) {
            String value = record.get(csvColumn);
            if (value != null && !value.isEmpty()) {
                lineBuilder.append(value).append("\t");
                usedColumns.add(csvColumn);
                return;
            }
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            return false;
        }

        if (configuration != null && configuration.getAnalyzerId() != null && !parsedRecords.isEmpty()) {
            checkDuplicatesBeforeInsertion();
        }

        // Proceed with insertion (duplicates are handled by AnalyzerResultsServiceImpl)
        boolean success = inserter.insert(lines, systemUserId);
        if (!success) {
            error = inserter.getError();
        }
        return success;
    }

    /**
     * Check for duplicates before insertion and log warnings
     */
    private void checkDuplicatesBeforeInsertion() {
        try {
            FileImportService fileImportService = SpringContext.getBean(FileImportService.class);
            if (fileImportService == null) {
                return; // Service not available, skip duplicate checking
            }

            Map<String, String> columnMappings = configuration.getColumnMappings();
            Integer analyzerId = configuration.getAnalyzerId();

            for (Map<String, String> record : parsedRecords) {
                String sampleId = extractField(record, columnMappings, "sampleId", "Sample_ID", "sample_id");
                String testCode = extractField(record, columnMappings, "testCode", "Test_Code", "test_code");
                String testDate = extractField(record, columnMappings, "testDate", "Date", "date");
                String testTime = extractField(record, columnMappings, "testTime", "Time", "time");

                if (sampleId != null && testCode != null) {
                    boolean isDuplicate = fileImportService.isDuplicate(analyzerId, sampleId, testCode, testDate,
                            testTime);
                    if (isDuplicate) {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "checkDuplicatesBeforeInsertion",
                                "Duplicate result detected (will still be inserted): analyzer=" + analyzerId
                                        + ", sample=" + sampleId + ", test=" + testCode
                                        + (testDate != null ? ", date=" + testDate : "")
                                        + (testTime != null ? ", time=" + testTime : ""));
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail insertion
            LogEvent.logWarn(this.getClass().getSimpleName(), "checkDuplicatesBeforeInsertion",
                    "Error checking duplicates before insertion: " + e.getMessage());
        }
    }

    /**
     * Extract field value from parsed record using multiple possible field names
     */
    private String extractField(Map<String, String> record, Map<String, String> columnMappings,
            String internalFieldName, String... possibleColumnNames) {
        if (record.containsKey(internalFieldName)) {
            return record.get(internalFieldName);
        }

        for (String columnName : possibleColumnNames) {
            if (record.containsKey(columnName)) {
                return record.get(columnName);
            }
            // Also try case-insensitive match
            for (String key : record.keySet()) {
                if (key.equalsIgnoreCase(columnName)) {
                    return record.get(key);
                }
            }
        }

        return null;
    }

    @Override
    public String getError() {
        return error;
    }

    public void setConfiguration(FileImportConfiguration configuration) {
        this.configuration = configuration;
    }

    public FileImportConfiguration getConfiguration() {
        return configuration;
    }
}
