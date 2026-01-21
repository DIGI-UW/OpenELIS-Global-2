package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.form.VirologyManifestImportForm;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.VirologyManifestImportService;
import org.openelisglobal.notebook.service.VirologyManifestImportService.ParseError;
import org.openelisglobal.notebook.service.VirologyManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.VirologyManifestImportService.VirologyManifestImportResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Virology & Vaccine Unit manifest CSV import operations.
 * Supports reception metadata with: - Required: sampleId, source, sampleType,
 * receptionDateTime, testType, projectStudyAssociation - Virus/Vaccine
 * Production: batchId, productionStage, cellLineUsed, passageNumber,
 * titerValues, qualityControlResults, formulationDetails - Optional:
 * collectionDateTime, storageConditionOnArrival, transportTemperature,
 * receivingPersonnelName, manifestVerificationStatus, notes - Auto-generated:
 * accessionNumber, barcodeQrCode
 */
@RestController
@RequestMapping("/rest/notebook/virology")
public class VirologyManifestImportController extends BaseRestController {

    @Autowired
    private VirologyManifestImportService virologyManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Get valid sample types for the Virology & Vaccine Unit laboratory. Returns
     * sample types that are both in the valid virology list AND exist in the
     * database.
     */
    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidSampleTypes() {
        List<Map<String, String>> sampleTypes = virologyManifestImportService.getValidVirologySampleTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("sampleTypes", sampleTypes);
        response.put("total", sampleTypes.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") VirologyManifestImportForm form) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService.getMatch("id",
                entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            ParsedManifest parsed = virologyManifestImportService.parseManifestCsv(inputStream, form);
            List<ParseError> validationErrors = virologyManifestImportService.validateManifest(parsed);

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
                // Required fields - Sample Arrival
                rowMap.put("sampleId", row.sampleId());
                rowMap.put("source", row.source());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("receptionDateTime", row.receptionDateTime());
                rowMap.put("testType", row.testType());
                rowMap.put("projectStudyAssociation", row.projectStudyAssociation());
                // Virus/Vaccine Production fields
                rowMap.put("batchId", row.batchId());
                rowMap.put("productionStage", row.productionStage());
                rowMap.put("cellLineUsed", row.cellLineUsed());
                rowMap.put("passageNumber", row.passageNumber());
                rowMap.put("titerValues", row.titerValues());
                rowMap.put("qualityControlResults", row.qualityControlResults());
                rowMap.put("formulationDetails", row.formulationDetails());
                // Optional fields
                rowMap.put("collectionDateTime", row.collectionDateTime());
                rowMap.put("storageConditionOnArrival", row.storageConditionOnArrival());
                rowMap.put("transportTemperature", row.transportTemperature());
                rowMap.put("receivingPersonnelName", row.receivingPersonnelName());
                rowMap.put("manifestVerificationStatus", row.manifestVerificationStatus());
                rowMap.put("notes", row.notes());
                return rowMap;
            }).collect(Collectors.toList()));
            response.put("errors", allErrors.stream().map(error -> {
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("rowNumber", error.rowNumber());
                errorMap.put("column", error.column());
                errorMap.put("message", error.message());
                return errorMap;
            }).collect(Collectors.toList()));
            response.put("valid", allErrors.isEmpty());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") VirologyManifestImportForm form,
            HttpServletRequest httpRequest) {

        Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService.getMatch("id",
                entryId);
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
            ParsedManifest parsed = virologyManifestImportService.parseManifestCsv(inputStream, form);

            if (!parsed.errors().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "CSV parsing errors");
                response.put("errors", parsed.errors());
                return ResponseEntity.badRequest().body(response);
            }

            List<ParseError> validationErrors = virologyManifestImportService.validateManifest(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Manifest validation errors");
                response.put("errors", validationErrors);
                return ResponseEntity.badRequest().body(response);
            }

            VirologyManifestImportResult result = virologyManifestImportService.createSamplesForEntry(entryId, parsed,
                    sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdAccessionNumbers", result.createdAccessionNumbers());
            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors());
            }

            return result.errors().isEmpty() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
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
