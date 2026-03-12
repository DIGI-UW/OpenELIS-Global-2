package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openelisglobal.analyzer.valueholder.FileImportConfiguration;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Excel reader for FILE analyzers (.xls/.xlsx).
 *
 * Produces the same tab-delimited line contract as FileAnalyzerReader so it can
 * feed existing AnalyzerLineInserter plugins.
 */
public class ExcelAnalyzerReader extends AnalyzerReader {

    private static final List<String> PREFERRED_FIELD_ORDER = List.of("sampleId", "testCode", "result",
            "interpretation", "position", "testDate", "testTime");

    private final FileImportConfiguration configuration;
    private final List<String> lines = new ArrayList<>();
    private final List<Map<String, String>> parsedRecords = new ArrayList<>();
    private AnalyzerLineInserter inserter;
    private String error;

    public ExcelAnalyzerReader(FileImportConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines.clear();
        parsedRecords.clear();

        if (configuration == null) {
            error = "FileImportConfiguration not provided";
            return false;
        }

        try (Workbook workbook = WorkbookFactory.create(stream)) {
            Sheet sheet = resolveSheet(workbook);
            if (sheet == null) {
                error = "Empty workbook or missing sheet";
                return false;
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, String> columnMappings = configuration.getColumnMappings();

            if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                int headerRowIndex = findHeaderRow(sheet, formatter);
                if (headerRowIndex < 0) {
                    error = "Excel header row is missing (expected row with 'Well' or column headers)";
                    return false;
                }

                Row headerRow = sheet.getRow(headerRowIndex);
                if (headerRow == null) {
                    error = "Excel header row is missing";
                    return false;
                }

                Map<String, Integer> headerIndex = buildHeaderIndex(headerRow, formatter);
                prependHeaderLine(headerRow, formatter, headerIndex);
                for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }

                    Map<String, String> parsedRecord = new HashMap<>();
                    for (Map.Entry<String, String> mapping : columnMappings.entrySet()) {
                        Integer cellIndex = headerIndex.get(mapping.getKey());
                        if (cellIndex == null) {
                            continue;
                        }
                        String value = formatter.formatCellValue(row.getCell(cellIndex));
                        if (value != null && !value.isBlank()) {
                            parsedRecord.put(mapping.getValue(), value);
                            parsedRecord.put(mapping.getKey(), value);
                        }
                    }
                    appendRecord(parsedRecord);
                }
            } else {
                for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) {
                        continue;
                    }
                    Map<String, String> parsedRecord = new HashMap<>();
                    short lastCellNum = row.getLastCellNum();
                    if (lastCellNum < 0) {
                        continue;
                    }
                    for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        String value = formatter.formatCellValue(cell);
                        if (value != null && !value.isBlank()) {
                            parsedRecord.put("column_" + cellIndex, value);
                        }
                    }
                    appendRecord(parsedRecord);
                }
            }

            if (lines.isEmpty()) {
                error = "Empty file or no valid records found";
                return false;
            }

            setInserter();
            if (inserter == null) {
                error = "Unable to understand which analyzer sent the file";
                return false;
            }
            return true;
        } catch (IOException e) {
            error = "Unable to read Excel file: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream", error);
            return false;
        } catch (Exception e) {
            error = "Error parsing Excel file: " + e.getMessage();
            LogEvent.logError(this.getClass().getSimpleName(), "readStream", error);
            return false;
        }
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            return false;
        }

        boolean success = inserter.insert(lines, systemUserId);
        if (!success) {
            error = inserter.getError();
        }
        return success;
    }

    @Override
    public String getError() {
        return error;
    }

    @Override
    public List<Map<String, String>> getParsedRecords() {
        return parsedRecords == null ? List.of() : new ArrayList<>(parsedRecords);
    }

    @Override
    public List<String> getLines() {
        return new ArrayList<>(lines);
    }

    private Sheet resolveSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            return null;
        }
        Sheet byName = workbook.getSheet("Results");
        if (byName != null) {
            return byName;
        }
        return workbook.getSheetAt(0);
    }

    /**
     * Find header row. For QuantStudio-style files with metadata block, scans
     * downward for row where first cell equals "Well". Otherwise uses first row.
     */
    private int findHeaderRow(Sheet sheet, DataFormatter formatter) {
        int firstRow = sheet.getFirstRowNum();
        Row first = sheet.getRow(firstRow);
        if (first == null) {
            return -1;
        }
        String firstCell = formatter.formatCellValue(first.getCell(0));
        if (firstCell != null && firstCell.trim().equals("Well")) {
            return firstRow;
        }
        if (firstCell != null && (firstCell.contains("Block Type") || firstCell.contains("Experiment Name"))) {
            for (int r = firstRow + 1; r <= Math.min(firstRow + 60, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String cell0 = formatter.formatCellValue(row.getCell(0));
                if (cell0 != null && cell0.trim().equals("Well")) {
                    return r;
                }
            }
        }
        return firstRow;
    }

    /**
     * Prepend header line so the inserter can parse data rows by column name. Uses
     * Excel column names from headerRow, limited to columns in columnMappings.
     */
    private void prependHeaderLine(Row headerRow, DataFormatter formatter, Map<String, Integer> headerIndex) {
        Map<String, String> columnMappings = configuration.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            return;
        }
        List<String> headerNames = new ArrayList<>();
        for (String internalField : PREFERRED_FIELD_ORDER) {
            for (Map.Entry<String, String> e : columnMappings.entrySet()) {
                if (e.getValue().equals(internalField) && headerIndex.containsKey(e.getKey())) {
                    headerNames.add(e.getKey());
                    break;
                }
            }
        }
        for (Map.Entry<String, String> e : columnMappings.entrySet()) {
            if (!headerNames.contains(e.getKey()) && headerIndex.containsKey(e.getKey())) {
                headerNames.add(e.getKey());
            }
        }
        if (!headerNames.isEmpty()) {
            lines.add(0, String.join("\t", headerNames));
        }
    }

    private Map<String, Integer> buildHeaderIndex(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> index = new HashMap<>();
        short lastCellNum = headerRow.getLastCellNum();
        if (lastCellNum < 0) {
            return index;
        }
        for (int i = 0; i < lastCellNum; i++) {
            String header = formatter.formatCellValue(headerRow.getCell(i));
            if (header != null && !header.isBlank()) {
                index.put(header.trim(), i);
            }
        }
        return index;
    }

    private void appendRecord(Map<String, String> parsedRecord) {
        if (parsedRecord == null || parsedRecord.isEmpty()) {
            return;
        }
        parsedRecords.add(parsedRecord);

        StringBuilder lineBuilder = new StringBuilder();
        Set<String> usedKeys = new HashSet<>();

        for (String internalField : PREFERRED_FIELD_ORDER) {
            String value = parsedRecord.get(internalField);
            if (value != null && !value.isBlank()) {
                lineBuilder.append(value).append("\t");
                usedKeys.add(internalField);
            }
        }
        for (Map.Entry<String, String> entry : parsedRecord.entrySet()) {
            if (usedKeys.contains(entry.getKey())) {
                continue;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                lineBuilder.append(value).append("\t");
            }
        }
        if (lineBuilder.length() > 0) {
            lineBuilder.setLength(lineBuilder.length() - 1);
            lines.add(lineBuilder.toString());
        }
    }

    private void setInserter() {
        PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
        if (configuration != null && configuration.getAnalyzerId() != null) {
            String analyzerId = configuration.getAnalyzerId().toString();
            AnalyzerImporterPlugin configuredPlugin = pluginService.getPluginByAnalyzerId(analyzerId);
            if (configuredPlugin == null) {
                configuredPlugin = findPluginByConfiguredAnalyzerType();
            }
            if (configuredPlugin != null) {
                inserter = configuredPlugin.getAnalyzerLineInserter();
                inserter.setContextAnalyzerId(analyzerId);
                return;
            }
        }
        for (AnalyzerImporterPlugin plugin : pluginService.getAnalyzerPlugins()) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    inserter = plugin.getAnalyzerLineInserter();
                    if (configuration != null && configuration.getAnalyzerId() != null) {
                        inserter.setContextAnalyzerId(String.valueOf(configuration.getAnalyzerId()));
                    }
                    return;
                }
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "setInserter",
                        plugin.getClass().getName() + ".isTargetAnalyzer() threw: " + e.getMessage());
            }
        }
    }

    private AnalyzerImporterPlugin findPluginByConfiguredAnalyzerType() {
        try {
            org.openelisglobal.analyzer.service.AnalyzerService analyzerService = SpringContext
                    .getBean(org.openelisglobal.analyzer.service.AnalyzerService.class);
            if (analyzerService == null || configuration == null || configuration.getAnalyzerId() == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findPluginByConfiguredAnalyzerType",
                        "Null guard: analyzerService=" + (analyzerService != null) + ", config="
                                + (configuration != null));
                return null;
            }

            org.openelisglobal.analyzer.valueholder.Analyzer analyzer = analyzerService
                    .getWithType(String.valueOf(configuration.getAnalyzerId())).orElse(null);
            if (analyzer == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findPluginByConfiguredAnalyzerType",
                        "Analyzer not found for id: " + configuration.getAnalyzerId());
                return null;
            }
            if (analyzer.getAnalyzerType() == null || analyzer.getAnalyzerType().getPluginClassName() == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "findPluginByConfiguredAnalyzerType", "Analyzer "
                        + analyzer.getId() + " has no type or pluginClassName. type=" + analyzer.getAnalyzerType());
                return null;
            }

            String pluginClassName = analyzer.getAnalyzerType().getPluginClassName();
            PluginAnalyzerService pluginService = SpringContext.getBean(PluginAnalyzerService.class);
            for (AnalyzerImporterPlugin plugin : pluginService.getAnalyzerPlugins()) {
                if (plugin.getClass().getName().equals(pluginClassName)) {
                    return plugin;
                }
            }
            LogEvent.logWarn(this.getClass().getSimpleName(), "findPluginByConfiguredAnalyzerType",
                    "No registered plugin matches class: " + pluginClassName);
        } catch (Exception e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "findPluginByConfiguredAnalyzerType",
                    "Unable to resolve plugin by analyzer type: " + e.getMessage());
        }
        return null;
    }
}
