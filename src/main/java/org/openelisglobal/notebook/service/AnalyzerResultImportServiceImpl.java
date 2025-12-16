package org.openelisglobal.notebook.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.notebook.dao.AnalyzerResultImportDAO;
import org.openelisglobal.notebook.valueholder.AnalyzerResultImport;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.SampleRouting;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for AnalyzerResultImport operations. Implements
 * CSV/Excel parsing, well coordinate matching, and result storage.
 */
@Service
public class AnalyzerResultImportServiceImpl extends AuditableBaseObjectServiceImpl<AnalyzerResultImport, Integer>
        implements AnalyzerResultImportService {

    private static final int MAX_ROWS = 10000;

    @Autowired
    private AnalyzerResultImportDAO baseObjectDAO;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private SystemUserService systemUserService;

    @Autowired
    private SampleRoutingService sampleRoutingService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    public AnalyzerResultImportServiceImpl() {
        super(AnalyzerResultImport.class);
    }

    @Override
    protected BaseDAO<AnalyzerResultImport, Integer> getBaseObjectDAO() {
        return baseObjectDAO;
    }

    // ========== Existing Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerResultImport> getByNotebookPageId(Integer notebookPageId) {
        return baseObjectDAO.getByNotebookPageId(notebookPageId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerResultImport getLatestByNotebookPageId(Integer notebookPageId) {
        return baseObjectDAO.getLatestByNotebookPageId(notebookPageId);
    }

    @Override
    @Transactional
    public AnalyzerResultImport recordImport(Integer notebookPageId, Integer analyzerId, String fileName, int totalRows,
            int successfulRows, int failedRows, Map<String, String> columnMapping,
            List<Map<String, Object>> errorDetails, String userId) {
        // Delegate to extended version with null metadata
        return recordImport(notebookPageId, analyzerId, fileName, totalRows, successfulRows, failedRows, columnMapping,
                errorDetails, null, null, null, null, null, userId);
    }

    @Override
    @Transactional
    public AnalyzerResultImport recordImport(Integer notebookPageId, Integer analyzerId, String fileName, int totalRows,
            int successfulRows, int failedRows, Map<String, String> columnMapping,
            List<Map<String, Object>> errorDetails, String assayRunId, String operatorId,
            Map<String, String> machineParameters, List<String> reagentLots, String fileFormat, String userId) {

        // Validate total
        if (successfulRows + failedRows != totalRows) {
            throw new IllegalArgumentException(
                    "Row counts don't add up: " + successfulRows + " + " + failedRows + " != " + totalRows);
        }

        // Get the notebook page
        NoteBookPage page = noteBookPageService.get(notebookPageId);
        if (page == null) {
            throw new IllegalArgumentException("Notebook page not found: " + notebookPageId);
        }

        // Get analyzer if provided
        Analyzer analyzer = null;
        if (analyzerId != null) {
            analyzer = analyzerService.get(analyzerId.toString());
        }

        // Get importing user
        SystemUser user = systemUserService.get(userId);

        // Create import record
        AnalyzerResultImport importRecord = new AnalyzerResultImport();
        importRecord.setNotebookPage(page);
        importRecord.setAnalyzer(analyzer);
        importRecord.setFileName(fileName);
        importRecord.setImportDate(new Timestamp(System.currentTimeMillis()));
        importRecord.setImportedBy(user);
        importRecord.setTotalRows(totalRows);
        importRecord.setSuccessfulRows(successfulRows);
        importRecord.setFailedRows(failedRows);
        importRecord.setColumnMapping(columnMapping);
        importRecord.setErrorDetails(errorDetails);
        importRecord.setAssayRunId(assayRunId);
        importRecord.setOperatorId(operatorId);
        importRecord.setMachineParameters(machineParameters);
        importRecord.setReagentLots(reagentLots);
        importRecord.setFileFormat(fileFormat);

        Integer id = insert(importRecord);
        importRecord.setId(id);
        return importRecord;
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalSuccessfulRows(Integer notebookPageId) {
        return baseObjectDAO.getTotalSuccessfulRows(notebookPageId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFailedImports(Integer notebookPageId) {
        return baseObjectDAO.hasFailedImports(notebookPageId);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportSummary getImportSummary(Integer notebookPageId) {
        List<AnalyzerResultImport> imports = getByNotebookPageId(notebookPageId);

        if (imports.isEmpty()) {
            return new ImportSummary(0, 0, 0, 0, null);
        }

        long totalRows = 0;
        long successfulRows = 0;
        long failedRows = 0;
        Timestamp lastImport = null;

        for (AnalyzerResultImport imp : imports) {
            totalRows += imp.getTotalRows();
            successfulRows += imp.getSuccessfulRows();
            failedRows += imp.getFailedRows();
            if (lastImport == null || imp.getImportDate().after(lastImport)) {
                lastImport = imp.getImportDate();
            }
        }

        return new ImportSummary(imports.size(), totalRows, successfulRows, failedRows, lastImport);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerResultImport getImportById(Integer importId) {
        return get(importId);
    }

    // ========== File Parsing Methods (T095, T096) ==========

    @Override
    public FileFormat detectFileFormat(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".csv")) {
            return FileFormat.CSV;
        } else if (lowerName.endsWith(".xlsx")) {
            return FileFormat.XLSX;
        } else if (lowerName.endsWith(".xls")) {
            return FileFormat.XLS;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported file format. Supported formats: CSV, XLSX, XLS. File: " + fileName);
        }
    }

    @Override
    public ParseResult parseAnalyzerFile(InputStream inputStream, String fileName) {
        FileFormat format = detectFileFormat(fileName);
        return switch (format) {
        case CSV -> parseCsvFile(inputStream);
        case XLSX, XLS -> parseExcelFile(inputStream, format);
        };
    }

    @Override
    public ParseResult parseCsvFile(InputStream inputStream) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();
        int rowNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (rowNumber > MAX_ROWS) {
                    parseErrors.add("File exceeds maximum row limit of " + MAX_ROWS);
                    break;
                }

                String[] values = parseCsvLine(line);

                if (isFirstLine) {
                    // First row is header
                    for (String header : values) {
                        headers.add(header.trim());
                    }
                    isFirstLine = false;
                } else {
                    // Data row
                    Map<String, String> rowData = new HashMap<>();
                    for (int i = 0; i < headers.size() && i < values.length; i++) {
                        rowData.put(headers.get(i), values[i].trim());
                    }
                    rows.add(rowData);
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "parseCsvFile", "Error parsing CSV: " + e.getMessage());
            parseErrors.add("Failed to parse CSV file: " + e.getMessage());
        }

        return new ParseResult(headers, rows, FileFormat.CSV, rows.size(), parseErrors);
    }

    /** Parse a single CSV line, handling quoted values with embedded commas. */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    @Override
    public ParseResult parseExcelFile(InputStream inputStream, FileFormat format) {
        List<String> headers = new ArrayList<>();
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> parseErrors = new ArrayList<>();

        try (Workbook workbook = format == FileFormat.XLSX ? new XSSFWorkbook(inputStream)
                : new HSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                parseErrors.add("No sheets found in workbook");
                return new ParseResult(headers, rows, format, 0, parseErrors);
            }

            Iterator<Row> rowIterator = sheet.iterator();
            boolean isFirstRow = true;
            int rowCount = 0;

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowCount++;
                if (rowCount > MAX_ROWS) {
                    parseErrors.add("File exceeds maximum row limit of " + MAX_ROWS);
                    break;
                }

                if (isFirstRow) {
                    // Header row
                    for (Cell cell : row) {
                        headers.add(getCellValueAsString(cell));
                    }
                    isFirstRow = false;
                } else {
                    // Data row
                    Map<String, String> rowData = new HashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        Cell cell = row.getCell(i);
                        rowData.put(headers.get(i), cell != null ? getCellValueAsString(cell) : "");
                    }
                    rows.add(rowData);
                }
            }
        } catch (IOException e) {
            LogEvent.logError(this.getClass().getName(), "parseExcelFile", "Error parsing Excel: " + e.getMessage());
            parseErrors.add("Failed to parse Excel file: " + e.getMessage());
        }

        return new ParseResult(headers, rows, format, rows.size(), parseErrors);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        return switch (cellType) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> {
            double value = cell.getNumericCellValue();
            // Check if it's a whole number
            if (value == Math.floor(value) && !Double.isInfinite(value)) {
                yield String.valueOf((long) value);
            }
            yield String.valueOf(value);
        }
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case BLANK -> "";
        default -> "";
        };
    }

    // ========== Sample Matching Methods (T097, T098) ==========

    @Override
    @Transactional(readOnly = true)
    public ImportPreview previewImport(Integer notebookId, Integer pageId, ParseResult parseResult,
            Map<String, String> columnMapping) {

        List<PreviewRow> previewRows = new ArrayList<>();
        int matchedCount = 0;
        int unmatchedCount = 0;
        List<String> warnings = new ArrayList<>();

        // Get well coordinate and external ID column names from mapping
        String wellCoordColumn = columnMapping.get("wellCoordinate");
        String externalIdColumn = columnMapping.get("externalId");
        String accessionColumn = columnMapping.get("accessionNumber");

        // Build a set to track duplicates
        Map<String, Integer> sampleIdOccurrences = new HashMap<>();

        int rowNumber = 0;
        for (Map<String, String> rowData : parseResult.rows()) {
            rowNumber++;
            String matchedSampleId = null;
            String matchType = null;
            String matchStatus = "UNMATCHED";

            // Try matching by well coordinate first (primary)
            if (wellCoordColumn != null && rowData.containsKey(wellCoordColumn)) {
                String wellCoord = rowData.get(wellCoordColumn);
                if (wellCoord != null && !wellCoord.isBlank()) {
                    matchedSampleId = findSampleByWellCoordinate(notebookId, null, wellCoord.trim());
                    if (matchedSampleId != null) {
                        matchType = "WELL_COORDINATE";
                    }
                }
            }

            // Fallback to external ID
            if (matchedSampleId == null && externalIdColumn != null && rowData.containsKey(externalIdColumn)) {
                String externalId = rowData.get(externalIdColumn);
                if (externalId != null && !externalId.isBlank()) {
                    matchedSampleId = findSampleByExternalId(notebookId, externalId.trim());
                    if (matchedSampleId != null) {
                        matchType = "EXTERNAL_ID";
                    }
                }
            }

            // Check match status
            if (matchedSampleId != null) {
                // Track duplicates but allow them (for replicate readings)
                int occurrences = sampleIdOccurrences.getOrDefault(matchedSampleId, 0) + 1;
                sampleIdOccurrences.put(matchedSampleId, occurrences);
                if (occurrences > 1) {
                    // Mark as DUPLICATE_MATCH - will still be imported as additional reading
                    matchStatus = "DUPLICATE_MATCH";
                    warnings.add("Row " + rowNumber + ": Additional reading for sample " + matchedSampleId
                            + " (replicate #" + occurrences + ")");
                } else {
                    matchStatus = "MATCHED";
                }
                matchedCount++;
            } else {
                unmatchedCount++;
            }

            previewRows.add(new PreviewRow(rowNumber, rowData, matchedSampleId, matchType, matchStatus));
        }

        if (unmatchedCount > 0) {
            warnings.add(unmatchedCount + " rows could not be matched to samples");
        }

        return new ImportPreview(previewRows, matchedCount, unmatchedCount, warnings);
    }

    @Override
    @Transactional(readOnly = true)
    public String findSampleByWellCoordinate(Integer notebookId, Integer boxId, String wellCoordinate) {
        if (wellCoordinate == null || wellCoordinate.isBlank()) {
            return null;
        }

        // Normalize well coordinate (e.g., "a1" -> "A1")
        String normalizedWell = wellCoordinate.trim().toUpperCase();

        // Get all routing records for the notebook and find by well coordinate
        List<SampleRouting> routings = sampleRoutingService.getByNotebookId(notebookId);
        for (SampleRouting routing : routings) {
            if (normalizedWell.equals(routing.getWellCoordinate())) {
                // If boxId is specified, also check box match
                if (boxId != null && routing.getBox() != null && !boxId.equals(routing.getBox().getId())) {
                    continue;
                }
                return String.valueOf(routing.getSampleItemId());
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public String findSampleByExternalId(Integer notebookId, String externalId) {
        if (externalId == null || externalId.isBlank()) {
            return null;
        }

        LogEvent.logInfo(this.getClass().getName(), "findSampleByExternalId",
                "Looking for externalId=" + externalId + " in notebookId=" + notebookId);

        // Get samples linked to this notebook and search by external ID
        List<NotebookPageSample> pageSamples = notebookPageSampleService.getByNotebookId(notebookId);
        LogEvent.logInfo(this.getClass().getName(), "findSampleByExternalId",
                "Found " + pageSamples.size() + " page samples for notebook " + notebookId);

        for (NotebookPageSample nps : pageSamples) {
            try {
                SampleItem sampleItem = sampleItemService.get(nps.getSampleItemId());
                if (sampleItem != null) {
                    LogEvent.logDebug(this.getClass().getName(), "findSampleByExternalId", "Checking sampleItemId="
                            + nps.getSampleItemId() + " externalId=" + sampleItem.getExternalId());
                    if (externalId.equals(sampleItem.getExternalId())) {
                        LogEvent.logInfo(this.getClass().getName(), "findSampleByExternalId",
                                "MATCH FOUND: " + nps.getSampleItemId());
                        return nps.getSampleItemId();
                    }
                }
            } catch (Exception e) {
                LogEvent.logWarn(this.getClass().getName(), "findSampleByExternalId",
                        "Error looking up sample item " + nps.getSampleItemId() + ": " + e.getMessage());
            }
        }
        LogEvent.logInfo(this.getClass().getName(), "findSampleByExternalId",
                "No match found for externalId=" + externalId);
        return null;
    }

    // ========== Import Execution Methods (T099, T101-T104) ==========

    @Override
    @Transactional
    public ImportResult executeImport(Integer notebookPageId, ParseResult parseResult,
            Map<String, String> columnMapping, String assayRunId, String operatorId,
            Map<String, String> machineParameters, List<String> reagentLots, String userId) {

        NoteBookPage page = noteBookPageService.get(notebookPageId);
        if (page == null) {
            throw new IllegalArgumentException("Notebook page not found: " + notebookPageId);
        }
        Integer notebookId = page.getNotebook().getId();

        // Preview first to get matches
        ImportPreview preview = previewImport(notebookId, notebookPageId, parseResult, columnMapping);

        int successfulRows = 0;
        int failedRows = 0;
        List<Map<String, Object>> errors = new ArrayList<>();

        // Get result column from mapping
        String resultColumn = columnMapping.get("result");
        String valueColumn = columnMapping.get("value");

        for (PreviewRow previewRow : preview.rows()) {
            // Process MATCHED and DUPLICATE_MATCH rows (duplicates are treated as replicate
            // readings)
            if (("MATCHED".equals(previewRow.matchStatus()) || "DUPLICATE_MATCH".equals(previewRow.matchStatus()))
                    && previewRow.matchedSampleId() != null) {
                try {
                    // Get the NotebookPageSample for this sample on this page
                    NotebookPageSample nps = notebookPageSampleService
                            .getBySampleItemIdAndPageId(previewRow.matchedSampleId(), notebookPageId);

                    // Auto-create NotebookPageSample if it doesn't exist
                    // This handles cases where samples were routed directly to analysis
                    // without flowing through earlier workflow pages
                    if (nps == null) {
                        LogEvent.logInfo(this.getClass().getName(), "executeImport",
                                "Auto-creating NotebookPageSample for sample " + previewRow.matchedSampleId()
                                        + " on page " + notebookPageId);
                        notebookPageSampleService.createPageSampleForPage(notebookPageId,
                                Integer.parseInt(previewRow.matchedSampleId()), NotebookPageSample.Status.IN_PROGRESS);
                        nps = notebookPageSampleService.getBySampleItemIdAndPageId(previewRow.matchedSampleId(),
                                notebookPageId);
                    }

                    if (nps != null) {
                        // Store the analyzer result in the data JSONB field
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();

                        // Add analyzer result data
                        Map<String, Object> analyzerData = new HashMap<>();
                        analyzerData.put("importDate", new Timestamp(System.currentTimeMillis()).toString());
                        analyzerData.put("assayRunId", assayRunId);
                        analyzerData.put("operatorId", operatorId);
                        analyzerData.put("matchType", previewRow.matchType());
                        analyzerData.put("rowNumber", previewRow.rowNumber());

                        // Copy all mapped data
                        for (Map.Entry<String, String> entry : previewRow.data().entrySet()) {
                            analyzerData.put("raw_" + entry.getKey(), entry.getValue());
                        }

                        // Extract specific result value if mapped
                        if (resultColumn != null && previewRow.data().containsKey(resultColumn)) {
                            analyzerData.put("result", previewRow.data().get(resultColumn));
                        }
                        if (valueColumn != null && previewRow.data().containsKey(valueColumn)) {
                            analyzerData.put("value", previewRow.data().get(valueColumn));
                        }

                        // Store machine parameters and reagent lots
                        if (machineParameters != null && !machineParameters.isEmpty()) {
                            analyzerData.put("machineParameters", machineParameters);
                        }
                        if (reagentLots != null && !reagentLots.isEmpty()) {
                            analyzerData.put("reagentLots", reagentLots);
                        }

                        // Handle replicate readings: store as array if multiple
                        if ("DUPLICATE_MATCH".equals(previewRow.matchStatus())) {
                            // Add to readings array for replicates
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> readings = (List<Map<String, Object>>) data
                                    .computeIfAbsent("analyzerReadings", k -> new ArrayList<>());
                            readings.add(analyzerData);

                            // Also update the main analyzerResult with the latest reading
                            data.put("analyzerResult", analyzerData);
                        } else {
                            // First reading - store directly and init readings array
                            data.put("analyzerResult", analyzerData);
                            List<Map<String, Object>> readings = new ArrayList<>();
                            readings.add(analyzerData);
                            data.put("analyzerReadings", readings);
                        }
                        nps.setData(data);

                        // Update status to IN_PROGRESS if pending
                        if (nps.getStatus() == NotebookPageSample.Status.PENDING) {
                            nps.setStatus(NotebookPageSample.Status.IN_PROGRESS);
                        }

                        notebookPageSampleService.update(nps);
                        successfulRows++;
                    } else {
                        failedRows++;
                        Map<String, Object> error = new HashMap<>();
                        error.put("row", previewRow.rowNumber());
                        error.put("error",
                                "Failed to create NotebookPageSample for sample " + previewRow.matchedSampleId());
                        errors.add(error);
                    }
                } catch (Exception e) {
                    failedRows++;
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", previewRow.rowNumber());
                    error.put("error", "Failed to store result: " + e.getMessage());
                    errors.add(error);
                    LogEvent.logError(this.getClass().getName(), "executeImport",
                            "Error storing result for row " + previewRow.rowNumber() + ": " + e.getMessage());
                }
            } else {
                // Unmatched or duplicate row
                failedRows++;
                Map<String, Object> error = new HashMap<>();
                error.put("row", previewRow.rowNumber());
                error.put("error", "Row not matched: " + previewRow.matchStatus());
                errors.add(error);
            }
        }

        // Record the import for audit trail
        AnalyzerResultImport importRecord = recordImport(notebookPageId, null, parseResult.format().name(),
                parseResult.totalRows(), successfulRows, failedRows, columnMapping, errors, assayRunId, operatorId,
                machineParameters, reagentLots, parseResult.format().name(), userId);

        return new ImportResult(importRecord.getId(), parseResult.totalRows(), successfulRows, failedRows, errors);
    }
}
