package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * FileAnalyzerReader - Reads CSV/TXT files for analyzer result import.
 * 
 * Extends AnalyzerReader to support file-based analyzer integration (M3).
 * Uses Apache Commons CSV for parsing.
 */
public class FileAnalyzerReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerLineInserter inserter;
    private String error;
    private FileImportConfiguration configuration;

    public FileAnalyzerReader() {
        this.lines = new ArrayList<>();
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

        if (configuration == null) {
            error = "FileImportConfiguration not provided";
            return false;
        }

        try {
            // Determine CSV format based on configuration
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(
                    configuration.getDelimiter() != null && !configuration.getDelimiter().isEmpty()
                            ? configuration.getDelimiter().charAt(0)
                            : ',');

            if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                csvFormat = csvFormat.withFirstRecordAsHeader();
            }

            // Parse CSV file
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                    CSVParser parser = csvFormat.parse(reader)) {

                Map<String, String> columnMappings = configuration.getColumnMappings();

                for (CSVRecord record : parser) {
                    // Convert CSV record to line format expected by AnalyzerLineInserter
                    StringBuilder lineBuilder = new StringBuilder();

                    if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                        // Use column mappings to extract values
                        for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                            String csvColumn = mapping.getKey();
                            String value = record.get(csvColumn);
                            if (value != null && !value.isEmpty()) {
                                lineBuilder.append(value).append("\t");
                            }
                        }
                    } else {
                        // No header - use record values directly
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
        // Try to find matching plugin analyzer
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
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

        // If no plugin matches, we might need a generic CSV inserter
        // For now, set error if no inserter found
        error = "No matching analyzer plugin found for file format";
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            return false;
        } else {
            boolean success = inserter.insert(lines, systemUserId);
            if (!success) {
                error = inserter.getError();
            }
            return success;
        }
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
