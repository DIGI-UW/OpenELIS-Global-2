package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.form.BioanalyticalManifestImportForm;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.BioanalyticalManifestImportResult;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.ParseError;
import org.openelisglobal.notebook.service.BioanalyticalManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Bioanalytical & Bioequivalence Laboratory manifest CSV
 * import operations.
 *
 * Supports reception metadata with: - Required: uniqueSampleId, sampleType,
 * sourceOrigin, requestedTests, dateTimeOfReceipt, receivingPersonnel -
 * Optional: projectStudyAssociation, storageConditionPrior, sampleVolume,
 * transportTemperature, manifestVerificationStatus, notes - Auto-generated:
 * systemAssignedSampleId, barcodeQrCode
 *
 * Endpoints: - GET /sample-types - Get valid sample types - GET /tests - Get
 * valid analytical tests - GET /source-origins - Get valid source origins -
 * POST /entry/{entryId}/samples/preview-manifest - Preview CSV before import -
 * POST /entry/{entryId}/samples/import-manifest - Execute manifest import
 */
@RestController
@RequestMapping("/rest/notebook/bioanalytical")
public class BioanalyticalManifestImportController extends BaseRestController {

    @Autowired
    private BioanalyticalManifestImportService bioanalyticalManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Get valid sample types for the Bioanalytical laboratory with pagination and
     * filtering.
     *
     * @param offset Pagination offset (default 0)
     * @param limit  Pagination limit (default 50, max 200)
     * @param filter Optional filter string (searches in description)
     * @return Paginated list of valid sample types (biological matrices +
     *         pharmaceutical products)
     */
    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidSampleTypes(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "filter", required = false) String filter) {

        // Validate pagination parameters
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Limit must be between 1 and 200");
        }

        List<Map<String, String>> allSampleTypes = bioanalyticalManifestImportService
                .getValidBioanalyticalSampleTypes();

        // Apply filter if provided
        List<Map<String, String>> filtered = allSampleTypes;
        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            filtered = allSampleTypes.stream().filter(st -> st.get("description").toLowerCase().contains(filterLower)
                    || st.get("id").toLowerCase().contains(filterLower)).toList();
        }

        // Apply pagination
        List<Map<String, String>> paginated = filtered.stream().skip(offset).limit(limit).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("sampleTypes", paginated);
        response.put("total", filtered.size());
        response.put("offset", offset);
        response.put("limit", limit);
        response.put("returned", paginated.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get valid analytical tests for the Bioanalytical laboratory with pagination
     * and filtering.
     *
     * @param offset Pagination offset (default 0)
     * @param limit  Pagination limit (default 50, max 200)
     * @param filter Optional filter string (searches in description)
     * @return Paginated list of valid tests (HPLC, LC-MS/MS, dissolution, assay,
     *         etc.)
     */
    @GetMapping(value = "/tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidAnalyticalTests(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "filter", required = false) String filter) {

        // Validate pagination parameters
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Limit must be between 1 and 200");
        }

        List<Map<String, String>> allTests = bioanalyticalManifestImportService.getValidBioanalyticalTests();

        // Apply filter if provided
        List<Map<String, String>> filtered = allTests;
        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            filtered = allTests.stream().filter(t -> t.get("description").toLowerCase().contains(filterLower)
                    || t.get("id").toLowerCase().contains(filterLower)).toList();
        }

        // Apply pagination
        List<Map<String, String>> paginated = filtered.stream().skip(offset).limit(limit).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("tests", paginated);
        response.put("total", filtered.size());
        response.put("offset", offset);
        response.put("limit", limit);
        response.put("returned", paginated.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Get valid source origins for bioanalytical samples with pagination and
     * filtering.
     *
     * @param offset Pagination offset (default 0)
     * @param limit  Pagination limit (default 50, max 200)
     * @param filter Optional filter string (searches in description)
     * @return Paginated list of valid source origins (Medical Lab, External Client,
     *         CRO, etc.)
     */
    @GetMapping(value = "/source-origins", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidSourceOrigins(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @RequestParam(value = "filter", required = false) String filter) {

        // Validate pagination parameters
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Limit must be between 1 and 200");
        }

        List<Map<String, String>> allOrigins = bioanalyticalManifestImportService.getValidSourceOrigins();

        // Apply filter if provided
        List<Map<String, String>> filtered = allOrigins;
        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            filtered = allOrigins.stream().filter(o -> o.get("description").toLowerCase().contains(filterLower)
                    || o.get("id").toLowerCase().contains(filterLower)).toList();
        }

        // Apply pagination
        List<Map<String, String>> paginated = filtered.stream().skip(offset).limit(limit).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("sourceOrigins", paginated);
        response.put("total", filtered.size());
        response.put("offset", offset);
        response.put("limit", limit);
        response.put("returned", paginated.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Preview manifest CSV before importing. Parses and validates CSV data, returns
     * counts and any validation errors.
     *
     * @param entryId The notebook entry ID to link samples to
     * @param file    The CSV file to preview
     * @param form    Column mapping configuration
     * @return Preview result with row counts and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") BioanalyticalManifestImportForm form) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = bioanalyticalManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = bioanalyticalManifestImportService.validateManifest(parsed);

            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamplesToCreate", parsed.rows().size());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                // Required fields
                rowMap.put("uniqueSampleId", row.uniqueSampleId());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("sourceOrigin", row.sourceOrigin());
                rowMap.put("requestedTests", row.requestedTests());
                rowMap.put("dateTimeOfReceipt", row.dateTimeOfReceipt());
                rowMap.put("receivingPersonnel", row.receivingPersonnel());
                // Optional fields
                rowMap.put("projectStudyAssociation", row.projectStudyAssociation());
                rowMap.put("storageConditionPrior", row.storageConditionPrior());
                rowMap.put("sampleVolume", row.sampleVolume());
                rowMap.put("transportTemperature", row.transportTemperature());
                rowMap.put("manifestVerificationStatus", row.manifestVerificationStatus());
                rowMap.put("notes", row.notes());
                return rowMap;
            }).toList());

            if (!allErrors.isEmpty()) {
                response.put("errors", allErrors.stream().map(
                        err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message", err.message()))
                        .toList());
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }
    }

    /**
     * Import manifest CSV and create samples in the notebook entry. This is the
     * actual import endpoint that persists data to database.
     *
     * @param entryId The notebook entry ID
     * @param file    The CSV file to import
     * @param form    Column mapping configuration
     * @param request HTTP request (for user session data)
     * @return Import result with created sample IDs and any errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/import-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") BioanalyticalManifestImportForm form,
            HttpServletRequest request) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            String userId = "SYSTEM";

            ParsedManifest parsed = bioanalyticalManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = bioanalyticalManifestImportService.validateManifest(parsed);

            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            if (!allErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Manifest validation failed");
                errorResponse.put("totalErrors", allErrors.size());
                errorResponse.put("errors", allErrors.stream().map(
                        err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message", err.message()))
                        .toList());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            BioanalyticalManifestImportResult result = bioanalyticalManifestImportService.createSamplesForEntry(entryId,
                    parsed, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdSampleIds", result.createdSampleIds());

            if (!result.errors().isEmpty()) {
                response.put("partialSuccess", true);
                response.put("errors", result.errors().stream().map(
                        err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message", err.message()))
                        .toList());
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to read CSV file: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
