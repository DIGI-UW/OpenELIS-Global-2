package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.notebook.form.BacteriologyManifestImportForm;
import org.openelisglobal.notebook.service.BacteriologyManifestImportService;
import org.openelisglobal.notebook.service.BacteriologyManifestImportService.BacteriologyManifestImportResult;
import org.openelisglobal.notebook.service.BacteriologyManifestImportService.ParseError;
import org.openelisglobal.notebook.service.BacteriologyManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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
 * REST controller for Bacteriology Laboratory manifest CSV import operations.
 * Handles Bacteriology-specific data points and sample creation.
 */
@RestController
@RequestMapping(value = "/rest/notebook/bacteriology")
public class BacteriologyManifestImportController extends BaseRestController {

    @Autowired
    private BacteriologyManifestImportService bacteriologyManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Preview Bacteriology manifest CSV for a notebook entry. POST
     * /rest/notebook/bacteriology/entry/{entryId}/samples/preview-manifest
     *
     * @param entryId the notebook entry ID
     * @param file    the CSV file
     * @param form    Bacteriology column mapping configuration
     * @return parsed rows and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") BacteriologyManifestImportForm form) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV
            ParsedManifest parsed = bacteriologyManifestImportService.parseManifestCsv(inputStream, form);

            // Validate sample types
            List<ParseError> validationErrors = bacteriologyManifestImportService.validateSampleTypes(parsed);

            // Combine all errors
            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamplesToCreate", parsed.rows().size());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("projectName", row.projectName());
                rowMap.put("studyId", row.studyId());
                rowMap.put("participantId", row.participantId());
                rowMap.put("barcode", row.barcode());
                rowMap.put("collectionSite", row.collectionSite());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("collectionDateTime", row.collectionDateTime());
                rowMap.put("sampleReceivedDate", row.sampleReceivedDate());
                rowMap.put("sampleArrivalTime", row.sampleArrivalTime());
                rowMap.put("receivedBy", row.receivedBy());
                rowMap.put("storageContainerType", row.storageContainerType());
                rowMap.put("storageTemperatureOnArrival", row.storageTemperatureOnArrival());
                rowMap.put("consentStatus", row.consentStatus());
                rowMap.put("crfStatus", row.crfStatus());
                rowMap.put("sampleOrigin", row.sampleOrigin());
                rowMap.put("sourceLocationFacility", row.sourceLocationFacility());
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

    /**
     * Create Bacteriology samples from manifest CSV for a notebook entry. POST
     * /rest/notebook/bacteriology/entry/{entryId}/samples/create-from-manifest
     *
     * @param entryId     the notebook entry ID
     * @param file        the CSV file
     * @param form        Bacteriology column mapping configuration
     * @param httpRequest for getting user session
     * @return creation result with created sample count and accession numbers
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") BacteriologyManifestImportForm form,
            HttpServletRequest httpRequest) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
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
            // Parse the CSV
            ParsedManifest parsed = bacteriologyManifestImportService.parseManifestCsv(inputStream, form);

            // Check for parsing errors
            if (!parsed.errors().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "CSV parsing errors");
                response.put("errors", parsed.errors().stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate sample types
            List<ParseError> validationErrors = bacteriologyManifestImportService.validateSampleTypes(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Sample type validation errors");
                response.put("errors", validationErrors.stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Create samples for the entry
            BacteriologyManifestImportResult result = bacteriologyManifestImportService.createSamplesForEntry(entryId,
                    parsed, sysUserId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdAccessionNumbers", result.createdAccessionNumbers());
            response.put("createdSamples", result.createdSamples().stream().map(sample -> {
                Map<String, Object> sampleMap = new HashMap<>();
                sampleMap.put("id", sample.getId());
                sampleMap.put("externalId", sample.getExternalId());
                sampleMap.put("sampleType",
                        sample.getTypeOfSample() != null ? sample.getTypeOfSample().getDescription() : null);
                return sampleMap;
            }).collect(Collectors.toList()));

            if (!result.errors().isEmpty()) {
                response.put("errors", result.errors().stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get valid Bacteriology sample types. GET
     * /rest/notebook/bacteriology/sample-types
     *
     * @return list of valid sample types that are configured in the system
     */
    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidSampleTypes() {
        List<Map<String, String>> sampleTypes = bacteriologyManifestImportService.getValidBacteriologySampleTypes();
        Map<String, Object> response = new HashMap<>();
        response.put("sampleTypes", sampleTypes);
        response.put("total", sampleTypes.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Download Bacteriology manifest CSV template. GET
     * /rest/notebook/bacteriology/manifest-template
     *
     * @return CSV template file for Bacteriology sample import
     */
    @GetMapping(value = "/manifest-template", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<Resource> downloadManifestTemplate() {
        try {
            Resource resource = new ClassPathResource("templates/bacteriology-sample-import-template.csv");
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=bacteriology-sample-import-template.csv");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
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
