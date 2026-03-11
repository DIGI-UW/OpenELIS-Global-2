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
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                error = "Empty workbook or missing sheet";
                return false;
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, String> columnMappings = configuration.getColumnMappings();

            if (configuration.getHasHeader() != null && configuration.getHasHeader()) {
                Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                if (headerRow == null) {
                    error = "Excel header row is missing";
                    return false;
                }

                Map<String, Integer> headerIndex = buildHeaderIndex(headerRow, formatter);
                for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
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
            AnalyzerImporterPlugin configuredPlugin = pluginService
                    .getPluginByAnalyzerId(configuration.getAnalyzerId().toString());
            if (configuredPlugin != null) {
                inserter = configuredPlugin.getAnalyzerLineInserter();
                inserter.setContextAnalyzerId(String.valueOf(configuration.getAnalyzerId()));
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
                LogEvent.logError(e);
            }
        }
    }
}
