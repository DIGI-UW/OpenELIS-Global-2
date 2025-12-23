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
import org.openelisglobal.notebook.form.PathologyManifestImportForm;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.openelisglobal.notebook.service.PathologyManifestImportService;
import org.openelisglobal.notebook.service.PathologyManifestImportService.ParseError;
import org.openelisglobal.notebook.service.PathologyManifestImportService.ParsedManifest;
import org.openelisglobal.notebook.service.PathologyManifestImportService.PathologyManifestImportResult;
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
 * REST controller for Pathology Laboratory manifest CSV import operations.
 * Supports two manifest types:
 *
 * 1. CLINICAL MANIFEST - For clinical diagnostic samples: Required: firstName,
 * patientId, requestingClinician, specimenType, collectionDateTime,
 * clinicalDetails, receivedDateTime, receivedBy Optional: surname,
 * specimenSite, sourceFacility
 *
 * 2. RESEARCH MANIFEST - For research samples: Required: firstName, studyId,
 * piName, participantAnimalId, specimenType, collectionDateTime,
 * ethicalApprovalRef, receivedDateTime, receivedBy Optional: specimenSite,
 * sourceFacility
 *
 * Common fields for ALL samples: - firstName (MANDATORY - primary name field
 * for order acceptance) - specimenType - collectionDateTime - receivedDateTime
 * - receivedBy
 */
@RestController
@RequestMapping(value = "/rest/notebook/pathology")
public class PathologyManifestImportController extends BaseRestController {

    @Autowired
    private PathologyManifestImportService pathologyManifestImportService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    /**
     * Preview Clinical manifest CSV for a notebook entry. POST
     * /rest/notebook/pathology/entry/{entryId}/samples/preview-manifest/clinical
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest/clinical", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewClinicalManifest(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PathologyManifestImportForm form) {
        form.setSampleCategoryColumn(null); // Not from CSV, set directly
        return previewManifest(entryId, file, form, "Clinical diagnostic");
    }

    /**
     * Preview Research manifest CSV for a notebook entry. POST
     * /rest/notebook/pathology/entry/{entryId}/samples/preview-manifest/research
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest/research", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewResearchManifest(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PathologyManifestImportForm form) {
        form.setSampleCategoryColumn(null); // Not from CSV, set directly
        return previewManifest(entryId, file, form, "Research");
    }

    /**
     * Common preview logic for both manifest types.
     */
    private ResponseEntity<Map<String, Object>> previewManifest(Integer entryId, MultipartFile file,
            PathologyManifestImportForm form, String sampleCategory) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV with the specified sample category
            ParsedManifest parsed = pathologyManifestImportService.parseManifestCsv(inputStream, form, sampleCategory);

            // Validate sample types
            List<ParseError> validationErrors = pathologyManifestImportService.validateSampleTypes(parsed);

            // Validate category-specific metadata
            List<ParseError> metadataErrors = pathologyManifestImportService.validateCategoryMetadata(parsed);

            // Combine all errors
            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);
            allErrors.addAll(metadataErrors);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("totalSamples", parsed.rows().size());
            response.put("sampleCategory", sampleCategory);
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("firstName", row.firstName());
                rowMap.put("surname", row.surname());
                rowMap.put("sampleCategory", row.sampleCategory());
                rowMap.put("receivedDateTime", row.receivedDateTime());
                rowMap.put("receivedBy", row.receivedBy());
                rowMap.put("sourceFacility", row.sourceFacility());
                rowMap.put("specimenType", row.specimenType());
                rowMap.put("specimenSite", row.specimenSite());
                rowMap.put("collectionDateTime", row.collectionDateTime());
                rowMap.put("patientId", row.patientId());
                rowMap.put("requestingClinician", row.requestingClinician());
                rowMap.put("clinicalDetails", row.clinicalDetails());
                rowMap.put("studyId", row.studyId());
                rowMap.put("piName", row.piName());
                rowMap.put("participantAnimalId", row.participantAnimalId());
                rowMap.put("ethicalApprovalRef", row.ethicalApprovalRef());
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
     * Create Clinical samples from manifest CSV. POST
     * /rest/notebook/pathology/entry/{entryId}/samples/create-from-manifest/clinical
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest/clinical", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createClinicalSamples(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PathologyManifestImportForm form,
            HttpServletRequest httpRequest) {
        form.setSampleCategoryColumn(null);
        return createSamples(entryId, file, form, "Clinical diagnostic", httpRequest);
    }

    /**
     * Create Research samples from manifest CSV. POST
     * /rest/notebook/pathology/entry/{entryId}/samples/create-from-manifest/research
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest/research", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createResearchSamples(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") PathologyManifestImportForm form,
            HttpServletRequest httpRequest) {
        form.setSampleCategoryColumn(null);
        return createSamples(entryId, file, form, "Research", httpRequest);
    }

    /**
     * Common create logic for both manifest types.
     */
    private ResponseEntity<Map<String, Object>> createSamples(Integer entryId, MultipartFile file,
            PathologyManifestImportForm form, String sampleCategory, HttpServletRequest httpRequest) {

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
            // Parse the CSV with the specified sample category
            ParsedManifest parsed = pathologyManifestImportService.parseManifestCsv(inputStream, form, sampleCategory);

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
            List<ParseError> validationErrors = pathologyManifestImportService.validateSampleTypes(parsed);
            if (!validationErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Specimen type validation errors");
                response.put("errors", validationErrors.stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate category-specific metadata
            List<ParseError> metadataErrors = pathologyManifestImportService.validateCategoryMetadata(parsed);
            if (!metadataErrors.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "Metadata validation errors");
                response.put("errors", metadataErrors.stream().map(error -> {
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("rowNumber", error.rowNumber());
                    errorMap.put("column", error.column());
                    errorMap.put("message", error.message());
                    return errorMap;
                }).collect(Collectors.toList()));
                return ResponseEntity.badRequest().body(response);
            }

            // Create samples for the entry
            PathologyManifestImportResult result = pathologyManifestImportService.createSamplesForEntry(entryId, parsed,
                    sysUserId);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.errors().isEmpty());
            response.put("entryId", entryId);
            response.put("sampleCategory", sampleCategory);
            response.put("totalRequested", result.totalRequested());
            response.put("totalCreated", result.totalCreated());
            response.put("createdAccessionNumbers", result.createdAccessionNumbers());
            response.put("createdSamples", result.createdSamples().stream().map(sample -> {
                Map<String, Object> sampleMap = new HashMap<>();
                sampleMap.put("id", sample.getId());
                sampleMap.put("externalId", sample.getExternalId());
                sampleMap.put("specimenType",
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
     * Get valid Pathology sample types. GET /rest/notebook/pathology/sample-types
     */
    @GetMapping(value = "/sample-types", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidSampleTypes() {
        List<Map<String, String>> sampleTypes = pathologyManifestImportService.getValidPathologySampleTypes();
        Map<String, Object> response = new HashMap<>();
        response.put("sampleTypes", sampleTypes);
        response.put("total", sampleTypes.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Download Clinical manifest CSV template. GET
     * /rest/notebook/pathology/manifest-template/clinical
     */
    @GetMapping(value = "/manifest-template/clinical", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<Resource> downloadClinicalTemplate() {
        return downloadTemplate("pathology-clinical-manifest-template.csv");
    }

    /**
     * Download Research manifest CSV template. GET
     * /rest/notebook/pathology/manifest-template/research
     */
    @GetMapping(value = "/manifest-template/research", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<Resource> downloadResearchTemplate() {
        return downloadTemplate("pathology-research-manifest-template.csv");
    }

    /**
     * Common template download logic.
     */
    private ResponseEntity<Resource> downloadTemplate(String filename) {
        try {
            Resource resource = new ClassPathResource("sample-import-templates/" + filename);
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
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
