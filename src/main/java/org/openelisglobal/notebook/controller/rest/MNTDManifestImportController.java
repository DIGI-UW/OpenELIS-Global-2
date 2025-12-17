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
import org.openelisglobal.notebook.form.MNTDManifestImportForm;
import org.openelisglobal.notebook.service.MNTDManifestImportService;
import org.openelisglobal.notebook.service.MNTDManifestImportService.MNTDManifestImportResult;
import org.openelisglobal.notebook.service.MNTDManifestImportService.ParseError;
import org.openelisglobal.notebook.service.MNTDManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for MNTD (Malaria and Neglected Tropical Disease) Laboratory
 * manifest CSV import operations. Handles MNTD-specific data points and sample
 * creation.
 */
@RestController
@RequestMapping(value = "/rest/notebook/mntd")
public class MNTDManifestImportController extends BaseRestController {

    @Autowired
    private MNTDManifestImportService mntdManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Preview MNTD manifest CSV for a notebook entry. POST
     * /rest/notebook/mntd/entry/{entryId}/samples/preview-manifest
     *
     * @param entryId the notebook entry ID
     * @param file    the CSV file
     * @param form    MNTD column mapping configuration
     * @return parsed rows and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") MNTDManifestImportForm form) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV
            ParsedManifest parsed = mntdManifestImportService.parseManifestCsv(inputStream, form);

            // Validate sample types
            List<ParseError> validationErrors = mntdManifestImportService.validateSampleTypes(parsed);

            // Combine all errors
            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamplesToCreate",
                    parsed.rows().stream().mapToInt(MNTDManifestImportService.MNTDManifestRow::numOfSamples).sum());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("sampleId", row.sampleId());
                rowMap.put("sampleSource", row.sampleSource());
                rowMap.put("projectName", row.projectName());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("collectionSite", row.collectionSite());
                rowMap.put("collectionDateTime", row.collectionDateTime());
                rowMap.put("collectedBy", row.collectedBy());
                rowMap.put("numOfSamples", row.numOfSamples());
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
     * Create MNTD samples from manifest CSV for a notebook entry. POST
     * /rest/notebook/mntd/entry/{entryId}/samples/create-from-manifest
     *
     * @param entryId     the notebook entry ID
     * @param file        the CSV file
     * @param form        MNTD column mapping configuration
     * @param httpRequest for getting user session
     * @return creation result with created sample count and accession numbers
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") MNTDManifestImportForm form,
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
            ParsedManifest parsed = mntdManifestImportService.parseManifestCsv(inputStream, form);

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
            List<ParseError> validationErrors = mntdManifestImportService.validateSampleTypes(parsed);
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
            MNTDManifestImportResult result = mntdManifestImportService.createSamplesForEntry(entryId, parsed,
                    sysUserId);

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

    @Override
    protected String getSysUserId(HttpServletRequest request) {
        UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
        if (usd == null) {
            return null;
        }
        return String.valueOf(usd.getSystemUserId());
    }
}
