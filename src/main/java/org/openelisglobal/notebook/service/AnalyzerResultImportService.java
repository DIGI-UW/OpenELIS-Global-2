package org.openelisglobal.notebook.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.notebook.valueholder.AnalyzerResultImport;

/**
 * Service interface for AnalyzerResultImport operations.
 */
public interface AnalyzerResultImportService extends BaseObjectService<AnalyzerResultImport, Integer> {

    /**
     * Supported file formats for analyzer result import.
     */
    enum FileFormat {
        CSV, XLSX, XLS
    }

    /**
     * Result of parsing an analyzer file - contains raw data and metadata.
     */
    record ParseResult(List<String> headers, List<Map<String, String>> rows, FileFormat format, int totalRows,
            List<String> parseErrors) {
    }

    /**
     * Check if parse result has errors.
     */
    static boolean hasParseErrors(ParseResult result) {
        return result.parseErrors() != null && !result.parseErrors().isEmpty();
    }

    /**
     * Result of previewing import - shows match status for each row.
     */
    record ImportPreview(List<PreviewRow> rows, int matchedCount, int unmatchedCount, List<String> warnings) {
    }

    /**
     * Single row in the import preview with match status.
     */
    record PreviewRow(int rowNumber, Map<String, String> data, String matchedSampleId, String matchType, // WELL_COORDINATE,
                                                                                                         // EXTERNAL_ID,
                                                                                                         // ACCESSION_NUMBER
            String matchStatus) { // MATCHED, UNMATCHED, DUPLICATE
    }

    /**
     * Result of executing an import.
     */
    record ImportResult(int importId, int totalRows, int successfulRows, int failedRows,
            List<Map<String, Object>> errors) {
    }

    /**
     * Check if import was fully successful (no failed rows).
     */
    static boolean isFullySuccessful(ImportResult result) {
        return result.failedRows() == 0;
    }

    /**
     * Get all import records for a notebook page.
     */
    List<AnalyzerResultImport> getByNotebookPageId(Integer notebookPageId);

    /**
     * Get the most recent import for a notebook page.
     */
    AnalyzerResultImport getLatestByNotebookPageId(Integer notebookPageId);

    /**
     * Record a new analyzer result import.
     *
     * @param notebookPageId page where import occurred
     * @param analyzerId     analyzer used (nullable)
     * @param fileName       uploaded file name
     * @param totalRows      total rows in file
     * @param successfulRows rows successfully imported
     * @param failedRows     rows with errors
     * @param columnMapping  user-defined column mappings
     * @param errorDetails   error details per failed row
     * @param userId         user who performed import
     * @return created import record
     */
    AnalyzerResultImport recordImport(Integer notebookPageId, Integer analyzerId, String fileName, int totalRows,
            int successfulRows, int failedRows, Map<String, String> columnMapping,
            List<Map<String, Object>> errorDetails, String userId);

    /**
     * Get total successful rows imported for a notebook page.
     */
    long getTotalSuccessfulRows(Integer notebookPageId);

    /**
     * Check if there are any failed imports for a notebook page.
     */
    boolean hasFailedImports(Integer notebookPageId);

    /**
     * Get import summary for a notebook page.
     */
    ImportSummary getImportSummary(Integer notebookPageId);

    /**
     * Summary record for import statistics.
     */
    record ImportSummary(int importCount, long totalRows, long successfulRows, long failedRows,
            java.sql.Timestamp lastImportDate) {
    }

    /**
     * Calculate overall success rate for import summary.
     */
    static double overallSuccessRate(ImportSummary summary) {
        if (summary.totalRows() == 0)
            return 0.0;
        return summary.successfulRows() * 100.0 / summary.totalRows();
    }

    // ========== File Parsing Methods (T095, T096) ==========

    /**
     * Detect file format from file name extension.
     *
     * @param fileName the file name
     * @return detected file format
     * @throws IllegalArgumentException if format is not supported
     */
    FileFormat detectFileFormat(String fileName);

    /**
     * Parse an analyzer result file (CSV or Excel).
     *
     * @param inputStream the file content
     * @param fileName    the file name (used for format detection)
     * @return parse result containing headers, rows, and any errors
     */
    ParseResult parseAnalyzerFile(InputStream inputStream, String fileName);

    /**
     * Parse a CSV file.
     *
     * @param inputStream the file content
     * @return parse result
     */
    ParseResult parseCsvFile(InputStream inputStream);

    /**
     * Parse an Excel file (XLSX or XLS).
     *
     * @param inputStream the file content
     * @param format      XLSX or XLS
     * @return parse result
     */
    ParseResult parseExcelFile(InputStream inputStream, FileFormat format);

    // ========== Sample Matching Methods (T097, T098) ==========

    /**
     * Preview import by matching rows to samples. Primary matching is by well
     * coordinate (via SampleRouting), fallback is by external_id.
     *
     * @param notebookId    the notebook ID
     * @param pageId        the notebook page ID
     * @param parseResult   the parsed file data
     * @param columnMapping user-defined column mappings (source -> target)
     * @return preview showing match status for each row
     */
    ImportPreview previewImport(Integer notebookId, Integer pageId, ParseResult parseResult,
            Map<String, String> columnMapping);

    /**
     * Find sample by well coordinate via SampleRouting lookup.
     *
     * @param notebookId     the notebook ID
     * @param boxId          the box ID (if known)
     * @param wellCoordinate the well coordinate (e.g., "A1", "H12")
     * @return sample item ID if found, null otherwise
     */
    String findSampleByWellCoordinate(Integer notebookId, Integer boxId, String wellCoordinate);

    /**
     * Find sample by external ID.
     *
     * @param notebookId the notebook ID
     * @param externalId the external ID to search for
     * @return sample item ID if found, null otherwise
     */
    String findSampleByExternalId(Integer notebookId, String externalId);

    /**
     * Find sample by external ID on a specific page.
     *
     * @param pageId   the notebook page ID
     * @param searchId the external ID or accession number to search for
     * @return sample item ID if found, null otherwise
     */
    String findSampleByExternalIdOnPage(Integer pageId, String searchId);

    // ========== Import Execution Methods (T099, T101-T104) ==========

    /**
     * Execute the import, storing results in NotebookPageSample.data JSONB.
     *
     * @param notebookPageId    the notebook page ID
     * @param parseResult       the parsed file data
     * @param columnMapping     user-defined column mappings
     * @param assayRunId        unique identifier for this assay run
     * @param operatorId        operator who performed the assay
     * @param machineParameters machine/instrument parameters
     * @param reagentLots       reagent lot numbers used
     * @param userId            user performing the import
     * @return import result with success/failure counts
     */
    ImportResult executeImport(Integer notebookPageId, ParseResult parseResult, Map<String, String> columnMapping,
            String assayRunId, String operatorId, Map<String, String> machineParameters, List<String> reagentLots,
            String userId);

    /**
     * Extended recordImport that includes assay metadata.
     */
    AnalyzerResultImport recordImport(Integer notebookPageId, Integer analyzerId, String fileName, int totalRows,
            int successfulRows, int failedRows, Map<String, String> columnMapping,
            List<Map<String, Object>> errorDetails, String assayRunId, String operatorId,
            Map<String, String> machineParameters, List<String> reagentLots, String fileFormat, String userId);

    /**
     * Get import record by ID.
     *
     * @param importId the import record ID
     * @return the import record or null if not found
     */
    AnalyzerResultImport getImportById(Integer importId);
}
