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
import org.openelisglobal.notebook.form.AnalyticsManifestImportForm;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.AnalyticsImportResult;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.DataArchiving;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.ParseError;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.ParsedManifest;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.ResultReview;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.SampleRetention;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.TestAssignment;
import org.openelisglobal.notebook.service.AnalyticsWorkflowService.TestExecutionData;
import org.openelisglobal.notebook.service.NotebookEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for Analytics Laboratory workflow operations.
 * Handles all five pages of the Analytics workflow:
 *
 * Page 1: Sample Creation & Full Metadata Capture
 * Page 2: Test Assignment & Preparation
 * Page 3: Analysis / Test Execution
 * Page 4: Result Review, Reporting & Release
 * Page 5: Post-Test Sample & Data Handling
 */
@RestController
@RequestMapping(value = "/rest/notebook/analytics")
public class AnalyticsWorkflowController extends BaseRestController {

    @Autowired
    private AnalyticsWorkflowService analyticsWorkflowService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    // ========================================
    // Page 1: Sample Creation & Full Metadata Capture
    // ========================================

    /**
     * Preview Analytics manifest CSV for a notebook entry.
     * POST /rest/notebook/analytics/entry/{entryId}/samples/preview-manifest
     *
     * @param entryId the notebook entry ID
     * @param file    the CSV file
     * @param form    Analytics column mapping configuration
     * @return parsed rows and validation errors
     */
    @PostMapping(value = "/entry/{entryId}/samples/preview-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> previewManifestForEntry(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") AnalyticsManifestImportForm form) {

        // Verify entry exists
        java.util.Optional<org.openelisglobal.notebook.valueholder.NotebookEntry> optEntry = notebookEntryService
                .getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try (InputStream inputStream = file.getInputStream()) {
            // Parse the CSV
            ParsedManifest parsed = analyticsWorkflowService.parseManifestCsv(inputStream, form);

            // Validate sample types
            List<ParseError> validationErrors = analyticsWorkflowService.validateSampleTypes(parsed);

            // Combine all errors
            List<ParseError> allErrors = new java.util.ArrayList<>(parsed.errors());
            allErrors.addAll(validationErrors);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("entryId", entryId);
            response.put("totalRows", parsed.rows().size());
            response.put("validRows", parsed.rows().size() - allErrors.size());
            response.put("rows", parsed.rows().stream().map(row -> {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("rowNumber", row.rowNumber());
                rowMap.put("sampleIdentifier", row.sampleIdentifier());
                rowMap.put("barcode", row.barcode());
                rowMap.put("sampleCategory", row.sampleCategory());
                rowMap.put("sampleType", row.sampleType());
                rowMap.put("sampleSource", row.sampleSource());
                rowMap.put("requestingUnit", row.requestingUnit());
                rowMap.put("requestedTests", row.requestedTests());
                rowMap.put("studyProjectId", row.studyProjectId());
                rowMap.put("storageCondition", row.storageCondition());
                rowMap.put("receivedDateTime", row.receivedDateTime());
                rowMap.put("receivedBy", row.receivedBy());
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
     * Create Analytics samples from manifest CSV for a notebook entry.
     * POST /rest/notebook/analytics/entry/{entryId}/samples/create-from-manifest
     *
     * @param entryId     the notebook entry ID
     * @param file        the CSV file
     * @param form        Analytics column mapping configuration
     * @param httpRequest for getting user session
     * @return creation result with created sample count and accession numbers
     */
    @PostMapping(value = "/entry/{entryId}/samples/create-from-manifest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSamplesFromManifest(@PathVariable("entryId") Integer entryId,
            @RequestPart("file") MultipartFile file, @RequestPart("mapping") AnalyticsManifestImportForm form,
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
            ParsedManifest parsed = analyticsWorkflowService.parseManifestCsv(inputStream, form);

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
            List<ParseError> validationErrors = analyticsWorkflowService.validateSampleTypes(parsed);
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
            AnalyticsImportResult result = analyticsWorkflowService.createSamplesForEntry(entryId, parsed, sysUserId);

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
     * Create a single Analytics sample.
     * POST /rest/notebook/analytics/entry/{entryId}/samples/create
     */
    @PostMapping(value = "/entry/{entryId}/samples/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createSample(@PathVariable("entryId") Integer entryId,
            @RequestBody Map<String, Object> sampleData, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        try {
            String sampleIdentifier = (String) sampleData.get("sampleIdentifier");
            String barcode = (String) sampleData.get("barcode");
            String sampleCategory = (String) sampleData.get("sampleCategory");
            String sampleType = (String) sampleData.get("sampleType");
            String sampleSource = (String) sampleData.get("sampleSource");
            String requestingUnit = (String) sampleData.get("requestingUnit");
            String requestedTests = (String) sampleData.get("requestedTests");
            String studyProjectId = (String) sampleData.get("studyProjectId");
            String storageCondition = (String) sampleData.get("storageCondition");
            String receivedBy = (String) sampleData.get("receivedBy");

            var sample = analyticsWorkflowService.createSample(entryId, sampleIdentifier, barcode, sampleCategory,
                    sampleType, sampleSource, requestingUnit, requestedTests, studyProjectId, storageCondition,
                    receivedBy, sysUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("id", sample.getId());
            response.put("externalId", sample.getExternalId());
            response.put("sampleType", sample.getTypeOfSample() != null ? sample.getTypeOfSample().getDescription() : null);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========================================
    // Page 2: Test Assignment & Preparation
    // ========================================

    /**
     * Assign tests to samples.
     * POST /rest/notebook/analytics/page/{pageId}/assign-tests
     */
    @PostMapping(value = "/page/{pageId}/assign-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> assignTests(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        String analystRole = (String) requestBody.get("analystRole");
        String analystName = (String) requestBody.get("analystName");
        String assignmentDate = (String) requestBody.get("assignmentDate");
        @SuppressWarnings("unchecked")
        List<String> analyticalMethodology = (List<String>) requestBody.get("analyticalMethodology");

        TestAssignment assignment = new TestAssignment(analystRole, analystName, assignmentDate, analyticalMethodology);

        int updated = analyticsWorkflowService.assignTestsToSamples(pageId, sampleIds, assignment, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCount", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Get test assignment for a sample.
     * GET /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/assignment
     */
    @GetMapping(value = "/page/{pageId}/sample/{sampleId}/assignment", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestAssignment(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId) {

        TestAssignment assignment = analyticsWorkflowService.getTestAssignment(pageId, sampleId);

        if (assignment == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("analystRole", assignment.analystRole());
        response.put("analystName", assignment.analystName());
        response.put("assignmentDate", assignment.assignmentDate());
        response.put("analyticalMethodology", assignment.analyticalMethodology());
        return ResponseEntity.ok(response);
    }

    // ========================================
    // Page 3: Analysis / Test Execution
    // ========================================

    /**
     * Record test execution for a sample.
     * POST /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/execution
     */
    @PostMapping(value = "/page/{pageId}/sample/{sampleId}/execution", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordTestExecution(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId, @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        String testType = (String) requestBody.get("testType");
        String instrumentId = (String) requestBody.get("instrumentId");
        String analystName = (String) requestBody.get("analystName");
        String runDate = (String) requestBody.get("runDate");
        @SuppressWarnings("unchecked")
        Map<String, Object> instrumentParameters = (Map<String, Object>) requestBody.get("instrumentParameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> rawResults = (Map<String, Object>) requestBody.get("rawResults");
        String notes = (String) requestBody.get("notes");

        TestExecutionData executionData = new TestExecutionData(testType, instrumentId, analystName, runDate,
                instrumentParameters, rawResults, notes);

        boolean success = analyticsWorkflowService.recordTestExecution(pageId, sampleId, executionData, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk record test execution for multiple samples.
     * POST /rest/notebook/analytics/page/{pageId}/bulk-execution
     */
    @PostMapping(value = "/page/{pageId}/bulk-execution", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkRecordTestExecution(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        String testType = (String) requestBody.get("testType");
        String instrumentId = (String) requestBody.get("instrumentId");
        String analystName = (String) requestBody.get("analystName");
        String runDate = (String) requestBody.get("runDate");
        @SuppressWarnings("unchecked")
        Map<String, Object> instrumentParameters = (Map<String, Object>) requestBody.get("instrumentParameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> rawResults = (Map<String, Object>) requestBody.get("rawResults");
        String notes = (String) requestBody.get("notes");

        TestExecutionData executionData = new TestExecutionData(testType, instrumentId, analystName, runDate,
                instrumentParameters, rawResults, notes);

        int updated = analyticsWorkflowService.bulkRecordTestExecution(pageId, sampleIds, executionData, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCount", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Get test execution for a sample.
     * GET /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/execution
     */
    @GetMapping(value = "/page/{pageId}/sample/{sampleId}/execution", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTestExecution(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId) {

        TestExecutionData execution = analyticsWorkflowService.getTestExecution(pageId, sampleId);

        if (execution == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("testType", execution.testType());
        response.put("instrumentId", execution.instrumentId());
        response.put("analystName", execution.analystName());
        response.put("runDate", execution.runDate());
        response.put("instrumentParameters", execution.instrumentParameters());
        response.put("rawResults", execution.rawResults());
        response.put("notes", execution.notes());
        return ResponseEntity.ok(response);
    }

    // ========================================
    // Page 4: Result Review, Reporting & Release
    // ========================================

    /**
     * Review results for a sample.
     * POST /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/review
     */
    @PostMapping(value = "/page/{pageId}/sample/{sampleId}/review", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reviewResults(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId, @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        String complianceStatus = (String) requestBody.get("complianceStatus");
        String reviewerName = (String) requestBody.get("reviewerName");
        String reviewDate = (String) requestBody.get("reviewDate");
        String reviewNotes = (String) requestBody.get("reviewNotes");
        @SuppressWarnings("unchecked")
        List<String> releaseRecipients = (List<String>) requestBody.get("releaseRecipients");
        Boolean resultsReleased = (Boolean) requestBody.get("resultsReleased");

        ResultReview review = new ResultReview(complianceStatus, reviewerName, reviewDate, reviewNotes,
                releaseRecipients, resultsReleased != null && resultsReleased);

        boolean success = analyticsWorkflowService.reviewResults(pageId, sampleId, review, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk review results for multiple samples.
     * POST /rest/notebook/analytics/page/{pageId}/bulk-review
     */
    @PostMapping(value = "/page/{pageId}/bulk-review", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkReviewResults(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        String complianceStatus = (String) requestBody.get("complianceStatus");
        String reviewerName = (String) requestBody.get("reviewerName");
        String reviewDate = (String) requestBody.get("reviewDate");
        String reviewNotes = (String) requestBody.get("reviewNotes");
        @SuppressWarnings("unchecked")
        List<String> releaseRecipients = (List<String>) requestBody.get("releaseRecipients");
        Boolean resultsReleased = (Boolean) requestBody.get("resultsReleased");

        ResultReview review = new ResultReview(complianceStatus, reviewerName, reviewDate, reviewNotes,
                releaseRecipients, resultsReleased != null && resultsReleased);

        int updated = analyticsWorkflowService.bulkReviewResults(pageId, sampleIds, review, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCount", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Release results to recipients.
     * POST /rest/notebook/analytics/page/{pageId}/release-results
     */
    @PostMapping(value = "/page/{pageId}/release-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> releaseResults(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) requestBody.get("recipients");

        int released = analyticsWorkflowService.releaseResults(pageId, sampleIds, recipients, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("releasedCount", released);
        return ResponseEntity.ok(response);
    }

    /**
     * Get result review for a sample.
     * GET /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/review
     */
    @GetMapping(value = "/page/{pageId}/sample/{sampleId}/review", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getResultReview(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId) {

        ResultReview review = analyticsWorkflowService.getResultReview(pageId, sampleId);

        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("complianceStatus", review.complianceStatus());
        response.put("reviewerName", review.reviewerName());
        response.put("reviewDate", review.reviewDate());
        response.put("reviewNotes", review.reviewNotes());
        response.put("releaseRecipients", review.releaseRecipients());
        response.put("resultsReleased", review.resultsReleased());
        return ResponseEntity.ok(response);
    }

    // ========================================
    // Page 5: Post-Test Sample & Data Handling
    // ========================================

    /**
     * Set sample retention for a sample.
     * POST /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/retention
     */
    @PostMapping(value = "/page/{pageId}/sample/{sampleId}/retention", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setSampleRetention(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId, @RequestBody Map<String, Object> requestBody,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        String retentionReason = (String) requestBody.get("retentionReason");
        Integer retentionYears = requestBody.get("retentionYears") != null
                ? ((Number) requestBody.get("retentionYears")).intValue()
                : null;
        String biorepositoryTransferStatus = (String) requestBody.get("biorepositoryTransferStatus");
        String transferDate = (String) requestBody.get("transferDate");
        String transferNotes = (String) requestBody.get("transferNotes");

        SampleRetention retention = new SampleRetention(retentionReason, retentionYears, biorepositoryTransferStatus,
                transferDate, transferNotes);

        boolean success = analyticsWorkflowService.setSampleRetention(pageId, sampleId, retention, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk set sample retention for multiple samples.
     * POST /rest/notebook/analytics/page/{pageId}/bulk-retention
     */
    @PostMapping(value = "/page/{pageId}/bulk-retention", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkSetSampleRetention(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        String retentionReason = (String) requestBody.get("retentionReason");
        Integer retentionYears = requestBody.get("retentionYears") != null
                ? ((Number) requestBody.get("retentionYears")).intValue()
                : null;
        String biorepositoryTransferStatus = (String) requestBody.get("biorepositoryTransferStatus");
        String transferDate = (String) requestBody.get("transferDate");
        String transferNotes = (String) requestBody.get("transferNotes");

        SampleRetention retention = new SampleRetention(retentionReason, retentionYears, biorepositoryTransferStatus,
                transferDate, transferNotes);

        int updated = analyticsWorkflowService.bulkSetSampleRetention(pageId, sampleIds, retention, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("updatedCount", updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Archive data for samples.
     * POST /rest/notebook/analytics/page/{pageId}/archive-data
     */
    @PostMapping(value = "/page/{pageId}/archive-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveData(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");
        Boolean rawDataArchived = (Boolean) requestBody.get("rawDataArchived");
        Boolean processedResultsArchived = (Boolean) requestBody.get("processedResultsArchived");
        Boolean metadataExported = (Boolean) requestBody.get("metadataExported");
        String archiveLocation = (String) requestBody.get("archiveLocation");
        String archiveDate = (String) requestBody.get("archiveDate");

        DataArchiving archiving = new DataArchiving(rawDataArchived != null && rawDataArchived,
                processedResultsArchived != null && processedResultsArchived,
                metadataExported != null && metadataExported, archiveLocation, archiveDate);

        int archived = analyticsWorkflowService.archiveData(pageId, sampleIds, archiving, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("archivedCount", archived);
        return ResponseEntity.ok(response);
    }

    /**
     * Finalize sample lifecycle.
     * POST /rest/notebook/analytics/page/{pageId}/finalize-lifecycle
     */
    @PostMapping(value = "/page/{pageId}/finalize-lifecycle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizeSampleLifecycle(@PathVariable("pageId") Integer pageId,
            @RequestBody Map<String, Object> requestBody, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        @SuppressWarnings("unchecked")
        List<Integer> sampleIds = (List<Integer>) requestBody.get("sampleIds");

        int finalized = analyticsWorkflowService.finalizeSampleLifecycle(pageId, sampleIds, sysUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("finalizedCount", finalized);
        return ResponseEntity.ok(response);
    }

    /**
     * Get sample retention for a sample.
     * GET /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/retention
     */
    @GetMapping(value = "/page/{pageId}/sample/{sampleId}/retention", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSampleRetention(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId) {

        SampleRetention retention = analyticsWorkflowService.getSampleRetention(pageId, sampleId);

        if (retention == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("retentionReason", retention.retentionReason());
        response.put("retentionYears", retention.retentionYears());
        response.put("biorepositoryTransferStatus", retention.biorepositoryTransferStatus());
        response.put("transferDate", retention.transferDate());
        response.put("transferNotes", retention.transferNotes());
        return ResponseEntity.ok(response);
    }

    /**
     * Get data archiving for a sample.
     * GET /rest/notebook/analytics/page/{pageId}/sample/{sampleId}/archiving
     */
    @GetMapping(value = "/page/{pageId}/sample/{sampleId}/archiving", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDataArchiving(@PathVariable("pageId") Integer pageId,
            @PathVariable("sampleId") Integer sampleId) {

        DataArchiving archiving = analyticsWorkflowService.getDataArchiving(pageId, sampleId);

        if (archiving == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("rawDataArchived", archiving.rawDataArchived());
        response.put("processedResultsArchived", archiving.processedResultsArchived());
        response.put("metadataExported", archiving.metadataExported());
        response.put("archiveLocation", archiving.archiveLocation());
        response.put("archiveDate", archiving.archiveDate());
        return ResponseEntity.ok(response);
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
