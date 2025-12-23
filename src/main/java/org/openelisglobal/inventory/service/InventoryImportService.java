package org.openelisglobal.inventory.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Service for importing inventory data from Excel/CSV files. Handles parsing,
 * validation, preview, and import execution.
 */
public interface InventoryImportService {

    /**
     * Supported file formats for import
     */
    enum FileFormat {
        CSV, XLSX, XLS
    }

    /**
     * Result of parsing a file
     */
    record ParseResult(List<String> sheetNames, Map<String, List<String>> headersBySheet,
            Map<String, List<Map<String, String>>> rowsBySheet, FileFormat format, int totalRows,
            List<String> parseErrors) {
    }

    /**
     * A single row in the import preview
     */
    record ImportPreviewRow(int rowNumber, String sheetName, Map<String, String> data, Map<String, String> mappedData,
            String itemName, String lotNumber, String status, List<String> validationErrors,
            List<String> validationWarnings, boolean isNewItem, boolean isNewLot) {
    }

    /**
     * Preview result before import
     */
    record ImportPreview(List<ImportPreviewRow> rows, int totalRows, int validRows, int invalidRows, int newItems,
            int existingItems, int newLots, List<String> warnings, Map<String, String> suggestedColumnMapping) {
    }

    /**
     * Result of import execution
     */
    record ImportResult(int totalRows, int successfulItems, int successfulLots, int failedRows,
            List<Map<String, Object>> errors, List<Map<String, Object>> createdItems,
            List<Map<String, Object>> createdLots) {
    }

    /**
     * Column mapping configuration for import
     */
    record ColumnMapping(String itemName, String lotNumber, String quantity, String unit, String expirationDate,
            String manufacturingDate, String openDate, String storageCondition, String storageLocation,
            String concentration, String experimentType, String manufacturer, String catalogNumber, String remarks) {
    }

    /**
     * Detect file format from filename
     */
    FileFormat detectFileFormat(String fileName);

    /**
     * Parse an Excel or CSV file and return structured data
     */
    ParseResult parseFile(InputStream inputStream, String fileName);

    /**
     * Generate suggested column mappings based on header names
     */
    Map<String, String> suggestColumnMapping(List<String> headers);

    /**
     * Preview import without actually importing
     */
    ImportPreview previewImport(ParseResult parseResult, String selectedSheet, ColumnMapping columnMapping);

    /**
     * Execute the import
     *
     * @param parseResult     The parsed file result
     * @param selectedSheet   Comma-separated list of sheets to import
     * @param columnMapping   Column mapping configuration
     * @param userId          User ID performing the import
     * @param skipInvalidRows If true, skip invalid rows and continue. If false,
     *                        stop on first invalid row.
     */
    ImportResult executeImport(ParseResult parseResult, String selectedSheet, ColumnMapping columnMapping,
            String userId, boolean skipInvalidRows);
}
