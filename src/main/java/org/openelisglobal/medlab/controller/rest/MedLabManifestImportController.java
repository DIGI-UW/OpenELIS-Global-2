package org.openelisglobal.medlab.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.medlab.form.MedLabManifestImportForm;
import org.openelisglobal.medlab.service.MedLabManifestImportService;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ImportResult;
import org.openelisglobal.medlab.service.MedLabManifestImportService.MedLabManifestRow;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ParseError;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ParsedMedLabManifest;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ValidationResult;
import org.openelisglobal.medlab.service.MedLabManifestImportService.ValidationWarning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for MedLab manifest import operations. Handles CSV manifest
 * upload, preview, and import for the Sample Collection workflow (Stage 2).
 *
 * <p>
 * Two-Step Workflow:
 * <ol>
 * <li>Preview: Upload CSV → Parse → Validate → Return preview with errors</li>
 * <li>Import: Upload CSV → Parse → Create Sample/SampleItem records</li>
 * </ol>
 *
 * <p>
 * Supports 13 manifest fields per spec FR-010 to FR-014:
 * <ul>
 * <li>sampleId, sampleType, containerType, customLabel</li>
 * <li>quantity, unitOfMeasure, collectionSource, collector</li>
 * <li>collectionDate, collectionTime, orderId, patientId, notes</li>
 * </ul>
 */
@RestController
@RequestMapping(value = "/rest/medlab/samples")
public class MedLabManifestImportController extends BaseRestController {

    @Autowired
    private MedLabManifestImportService medLabManifestImportService;

    /**
     * Previews a manifest CSV file without creating any records. Validates all
     * fields and returns a preview of parsed rows with any validation errors.
     *
     * @param file          the CSV file to preview
     * @param columnMapping JSON string containing column mapping configuration
     * @param request       HTTP request for user session
     * @return preview response with parsed rows and validation errors
     */
    @PostMapping(value = "/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifest(@RequestParam("file") MultipartFile file,
            @RequestParam("sampleIdColumn") String sampleIdColumn,
            @RequestParam("sampleTypeColumn") String sampleTypeColumn,
            @RequestParam("containerTypeColumn") String containerTypeColumn,
            @RequestParam("quantityColumn") String quantityColumn,
            @RequestParam("unitOfMeasureColumn") String unitOfMeasureColumn,
            @RequestParam("collectionSourceColumn") String collectionSourceColumn,
            @RequestParam("collectorColumn") String collectorColumn,
            @RequestParam("collectionDateColumn") String collectionDateColumn,
            @RequestParam("collectionTimeColumn") String collectionTimeColumn,
            @RequestParam(value = "customLabelColumn", required = false) String customLabelColumn,
            @RequestParam(value = "orderIdColumn", required = false) String orderIdColumn,
            @RequestParam(value = "patientIdColumn", required = false) String patientIdColumn,
            @RequestParam(value = "notesColumn", required = false) String notesColumn,
            @RequestParam(value = "dateFormat", defaultValue = "yyyy-MM-dd") String dateFormat,
            @RequestParam(value = "timeFormat", defaultValue = "HH:mm") String timeFormat, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            // Build column mapping form
            MedLabManifestImportForm columnMapping = new MedLabManifestImportForm();
            columnMapping.setSampleIdColumn(sampleIdColumn);
            columnMapping.setSampleTypeColumn(sampleTypeColumn);
            columnMapping.setContainerTypeColumn(containerTypeColumn);
            columnMapping.setQuantityColumn(quantityColumn);
            columnMapping.setUnitOfMeasureColumn(unitOfMeasureColumn);
            columnMapping.setCollectionSourceColumn(collectionSourceColumn);
            columnMapping.setCollectorColumn(collectorColumn);
            columnMapping.setCollectionDateColumn(collectionDateColumn);
            columnMapping.setCollectionTimeColumn(collectionTimeColumn);
            columnMapping.setCustomLabelColumn(customLabelColumn);
            columnMapping.setOrderIdColumn(orderIdColumn);
            columnMapping.setPatientIdColumn(patientIdColumn);
            columnMapping.setNotesColumn(notesColumn);
            columnMapping.setDateFormat(dateFormat);
            columnMapping.setTimeFormat(timeFormat);

            // Parse manifest
            InputStream csvInput = file.getInputStream();
            ParsedMedLabManifest manifest = medLabManifestImportService.parseManifestCsv(csvInput, columnMapping);

            // Validate sample types, container types, and duplicate sample IDs
            List<ParseError> allErrors = new ArrayList<>(manifest.errors());
            allErrors.addAll(medLabManifestImportService.validateSampleTypes(manifest));
            allErrors.addAll(medLabManifestImportService.validateContainerTypes(manifest));
            allErrors.addAll(medLabManifestImportService.validateDuplicateSampleIds(manifest));

            // Validate patient and order references (returns warnings, not blocking errors)
            ValidationResult patientOrderValidation = medLabManifestImportService
                    .validatePatientAndOrderReferences(manifest);
            allErrors.addAll(patientOrderValidation.errors());

            // Get row numbers with errors for filtering
            List<Integer> errorRowNumbers = allErrors.stream().map(ParseError::rowNumber).distinct().toList();

            // Separate rows into valid and invalid
            List<Map<String, Object>> validRows = new ArrayList<>();
            List<Map<String, Object>> invalidRows = new ArrayList<>();

            for (MedLabManifestRow row : manifest.rows()) {
                Map<String, Object> rowMap = rowToMap(row);
                if (errorRowNumbers.contains(row.rowNumber())) {
                    // Add error messages to the row
                    List<String> rowErrors = allErrors.stream().filter(e -> e.rowNumber() == row.rowNumber())
                            .map(ParseError::message).toList();
                    rowMap.put("errors", rowErrors);
                    invalidRows.add(rowMap);
                } else {
                    // Check for warnings on this row
                    List<String> rowWarnings = patientOrderValidation.warnings().stream()
                            .filter(w -> w.rowNumber() == row.rowNumber()).map(ValidationWarning::message).toList();
                    if (!rowWarnings.isEmpty()) {
                        rowMap.put("warnings", rowWarnings);
                    }
                    validRows.add(rowMap);
                }
            }

            // Build preview response
            Map<String, Object> response = new HashMap<>();
            response.put("success", allErrors.isEmpty());
            response.put("totalRows", manifest.rows().size());
            response.put("validRows", validRows.size());
            response.put("invalidRows", invalidRows.size());
            response.put("errors", formatErrors(allErrors));
            response.put("warnings", formatWarnings(patientOrderValidation.warnings()));

            // Include all valid rows (for import), and first 10 for preview display
            response.put("allValidRows", validRows); // All valid rows for import
            response.put("previewRows", validRows.subList(0, Math.min(10, validRows.size()))); // First 10 for display
            response.put("invalidPreviewRows", invalidRows);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "previewManifest",
                    "Error previewing manifest: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error previewing manifest: " + e.getMessage()));
        }
    }

    /**
     * Imports samples from validated manifest rows (JSON). Creates Sample and
     * SampleItem records for each provided row. This endpoint accepts pre-validated
     * rows from the frontend after preview, avoiding re-parsing and re-validation.
     *
     * @param importRequest JSON object containing entryId, validRows, and optional
     *                      selectedTests
     * @param request       HTTP request for user session
     * @return import response with created sample counts
     */
    @PostMapping(value = "/import-validated", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importValidatedRows(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> importRequest,
            HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            Integer entryId = (Integer) importRequest.get("entryId");
            Integer pageId = (Integer) importRequest.get("pageId");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> validRowsData = (List<Map<String, Object>>) importRequest.get("validRows");
            @SuppressWarnings("unchecked")
            List<Integer> selectedTests = (List<Integer>) importRequest.get("selectedTests");

            if (entryId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notebook entry ID is required"));
            }

            if (validRowsData == null || validRowsData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No valid rows provided for import"));
            }

            // Reconstruct MedLabManifestRow objects from JSON
            List<MedLabManifestRow> validRows = validRowsData.stream().map(this::mapToManifestRow).toList();

            // Create manifest from validated rows
            ParsedMedLabManifest validManifest = new ParsedMedLabManifest(validRows, List.of());

            // Import samples
            Integer createdBy = Integer.parseInt(sysUserId);
            LogEvent.logInfo(this.getClass().getSimpleName(), "importValidatedRows",
                    "Calling createSamplesForEntry with entryId=" + entryId + ", pageId=" + pageId + ", rowCount="
                            + validRows.size());

            ImportResult result = medLabManifestImportService.createSamplesForEntry(entryId, validManifest, createdBy,
                    selectedTests, pageId);

            LogEvent.logInfo(this.getClass().getSimpleName(), "importValidatedRows",
                    "createSamplesForEntry returned: success=" + result.success() + ", samplesCreated="
                            + result.samplesCreated() + ", analysesCreated=" + result.analysesCreated() + ", errors="
                            + result.errors().size());

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.success());
            response.put("samplesCreated", result.samplesCreated());
            response.put("analysesCreated", result.analysesCreated());

            if (!result.errors().isEmpty()) {
                LogEvent.logError(this.getClass().getSimpleName(), "importValidatedRows",
                        "ImportResult contains errors: " + result.errors());
                response.put("errors", formatErrors(result.errors()));
            }

            if (result.success()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importValidatedRows",
                    "Exception during import: " + e.getClass().getName() + ": " + e.getMessage());

            // Log the full stack trace
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            LogEvent.logError(this.getClass().getSimpleName(), "importValidatedRows", "Stack trace: " + sw.toString());

            // Log root cause if present
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            if (rootCause != e) {
                LogEvent.logError(this.getClass().getSimpleName(), "importValidatedRows",
                        "Root cause: " + rootCause.getClass().getName() + ": " + rootCause.getMessage());
            }

            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error importing validated rows: " + e.getMessage()));
        }
    }

    /**
     * Imports samples from a manifest CSV file. Creates Sample and SampleItem
     * records for each valid row in the manifest.
     *
     * @param file          the CSV file to import
     * @param entryId       the notebook entry ID to link samples to
     * @param columnMapping JSON string containing column mapping configuration
     * @param request       HTTP request for user session
     * @return import response with created sample counts
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importManifest(@RequestParam("file") MultipartFile file,
            @RequestParam("entryId") Integer entryId, @RequestParam("sampleIdColumn") String sampleIdColumn,
            @RequestParam("sampleTypeColumn") String sampleTypeColumn,
            @RequestParam("containerTypeColumn") String containerTypeColumn,
            @RequestParam("quantityColumn") String quantityColumn,
            @RequestParam("unitOfMeasureColumn") String unitOfMeasureColumn,
            @RequestParam("collectionSourceColumn") String collectionSourceColumn,
            @RequestParam("collectorColumn") String collectorColumn,
            @RequestParam("collectionDateColumn") String collectionDateColumn,
            @RequestParam("collectionTimeColumn") String collectionTimeColumn,
            @RequestParam(value = "customLabelColumn", required = false) String customLabelColumn,
            @RequestParam(value = "orderIdColumn", required = false) String orderIdColumn,
            @RequestParam(value = "patientIdColumn", required = false) String patientIdColumn,
            @RequestParam(value = "notesColumn", required = false) String notesColumn,
            @RequestParam(value = "dateFormat", defaultValue = "yyyy-MM-dd") String dateFormat,
            @RequestParam(value = "timeFormat", defaultValue = "HH:mm") String timeFormat,
            @RequestParam(value = "selectedTests", required = false) List<Integer> selectedTests,
            @RequestParam(value = "pageId", required = false) Integer pageId, HttpServletRequest request) {

        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
            }

            if (entryId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Notebook entry ID is required"));
            }

            // Build column mapping form
            MedLabManifestImportForm columnMapping = new MedLabManifestImportForm();
            columnMapping.setSampleIdColumn(sampleIdColumn);
            columnMapping.setSampleTypeColumn(sampleTypeColumn);
            columnMapping.setContainerTypeColumn(containerTypeColumn);
            columnMapping.setQuantityColumn(quantityColumn);
            columnMapping.setUnitOfMeasureColumn(unitOfMeasureColumn);
            columnMapping.setCollectionSourceColumn(collectionSourceColumn);
            columnMapping.setCollectorColumn(collectorColumn);
            columnMapping.setCollectionDateColumn(collectionDateColumn);
            columnMapping.setCollectionTimeColumn(collectionTimeColumn);
            columnMapping.setCustomLabelColumn(customLabelColumn);
            columnMapping.setOrderIdColumn(orderIdColumn);
            columnMapping.setPatientIdColumn(patientIdColumn);
            columnMapping.setNotesColumn(notesColumn);
            columnMapping.setDateFormat(dateFormat);
            columnMapping.setTimeFormat(timeFormat);

            // Parse manifest
            InputStream csvInput = file.getInputStream();
            ParsedMedLabManifest manifest = medLabManifestImportService.parseManifestCsv(csvInput, columnMapping);

            // Validate before importing (including duplicate sample IDs to prevent
            // constraint violations)
            List<ParseError> allErrors = new ArrayList<>(manifest.errors());
            allErrors.addAll(medLabManifestImportService.validateSampleTypes(manifest));
            allErrors.addAll(medLabManifestImportService.validateContainerTypes(manifest));
            allErrors.addAll(medLabManifestImportService.validateDuplicateSampleIds(manifest));

            // Get row numbers with errors for filtering
            List<Integer> errorRowNumbers = allErrors.stream().map(ParseError::rowNumber).distinct().toList();

            // Filter out invalid rows - only import valid ones
            List<MedLabManifestRow> validRows = manifest.rows().stream()
                    .filter(row -> !errorRowNumbers.contains(row.rowNumber())).toList();

            int skippedRows = manifest.rows().size() - validRows.size();

            if (validRows.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "No valid rows to import. Please fix errors and try again.");
                response.put("errors", formatErrors(allErrors));
                response.put("skippedRows", skippedRows);
                return ResponseEntity.badRequest().body(response);
            }

            // Create a new manifest with only valid rows
            ParsedMedLabManifest validManifest = new ParsedMedLabManifest(validRows, List.of());

            // Import samples (only valid rows)
            Integer createdBy = Integer.parseInt(sysUserId);
            ImportResult result = medLabManifestImportService.createSamplesForEntry(entryId, validManifest, createdBy,
                    selectedTests, pageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.success());
            response.put("samplesCreated", result.samplesCreated());
            response.put("analysesCreated", result.analysesCreated());
            response.put("skippedRows", skippedRows);

            if (!result.errors().isEmpty()) {
                response.put("errors", formatErrors(result.errors()));
            }

            // Include validation errors for skipped rows (for informational purposes)
            if (!allErrors.isEmpty()) {
                response.put("validationErrors", formatErrors(allErrors));
            }

            if (result.success()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "importManifest",
                    "Error importing manifest: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Error importing manifest: " + e.getMessage()));
        }
    }

    /**
     * Counts the number of unique rows with errors.
     */
    private int countRowsWithErrors(List<ParseError> errors) {
        return (int) errors.stream().map(ParseError::rowNumber).distinct().count();
    }

    /**
     * Formats errors into a list of maps for JSON response.
     */
    private List<Map<String, Object>> formatErrors(List<ParseError> errors) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (ParseError error : errors) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("rowNumber", error.rowNumber());
            errorMap.put("column", error.column());
            errorMap.put("message", error.message());
            formatted.add(errorMap);
        }
        return formatted;
    }

    /**
     * Formats warnings into a list of maps for JSON response.
     */
    private List<Map<String, Object>> formatWarnings(List<ValidationWarning> warnings) {
        List<Map<String, Object>> formatted = new ArrayList<>();
        for (ValidationWarning warning : warnings) {
            Map<String, Object> warningMap = new HashMap<>();
            warningMap.put("rowNumber", warning.rowNumber());
            warningMap.put("column", warning.column());
            warningMap.put("message", warning.message());
            warningMap.put("type", warning.type().name());
            formatted.add(warningMap);
        }
        return formatted;
    }

    /**
     * Converts a manifest row to a map for JSON response.
     */
    private Map<String, Object> rowToMap(MedLabManifestRow row) {
        Map<String, Object> map = new HashMap<>();
        map.put("rowNumber", row.rowNumber());
        map.put("sampleId", row.sampleId());
        map.put("sampleType", row.sampleType());
        map.put("containerType", row.containerType());
        map.put("customLabel", row.customLabel());
        map.put("quantity", row.quantity());
        map.put("unitOfMeasure", row.unitOfMeasure());
        map.put("collectionSource", row.collectionSource());
        map.put("collector", row.collector());
        map.put("collectionDate", row.collectionDate());
        map.put("collectionTime", row.collectionTime());
        map.put("orderId", row.orderId());
        map.put("patientId", row.patientId());
        map.put("notes", row.notes());
        return map;
    }

    /**
     * Converts a map to a MedLabManifestRow record (reverse of rowToMap).
     */
    private MedLabManifestRow mapToManifestRow(Map<String, Object> map) {
        return new MedLabManifestRow(map.get("rowNumber") != null ? ((Number) map.get("rowNumber")).intValue() : 0,
                (String) map.get("sampleId"), (String) map.get("sampleType"), (String) map.get("containerType"),
                (String) map.get("customLabel"), (String) map.get("quantity"), (String) map.get("unitOfMeasure"),
                (String) map.get("collectionSource"), (String) map.get("collector"), (String) map.get("collectionDate"),
                (String) map.get("collectionTime"), (String) map.get("orderId"), (String) map.get("patientId"),
                (String) map.get("notes"));
    }

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
