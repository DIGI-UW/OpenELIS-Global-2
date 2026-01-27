package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.form.GBDManifestImportForm;
import org.openelisglobal.notebook.service.GBDManifestImportService;
import org.openelisglobal.notebook.service.GBDManifestImportService.GBDManifestImportResult;
import org.openelisglobal.notebook.service.GBDManifestImportService.ParseError;
import org.openelisglobal.notebook.service.GBDManifestImportService.ParsedManifest;
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
 * REST controller for Genomic Bioanalytical Database (GBD) manifest CSV import
 * operations.
 *
 * Supports genomic sample metadata including:
 * - Required: sampleId, sampleType, source, collectionDate, receptionDateTime
 * - Optional reception metadata: projectStudyAssociation, volumeConcentration,
 *   A260/280, A260/230, RIN
 * - Optional processing metadata: extractionMethodKit, pcrProtocol,
 *   libraryPrepProtocol, sequencingPlatform, runId, operator, processingDateTime,
 *   notes
 * - Auto-generated: systemAssignedSampleId, barcodeQrCode
 *
 * Endpoints:
 * - GET /sample-types - Get valid sample types
 * - POST /entry/{entryId}/samples/preview-manifest - Preview CSV before import
 * - POST /entry/{entryId}/samples/import-manifest - Execute manifest import
 */
@RestController
@RequestMapping("/rest/notebook/gbd")
public class GBDManifestImportController extends BaseRestController {

    @Autowired
    private GBDManifestImportService gbdManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Get valid sample types for the GBD laboratory with pagination and filtering.
     *
     * @param offset Pagination offset (default 0)
     * @param limit  Pagination limit (default 50, max 200)
     * @param filter Optional filter string (searches in description)
     * @return Paginated list of valid sample types (DNA, RNA, cDNA, genomic
     *         materials)
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

        List<Map<String, String>> allSampleTypes = gbdManifestImportService.getValidGBDSampleTypes();

        List<Map<String, String>> filtered = allSampleTypes;
        if (filter != null && !filter.isBlank()) {
            String filterLower = filter.toLowerCase();
            filtered = allSampleTypes.stream()
                    .filter(st -> st.get("description").toLowerCase().contains(filterLower)
                            || st.get("id").toLowerCase().contains(filterLower))
                    .toList();
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
     * Preview manifest CSV before importing. Parses and validates CSV data, returns
     * counts and any validation errors without persisting to database.
     *
     * @param entryId The notebook entry ID to link samples to
     * @param file    The CSV file to preview
     * @param form    Column mapping configuration
     * @return Preview result with row counts, row data, and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") GBDManifestImportForm form) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = gbdManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = gbdManifestImportService.validateManifest(parsed);

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
                rowMap.put("sampleId", row.sampleId());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("source", row.source());
                rowMap.put("collectionDate", row.collectionDate());
                rowMap.put("receptionDateTime", row.receptionDateTime());
                rowMap.put("projectStudyAssociation", row.projectStudyAssociation());
                rowMap.put("volumeConcentration", row.volumeConcentration());
                rowMap.put("a260_280", row.a260_280());
                rowMap.put("a260_230", row.a260_230());
                rowMap.put("rin", row.rin());
                rowMap.put("extractionMethodKit", row.extractionMethodKit());
                rowMap.put("pcrProtocol", row.pcrProtocol());
                rowMap.put("libraryPrepProtocol", row.libraryPrepProtocol());
                rowMap.put("sequencingPlatform", row.sequencingPlatform());
                rowMap.put("runId", row.runId());
                rowMap.put("operator", row.operator());
                rowMap.put("processingDateTime", row.processingDateTime());
                rowMap.put("notes", row.notes());
                return rowMap;
            }).toList());

            if (!allErrors.isEmpty()) {
                response.put("errors", allErrors.stream()
                        .map(err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message",
                                err.message()))
                        .toList());
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }
    }

    /**
     * Import manifest CSV and create samples in the notebook entry. This is the
     * actual import endpoint that persists data to the database.
     *
     * @param entryId     The notebook entry ID
     * @param file        The CSV file to import
     * @param form        Column mapping configuration
     * @param httpRequest HTTP request (for user session data)
     * @return Import result with created sample IDs and any errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/import-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> importManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") GBDManifestImportForm form,
            HttpServletRequest httpRequest) {

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = gbdManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = gbdManifestImportService.validateManifest(parsed);

            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            if (!allErrors.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Manifest validation failed");
                errorResponse.put("totalErrors", allErrors.size());
                errorResponse.put("errors", allErrors.stream()
                        .map(err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message",
                                err.message()))
                        .toList());
                return ResponseEntity.badRequest().body(errorResponse);
            }

            GBDManifestImportResult result = gbdManifestImportService.createSamplesForEntry(entryId, parsed,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdSampleIds", result.createdSampleIds());

            if (!result.errors().isEmpty()) {
                response.put("partialSuccess", true);
                response.put("errors", result.errors().stream()
                        .map(err -> Map.of("rowNumber", err.rowNumber(), "column", err.column(), "message",
                                err.message()))
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

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
