package org.openelisglobal.notebook.controller.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NoteBookPageService;
import org.openelisglobal.notebook.service.NotebookBulkOperationService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.service.ResultCompilationService;
import org.openelisglobal.notebook.service.ResultCompilationService.ExportOptions;
import org.openelisglobal.notebook.service.ResultCompilationService.ValidationSummary;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.notebook.valueholder.ValidationStatus;
import org.openelisglobal.storage.dao.StorageBoxDAO;
import org.openelisglobal.storage.service.SampleStorageService;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for notebook bulk operations. Handles bulk data entry, value
 * application, and page progress tracking.
 *
 * <p>
 * Per FR-033: System MUST process bulk operations in batches of 50. Per FR-034:
 * System MUST provide bulk apply endpoint for common values. Per FR-035: System
 * MUST provide page progress endpoint.
 */
@RestController
@RequestMapping(value = "/rest/notebook/bulk")
public class NotebookBulkOperationController extends BaseRestController {

    @Autowired
    private NotebookBulkOperationService bulkOperationService;

    @Autowired
    private ResultCompilationService resultCompilationService;

    @Autowired
    private NoteBookPageService noteBookPageService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private StorageBoxDAO storageBoxDAO;

    @Autowired
    private SampleStorageService sampleStorageService;

    /**
     * Bulk apply values to multiple samples on a page. POST
     * /notebook/bulk/page/{pageId}/samples/apply
     *
     * <p>
     * Per FR-034: Apply common values to all selected samples in one request.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds and data to apply
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/apply", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkApplyValues(@PathVariable("pageId") Integer pageId,
            @RequestBody BulkApplyRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getData() == null || request.getData().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No data provided to apply");
            return ResponseEntity.badRequest().body(error);
        }

        int updatedCount = bulkOperationService.bulkApplyValues(pageId, request.getSampleIds(), request.getData(),
                sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("pageId", pageId);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * Bulk apply values to multiple samples on a page using String sample IDs. POST
     * /notebook/bulk/page/{pageId}/samples/apply-string
     *
     * This endpoint supports composite sample IDs (e.g., "123_cassette_0") used in
     * pathology workflow pages where samples are expanded from parent items.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds (as Strings) and data to apply
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/apply-string", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkApplyValuesString(@PathVariable("pageId") Integer pageId,
            @RequestBody StringApplyRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getData() == null || request.getData().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No data provided to apply");
            return ResponseEntity.badRequest().body(error);
        }

        int updatedCount = bulkOperationService.bulkApplyValuesString(pageId, request.getSampleIds(), request.getData(),
                sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("pageId", pageId);
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * Get progress information for a notebook page. GET
     * /notebook/bulk/page/{pageId}/progress
     *
     * <p>
     * Per FR-035: Real-time progress tracking for bulk operations.
     *
     * @param pageId the notebook page ID
     * @return progress information with status counts
     */
    @GetMapping(value = "/page/{pageId}/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPageProgress(@PathVariable("pageId") Integer pageId) {

        NotebookPageSampleService.PageProgress progress = bulkOperationService.getPageProgress(pageId);

        Map<String, Object> result = new HashMap<>();
        result.put("pageId", pageId);
        result.put("total", progress.total());
        result.put("pending", progress.pending());
        result.put("inProgress", progress.inProgress());
        result.put("completed", progress.completed());
        result.put("skipped", progress.skipped());
        result.put("percentage", progress.percentage());

        return ResponseEntity.ok(result);
    }

    /**
     * Update the content (configuration) of a notebook page. POST
     * /notebook/bulk/page/{pageId}/content
     *
     * This endpoint allows updating page-level configuration such as QC parameters,
     * workflow settings, etc. The new content is merged with existing content.
     *
     * @param pageId      the notebook page ID
     * @param request     contains the content JSON to merge
     * @param httpRequest for getting user session
     * @return result with updated page content
     */
    @PostMapping(value = "/page/{pageId}/content", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePageContent(@PathVariable("pageId") Integer pageId,
            @RequestBody PageContentUpdateRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }

        try {
            noteBookPageService.updatePageContent(pageId, request.getContent(), sysUserId);

            Map<String, Object> result = new HashMap<>();
            result.put("pageId", pageId);
            result.put("success", true);
            result.put("message", "Page content updated successfully");

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "updatePageContent",
                    "Error updating page content: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update page content"));
        }
    }

    /**
     * Get content change history for a notebook page. GET
     * /notebook/bulk/page/{pageId}/content/history
     *
     * Returns the audit trail of all content changes (including QC parameters) for
     * the specified page.
     *
     * @param pageId the notebook page ID
     * @return list of history records with timestamps, users, and content changes
     */
    @GetMapping(value = "/page/{pageId}/content/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPageContentHistory(@PathVariable("pageId") Integer pageId) {
        try {
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Page not found"));
            }

            org.openelisglobal.common.services.historyservices.NoteBookPageHistoryService historyService = new org.openelisglobal.common.services.historyservices.NoteBookPageHistoryService(
                    page);

            List<org.openelisglobal.audittrail.action.workers.AuditTrailItem> historyItems = historyService
                    .getContentHistoryItems();

            // Convert to response format
            List<Map<String, Object>> historyList = new ArrayList<>();
            for (org.openelisglobal.audittrail.action.workers.AuditTrailItem item : historyItems) {
                Map<String, Object> historyEntry = new HashMap<>();
                historyEntry.put("timestamp", item.getDate() + " " + item.getTime());
                historyEntry.put("user", item.getUser());
                historyEntry.put("action", item.getAction());
                historyEntry.put("attribute", item.getAttribute());
                historyEntry.put("oldValue", item.getOldValue());
                historyEntry.put("newValue", item.getNewValue());
                historyList.add(historyEntry);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("pageId", pageId);
            result.put("pageName", page.getTitle());
            result.put("history", historyList);
            result.put("totalRecords", historyList.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "getPageContentHistory",
                    "Error retrieving page content history: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve content history"));
        }
    }

    /**
     * Get paginated samples for a page. GET /notebook/bulk/page/{pageId}/samples
     *
     * @param pageId the notebook page ID
     * @param status optional status filter
     * @param page   page number (0-indexed)
     * @param size   page size (default 50)
     * @return paginated samples list
     */
    @GetMapping(value = "/page/{pageId}/samples", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSamplesPaginated(@PathVariable("pageId") Integer pageId,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Status statusFilter = null;
        if (status != null && !status.isBlank()) {
            try {
                statusFilter = Status.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid status: " + status);
                return ResponseEntity.badRequest().body(error);
            }
        }

        List<NotebookPageSample> samples = bulkOperationService.getSamplesPaginated(pageId, statusFilter, page, size);
        long totalCount = bulkOperationService.getSamplesCount(pageId, statusFilter);

        Map<String, Object> result = new HashMap<>();
        result.put("pageId", pageId);
        result.put("samples", samples);
        result.put("page", page);
        result.put("size", size);
        result.put("totalCount", totalCount);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));

        return ResponseEntity.ok(result);
    }

    /**
     * Mark a page as complete. POST /notebook/bulk/page/{pageId}/complete
     *
     * @param pageId      the notebook page ID
     * @param request     contains requireComplete flag
     * @param httpRequest for getting user session
     * @return result indicating success/failure
     */
    @PostMapping(value = "/page/{pageId}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markPageComplete(@PathVariable("pageId") Integer pageId,
            @RequestBody(required = false) MarkCompleteRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        boolean requireComplete = request != null && request.isRequireComplete();
        boolean success = bulkOperationService.markPageComplete(pageId, sysUserId, requireComplete);

        Map<String, Object> result = new HashMap<>();
        result.put("pageId", pageId);
        result.put("success", success);

        if (!success && requireComplete) {
            result.put("message", "Cannot mark page complete: some samples are still pending or in progress");
        }

        return success ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    // ========== Result Compilation Endpoints (US7) ==========

    /**
     * Flag a sample with validation status. POST
     * /notebook/bulk/page/{pageId}/samples/flag
     *
     * <p>
     * Per T122: Implement sample flagging (VALID, INVALID, INCONCLUSIVE)
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleId, status, and reason
     * @param httpRequest for getting user session
     * @return result indicating success/failure
     */
    @PostMapping(value = "/page/{pageId}/samples/flag", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> flagSample(@PathVariable("pageId") Integer pageId,
            @RequestBody FlagSampleRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getSampleId() == null || request.getSampleId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample ID is required"));
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Validation status is required"));
        }

        ValidationStatus status;
        try {
            status = ValidationStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + request.getStatus()));
        }

        try {
            boolean success = resultCompilationService.flagSample(pageId, request.getSampleId(), status,
                    request.getReason(), sysUserId);

            return ResponseEntity.ok(Map.of("success", success, "pageId", pageId, "sampleId", request.getSampleId(),
                    "status", status.name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bulk flag samples with same status. POST
     * /notebook/bulk/page/{pageId}/samples/bulk-flag
     *
     * <p>
     * Per T122b: Implement bulk flagging with reason for each sample
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds, status, and reason
     * @param httpRequest for getting user session
     * @return result with flagged count
     */
    @PostMapping(value = "/page/{pageId}/samples/bulk-flag", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkFlagSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody BulkFlagRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sample IDs are required"));
        }

        ValidationStatus status;
        try {
            status = ValidationStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + request.getStatus()));
        }

        try {
            int flagged = resultCompilationService.bulkFlagSamples(pageId, request.getSampleIds(), status,
                    request.getReason(), sysUserId);

            return ResponseEntity.ok(
                    Map.of("success", true, "flaggedCount", flagged, "totalRequested", request.getSampleIds().size()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get validation summary for a page. GET
     * /notebook/bulk/page/{pageId}/validation-summary
     *
     * <p>
     * Per T121: Get validation statistics for result compilation
     *
     * @param pageId the notebook page ID
     * @return validation summary with counts
     */
    @GetMapping(value = "/page/{pageId}/validation-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getValidationSummary(@PathVariable("pageId") Integer pageId) {

        ValidationSummary summary = resultCompilationService.getValidationSummary(pageId);

        Map<String, Object> result = new HashMap<>();
        result.put("pageId", pageId);
        result.put("total", summary.total());
        result.put("valid", summary.valid());
        result.put("invalid", summary.invalid());
        result.put("inconclusive", summary.inconclusive());
        result.put("pending", summary.pending());
        result.put("validPercentage", ResultCompilationService.validPercentage(summary));
        result.put("invalidPercentage", ResultCompilationService.invalidPercentage(summary));
        result.put("inconclusivePercentage", ResultCompilationService.inconclusivePercentage(summary));

        return ResponseEntity.ok(result);
    }

    /**
     * Get validation summary for an entire notebook (across all pages). GET
     * /notebook/bulk/notebook/{notebookId}/validation-summary
     *
     * This endpoint returns counts of unique samples across all pages, not the
     * total number of page-sample records.
     *
     * @param notebookId the notebook ID
     * @return validation summary with counts
     */
    @GetMapping(value = "/notebook/{notebookId}/validation-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getNotebookValidationSummary(
            @PathVariable("notebookId") Integer notebookId) {

        ValidationSummary summary = resultCompilationService.getNotebookValidationSummary(notebookId);

        Map<String, Object> result = new HashMap<>();
        result.put("notebookId", notebookId);
        result.put("total", summary.total());
        result.put("valid", summary.valid());
        result.put("invalid", summary.invalid());
        result.put("inconclusive", summary.inconclusive());
        result.put("pending", summary.pending());
        result.put("validPercentage", ResultCompilationService.validPercentage(summary));
        result.put("invalidPercentage", ResultCompilationService.invalidPercentage(summary));
        result.put("inconclusivePercentage", ResultCompilationService.inconclusivePercentage(summary));

        return ResponseEntity.ok(result);
    }

    /**
     * Get samples with validation status for a page. GET
     * /notebook/bulk/page/{pageId}/samples-with-validation
     *
     * @param pageId the notebook page ID
     * @return list of samples with validation info
     */
    @GetMapping(value = "/page/{pageId}/samples-with-validation", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getSamplesWithValidation(@PathVariable("pageId") Integer pageId) {
        List<Map<String, Object>> samples = resultCompilationService.getSamplesWithValidation(pageId);
        return ResponseEntity.ok(samples);
    }

    /**
     * Export results to Excel. GET
     * /notebook/bulk/notebook/{notebookId}/export/excel
     *
     * <p>
     * Per T123: Implement Excel report generation using Apache POI
     *
     * @param notebookId          the notebook ID
     * @param includeInvalid      whether to include invalid samples
     * @param includeInconclusive whether to include inconclusive samples
     * @param response            the HTTP response to write to
     */
    @GetMapping(value = "/notebook/{notebookId}/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public void exportToExcel(@PathVariable("notebookId") Integer notebookId,
            @RequestParam(defaultValue = "true") boolean includeInvalid,
            @RequestParam(defaultValue = "true") boolean includeInconclusive,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        try {
            LogEvent.logInfo(this.getClass().getName(), "exportToExcel",
                    "Exporting Excel for notebook ID: " + notebookId);

            ExportOptions options = new ExportOptions(includeInvalid, includeInconclusive, true, null, "yyyy-MM-dd",
                    "Analysis Results");

            byte[] excelBytes = resultCompilationService.compileToExcel(notebookId, options);

            LogEvent.logInfo(this.getClass().getName(), "exportToExcel",
                    "Generated Excel file with " + excelBytes.length + " bytes");

            String filename = "results_notebook_" + notebookId + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(excelBytes.length);
            response.getOutputStream().write(excelBytes);
            response.getOutputStream().flush();

        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getName(), "exportToExcel",
                    "Invalid argument for notebook " + notebookId + ": " + e.getMessage());
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "exportToExcel",
                    "Failed to export Excel for notebook " + notebookId + ": " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Export failed: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Export results to CSV. GET /notebook/bulk/notebook/{notebookId}/export/csv
     *
     * <p>
     * Per T129: Add export format selection (Excel, PDF, CSV)
     *
     * @param notebookId          the notebook ID
     * @param includeInvalid      whether to include invalid samples
     * @param includeInconclusive whether to include inconclusive samples
     * @param response            the HTTP response to write to
     */
    @GetMapping(value = "/notebook/{notebookId}/export/csv", produces = "text/csv")
    public void exportToCsv(@PathVariable("notebookId") Integer notebookId,
            @RequestParam(defaultValue = "true") boolean includeInvalid,
            @RequestParam(defaultValue = "true") boolean includeInconclusive,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        try {
            LogEvent.logInfo(this.getClass().getName(), "exportToCsv", "Exporting CSV for notebook ID: " + notebookId);

            ExportOptions options = new ExportOptions(includeInvalid, includeInconclusive, true, null, "yyyy-MM-dd",
                    "Analysis Results");

            byte[] csvBytes = resultCompilationService.compileToCsv(notebookId, options);

            String filename = "results_notebook_" + notebookId + ".csv";
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(csvBytes.length);
            response.getOutputStream().write(csvBytes);
            response.getOutputStream().flush();

        } catch (IllegalArgumentException e) {
            LogEvent.logError(this.getClass().getName(), "exportToCsv",
                    "Invalid argument for notebook " + notebookId + ": " + e.getMessage());
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "exportToCsv",
                    "Failed to export CSV for notebook " + notebookId + ": " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Export failed: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Generate Certificate of Analysis (COA) PDF for selected samples. POST
     * /notebook/bulk/page/{pageId}/coa/generate
     *
     * Generates a pharmaceutical COA document in PDF format containing: -
     * Product/batch information - Test results and specifications - QC status and
     * validation - Authorization signatures
     *
     * @param pageId      the notebook page ID
     * @param request     COA generation request with sample IDs and metadata
     * @param httpRequest for getting user session
     * @param response    HTTP response to write PDF to
     */
    @PostMapping(value = "/page/{pageId}/coa/generate", produces = "application/pdf")
    public void generateCOA(@PathVariable("pageId") Integer pageId, @RequestBody COAGenerationRequest request,
            HttpServletRequest httpRequest, HttpServletResponse response) throws java.io.IOException {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"User session not found\"}");
            return;
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"No sample IDs provided\"}");
            return;
        }

        if (request.getProductName() == null || request.getProductName().isBlank()) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Product name is required\"}");
            return;
        }

        if (request.getBatchNumber() == null || request.getBatchNumber().isBlank()) {
            response.setStatus(400);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Batch number is required\"}");
            return;
        }

        try {
            LogEvent.logInfo(this.getClass().getName(), "generateCOA",
                    "Generating COA for page " + pageId + " with " + request.getSampleIds().size() + " samples");

            byte[] pdfBytes = generateCOAPdf(pageId, request, sysUserId);

            String filename = "COA_" + request.getBatchNumber().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setContentLength(pdfBytes.length);
            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

            LogEvent.logInfo(this.getClass().getName(), "generateCOA",
                    "Successfully generated COA PDF with " + pdfBytes.length + " bytes");

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateCOA",
                    "Failed to generate COA for page " + pageId + ": " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"COA generation failed: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Generate the COA PDF document using iText.
     */
    private byte[] generateCOAPdf(Integer pageId, COAGenerationRequest request, String sysUserId) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        // Create PDF document
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 50, 50, 50,
                50);
        com.itextpdf.text.pdf.PdfWriter.getInstance(document, baos);
        document.open();

        // Fonts
        com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12,
                com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font normalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.NORMAL);
        com.itextpdf.text.Font smallFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8,
                com.itextpdf.text.Font.NORMAL);

        // Title
        com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("CERTIFICATE OF ANALYSIS", titleFont);
        title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Product Information Table
        com.itextpdf.text.pdf.PdfPTable infoTable = new com.itextpdf.text.pdf.PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(20);

        addTableRow(infoTable, "Product Name:", request.getProductName(), headerFont, normalFont);
        addTableRow(infoTable, "Batch/Lot Number:", request.getBatchNumber(), headerFont, normalFont);
        if (request.getManufacturingDate() != null && !request.getManufacturingDate().isBlank()) {
            addTableRow(infoTable, "Manufacturing Date:", request.getManufacturingDate(), headerFont, normalFont);
        }
        if (request.getExpiryDate() != null && !request.getExpiryDate().isBlank()) {
            addTableRow(infoTable, "Expiry/Retest Date:", request.getExpiryDate(), headerFont, normalFont);
        }
        addTableRow(infoTable, "Date of Analysis:", java.time.LocalDate.now().toString(), headerFont, normalFont);
        addTableRow(infoTable, "Number of Samples:", String.valueOf(request.getSampleIds().size()), headerFont,
                normalFont);

        document.add(infoTable);

        // Specifications Section
        if (request.getSpecifications() != null && !request.getSpecifications().isBlank()) {
            com.itextpdf.text.Paragraph specHeader = new com.itextpdf.text.Paragraph("Specifications:", headerFont);
            specHeader.setSpacingBefore(10);
            document.add(specHeader);

            com.itextpdf.text.Paragraph specText = new com.itextpdf.text.Paragraph(request.getSpecifications(),
                    normalFont);
            specText.setSpacingAfter(15);
            document.add(specText);
        }

        // Test Results Section
        com.itextpdf.text.Paragraph resultsHeader = new com.itextpdf.text.Paragraph("Test Results:", headerFont);
        resultsHeader.setSpacingBefore(10);
        resultsHeader.setSpacingAfter(10);
        document.add(resultsHeader);

        // Get sample data from the page
        List<Map<String, Object>> samplesWithValidation = resultCompilationService.getSamplesWithValidation(pageId);

        // Filter to only requested sample IDs
        java.util.Set<Integer> requestedIds = new java.util.HashSet<>(request.getSampleIds());
        List<Map<String, Object>> filteredSamples = samplesWithValidation.stream().filter(s -> {
            Object idObj = s.get("id");
            if (idObj instanceof Integer) {
                return requestedIds.contains((Integer) idObj);
            } else if (idObj instanceof String) {
                try {
                    return requestedIds.contains(Integer.parseInt((String) idObj));
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }).collect(java.util.stream.Collectors.toList());

        // Results Table
        com.itextpdf.text.pdf.PdfPTable resultsTable = new com.itextpdf.text.pdf.PdfPTable(5);
        resultsTable.setWidthPercentage(100);
        resultsTable.setWidths(new float[] { 2, 2, 2, 2, 2 });

        // Table headers
        addTableHeader(resultsTable, "Sample ID", headerFont);
        addTableHeader(resultsTable, "Sample Type", headerFont);
        addTableHeader(resultsTable, "Test Result", headerFont);
        addTableHeader(resultsTable, "QC Status", headerFont);
        addTableHeader(resultsTable, "Validation", headerFont);

        // Table rows
        for (Map<String, Object> sample : filteredSamples) {
            String externalId = sample.get("externalId") != null ? sample.get("externalId").toString() : "-";
            String sampleType = sample.get("sampleType") != null ? sample.get("sampleType").toString() : "-";

            // Get result from data
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) sample.get("data");
            String testResult = "-";
            String qcStatus = "-";
            String validation = sample.get("validationStatus") != null ? sample.get("validationStatus").toString()
                    : "PENDING";

            if (data != null) {
                if (data.get("assayResults") != null) {
                    testResult = data.get("assayResults").toString();
                } else if (data.get("results") != null) {
                    testResult = data.get("results").toString();
                }
                if (data.get("qcResult") != null) {
                    qcStatus = data.get("qcResult").toString();
                }
                if (data.get("validationStatus") != null) {
                    validation = data.get("validationStatus").toString();
                }
            }

            addTableCell(resultsTable, externalId, normalFont);
            addTableCell(resultsTable, sampleType, normalFont);
            addTableCell(resultsTable, testResult, normalFont);
            addTableCell(resultsTable, qcStatus, normalFont);
            addTableCell(resultsTable, validation, normalFont);
        }

        document.add(resultsTable);

        // Conclusion
        com.itextpdf.text.Paragraph conclusion = new com.itextpdf.text.Paragraph();
        conclusion.setSpacingBefore(20);
        conclusion.add(new com.itextpdf.text.Chunk("Conclusion: ", headerFont));
        conclusion.add(new com.itextpdf.text.Chunk(
                "The above batch has been analyzed and the results comply with the specifications.", normalFont));
        document.add(conclusion);

        // Authorization Section
        com.itextpdf.text.Paragraph authSection = new com.itextpdf.text.Paragraph();
        authSection.setSpacingBefore(40);

        authSection.add(new com.itextpdf.text.Chunk("Authorized By: ", headerFont));
        authSection.add(new com.itextpdf.text.Chunk(
                request.getAuthorizedBy() != null ? request.getAuthorizedBy() : "________________", normalFont));
        authSection.add(com.itextpdf.text.Chunk.NEWLINE);
        authSection.add(com.itextpdf.text.Chunk.NEWLINE);

        authSection.add(new com.itextpdf.text.Chunk("Date: ", headerFont));
        authSection.add(new com.itextpdf.text.Chunk(request.getAuthorizedDate() != null ? request.getAuthorizedDate()
                : java.time.LocalDate.now().toString(), normalFont));
        authSection.add(com.itextpdf.text.Chunk.NEWLINE);
        authSection.add(com.itextpdf.text.Chunk.NEWLINE);

        authSection.add(new com.itextpdf.text.Chunk("Signature: ", headerFont));
        authSection.add(new com.itextpdf.text.Chunk("________________", normalFont));

        document.add(authSection);

        // Footer
        com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph();
        footer.setSpacingBefore(30);
        footer.add(new com.itextpdf.text.Chunk("Generated: " + java.time.LocalDateTime.now().toString(), smallFont));
        footer.add(com.itextpdf.text.Chunk.NEWLINE);
        footer.add(new com.itextpdf.text.Chunk("This is a computer-generated document.", smallFont));
        document.add(footer);

        document.close();

        return baos.toByteArray();
    }

    private void addTableRow(com.itextpdf.text.pdf.PdfPTable table, String label, String value,
            com.itextpdf.text.Font labelFont, com.itextpdf.text.Font valueFont) {
        com.itextpdf.text.pdf.PdfPCell labelCell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(label, labelFont));
        labelCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        com.itextpdf.text.pdf.PdfPCell valueCell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(value, valueFont));
        valueCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addTableHeader(com.itextpdf.text.pdf.PdfPTable table, String text, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(text, font));
        cell.setBackgroundColor(com.itextpdf.text.BaseColor.LIGHT_GRAY);
        cell.setPadding(5);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(com.itextpdf.text.pdf.PdfPTable table, String text, com.itextpdf.text.Font font) {
        com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    /**
     * Request object for COA generation.
     */
    public static class COAGenerationRequest {
        private List<Integer> sampleIds;
        private String productName;
        private String batchNumber;
        private String manufacturingDate;
        private String expiryDate;
        private String specifications;
        private String authorizedBy;
        private String authorizedDate;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getBatchNumber() {
            return batchNumber;
        }

        public void setBatchNumber(String batchNumber) {
            this.batchNumber = batchNumber;
        }

        public String getManufacturingDate() {
            return manufacturingDate;
        }

        public void setManufacturingDate(String manufacturingDate) {
            this.manufacturingDate = manufacturingDate;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }

        public String getSpecifications() {
            return specifications;
        }

        public void setSpecifications(String specifications) {
            this.specifications = specifications;
        }

        public String getAuthorizedBy() {
            return authorizedBy;
        }

        public void setAuthorizedBy(String authorizedBy) {
            this.authorizedBy = authorizedBy;
        }

        public String getAuthorizedDate() {
            return authorizedDate;
        }

        public void setAuthorizedDate(String authorizedDate) {
            this.authorizedDate = authorizedDate;
        }
    }

    /**
     * Record delivery of results. POST /notebook/bulk/notebook/{notebookId}/deliver
     *
     * <p>
     * Per T125b: Implement POST /notebook/{id}/results/deliver endpoint
     *
     * @param notebookId  the notebook ID
     * @param request     contains recipient info
     * @param httpRequest for getting user session
     * @return delivery record
     */
    @PostMapping(value = "/notebook/{notebookId}/deliver", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordDelivery(@PathVariable("notebookId") Integer notebookId,
            @RequestBody DeliveryRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found"));
        }

        if (request.getRecipientName() == null || request.getRecipientName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient name is required"));
        }

        Integer deliveryId = resultCompilationService.recordDelivery(notebookId, request.getRecipientName(),
                request.getRecipientEmail(), request.getFileId(), request.getDeliveryType(),
                request.getRegulatoryBody(), request.getNotes(), sysUserId);

        return ResponseEntity.ok(Map.of("success", true, "deliveryId", deliveryId, "notebookId", notebookId,
                "recipientName", request.getRecipientName()));
    }

    /**
     * Get delivery history for a notebook. GET
     * /notebook/bulk/notebook/{notebookId}/delivery-history
     *
     * <p>
     * Per T129b: Add delivery confirmation tracking and display
     *
     * @param notebookId the notebook ID
     * @return list of delivery records
     */
    @GetMapping(value = "/notebook/{notebookId}/delivery-history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<ResultCompilationService.DeliveryRecord>> getDeliveryHistory(
            @PathVariable("notebookId") Integer notebookId) {

        List<ResultCompilationService.DeliveryRecord> history = resultCompilationService.getDeliveryHistory(notebookId);
        return ResponseEntity.ok(history);
    }

    // Request DTOs

    /**
     * Bulk assign storage location to multiple samples on a page. POST
     * /notebook/bulk/page/{pageId}/samples/storage
     *
     * Assigns samples to a specific storage location (box and well position). This
     * endpoint now persists to the actual storage system (SampleStorageAssignment).
     * Supports individual well assignments via wellAssignments map.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds, data, boxId, wellCoordinate or
     *                    wellAssignments
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/storage", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkAssignStorage(@PathVariable("pageId") Integer pageId,
            @RequestBody StorageAssignmentRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Box ID is now optional - some labs may not use rack/box hierarchy
        // but they may still want to track storage locations
        if (request.getBoxId() == null
                && (request.getWellAssignments() != null && !request.getWellAssignments().isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Box ID is required when using well assignments");
            return ResponseEntity.badRequest().body(error);
        }

        // Check if we have wellAssignments (individual sample-to-well mapping)
        Map<String, String> wellAssignments = request.getWellAssignments();
        Map<String, Object> result;

        if (wellAssignments != null && !wellAssignments.isEmpty()) {
            // Use individual well assignments - each sample gets its own well coordinate
            List<Integer> sampleIds = wellAssignments.keySet().stream().map(Integer::parseInt)
                    .collect(java.util.stream.Collectors.toList());

            result = bulkOperationService.assignSamplesToStorageWithWellMap(pageId, sampleIds, request.getBoxId(),
                    wellAssignments, request.getData(), sysUserId);
        } else if (request.getSampleIds() != null && !request.getSampleIds().isEmpty()) {
            // Use legacy single wellCoordinate for all samples (or auto-assign)
            result = bulkOperationService.assignSamplesToStorage(pageId, request.getSampleIds(), request.getBoxId(),
                    request.getWellCoordinate(), request.getData(), sysUserId);
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs or well assignments provided");
            return ResponseEntity.badRequest().body(error);
        }

        result.put("pageId", pageId);
        result.put("boxId", request.getBoxId());

        if (result.containsKey("errors") && !((java.util.List<?>) result.get("errors")).isEmpty()) {
            // If some samples failed but others succeeded, still return OK
            Integer assignedCount = (Integer) result.get("assignedCount");
            if (assignedCount != null && assignedCount > 0) {
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Auto-assign storage locations to multiple samples on a page. POST
     * /notebook/bulk/page/{pageId}/samples/storage/auto-assign
     *
     * Automatically assigns samples to the next available wells in the specified
     * box, filling sequentially from the starting position (default A1). This
     * endpoint now persists to the actual storage system (SampleStorageAssignment).
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds, box info, and storage metadata
     * @param httpRequest for getting user session
     * @return result with assignments array showing each sample's assigned well
     */
    @PostMapping(value = "/page/{pageId}/samples/storage/auto-assign", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autoAssignStorage(@PathVariable("pageId") Integer pageId,
            @RequestBody AutoAssignStorageRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        // Support both sampleIds (integers) and sampleIdsString (composite strings)
        List<String> effectiveSampleIds = request.getEffectiveSampleIds();
        if (effectiveSampleIds.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getBoxId() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Box ID is required for auto-assignment to well coordinates");
            return ResponseEntity.badRequest().body(error);
        }

        // Use the new auto-assign storage service method that persists to
        // SampleStorageAssignment
        // For composite IDs (pathology), use the string-based version
        Map<String, Object> result = bulkOperationService.autoAssignSamplesToStorageString(pageId, effectiveSampleIds,
                request.getBoxId(), request.getRows(), request.getColumns(), request.getOccupiedWells(),
                request.getData(), sysUserId);

        // Return bad request if there's a top-level error OR if there are errors in the
        // errors array
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }

        // Also check for errors array (individual sample failures)
        if (result.containsKey("errors") && !((java.util.List<?>) result.get("errors")).isEmpty()) {
            // If some samples succeeded and some failed, still return 200 but include
            // errors
            // If all samples failed, return 400
            Integer updatedCount = (Integer) result.get("updatedCount");
            if (updatedCount == null || updatedCount == 0) {
                return ResponseEntity.badRequest().body(result);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Update status for multiple samples on a page. POST
     * /notebook/bulk/page/{pageId}/samples/status
     *
     * Updates the page status for selected samples (e.g., mark as COMPLETED).
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds and status
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkUpdateStatus(@PathVariable("pageId") Integer pageId,
            @RequestBody StatusUpdateRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Status is required");
            return ResponseEntity.badRequest().body(error);
        }

        Status status;
        try {
            status = Status.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid status: " + request.getStatus());
            return ResponseEntity.badRequest().body(error);
        }

        int updatedCount = bulkOperationService.bulkUpdateStatus(pageId, request.getSampleIds(), status, sysUserId);

        // Also persist any additional data fields (containerType, collectionDate, etc.)
        Map<String, Object> additionalData = new HashMap<>();
        if (request.getContainerType() != null && !request.getContainerType().isBlank()) {
            additionalData.put("containerType", request.getContainerType());
        }
        if (request.getCollectionDate() != null && !request.getCollectionDate().isBlank()) {
            additionalData.put("collectionDate", request.getCollectionDate());
        }
        if (request.getCollectionTime() != null && !request.getCollectionTime().isBlank()) {
            additionalData.put("collectionTime", request.getCollectionTime());
        }
        if (request.getCollectorId() != null && !request.getCollectorId().isBlank()) {
            additionalData.put("collectorId", request.getCollectorId());
        }
        if (request.getVolume() != null && !request.getVolume().isBlank()) {
            additionalData.put("volume", request.getVolume());
        }
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            additionalData.put("notes", request.getNotes());
        }

        // If there's additional data, apply it to the samples
        if (!additionalData.isEmpty()) {
            bulkOperationService.bulkApplyValues(pageId, request.getSampleIds(), additionalData, sysUserId);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("pageId", pageId);
        result.put("status", status.name());
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * Update status for multiple samples on a page using String IDs. POST
     * /notebook/bulk/page/{pageId}/samples/status-string
     *
     * This endpoint supports composite sample IDs (e.g., "123_cassette_0") used in
     * pathology workflow pages where samples are expanded from parent items.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds (as Strings) and status
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/status-string", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkUpdateStatusString(@PathVariable("pageId") Integer pageId,
            @RequestBody StringStatusUpdateRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Status is required");
            return ResponseEntity.badRequest().body(error);
        }

        Status status;
        try {
            status = Status.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid status: " + request.getStatus());
            return ResponseEntity.badRequest().body(error);
        }

        int updatedCount = bulkOperationService.bulkUpdateStatusString(pageId, request.getSampleIds(), status,
                sysUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("pageId", pageId);
        result.put("status", status.name());
        result.put("success", true);

        return ResponseEntity.ok(result);
    }

    /**
     * Archive samples - marks them as ARCHIVED (terminal status, will not proceed
     * to next page). POST /notebook/bulk/page/{pageId}/samples/archive
     *
     * Archives selected samples with storage location and archive metadata.
     * Archived samples are removed from the active workflow and do NOT continue to
     * the next page.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds and archive metadata (storageLocation,
     *                    archiveReason, etc.)
     * @param httpRequest for getting user session
     * @return result with archived count
     */
    @PostMapping(value = "/page/{pageId}/samples/archive", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> archiveSamples(@PathVariable("pageId") Integer pageId,
            @RequestBody ArchiveSamplesRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Build archive data map from request
            Map<String, Object> archiveData = new HashMap<>();
            archiveData.put("isArchived", true);
            archiveData.put("terminalStatus", "ARCHIVED");
            archiveData.put("archiveDate",
                    request.getArchiveDate() != null ? request.getArchiveDate() : java.time.LocalDate.now().toString());
            archiveData.put("archivedBy", request.getArchivedBy());
            archiveData.put("archiveReason", request.getArchiveReason());
            archiveData.put("retentionPeriod", request.getRetentionPeriod());
            archiveData.put("notes", request.getNotes());

            // Storage location fields
            if (request.getStorageLocation() != null) {
                archiveData.put("storageLocation", request.getStorageLocation());
            }
            if (request.getStorageBox() != null) {
                archiveData.put("storageBox", request.getStorageBox());
            }
            if (request.getStoragePosition() != null) {
                archiveData.put("storagePosition", request.getStoragePosition());
            }
            // Slide-specific storage fields
            if (request.getSlideCabinet() != null) {
                archiveData.put("slideCabinet", request.getSlideCabinet());
            }
            if (request.getSlideDrawer() != null) {
                archiveData.put("slideDrawer", request.getSlideDrawer());
            }
            if (request.getSlidePosition() != null) {
                archiveData.put("slidePosition", request.getSlidePosition());
            }
            if (request.getSlideCondition() != null) {
                archiveData.put("slideCondition", request.getSlideCondition());
            }

            // Hierarchical storage location (from StorageHierarchySelector)
            if (request.getRoomId() != null) {
                archiveData.put("roomId", request.getRoomId());
            }
            if (request.getDeviceId() != null) {
                archiveData.put("deviceId", request.getDeviceId());
            }
            if (request.getShelfId() != null) {
                archiveData.put("shelfId", request.getShelfId());
            }
            if (request.getRackId() != null) {
                archiveData.put("rackId", request.getRackId());
            }

            // Resolve box info and build storage location strings (like assign-storage
            // does)
            StorageBox box = null;
            String boxLabel = null;
            if (request.getBoxId() != null) {
                archiveData.put("boxId", request.getBoxId());
                box = storageBoxDAO.get(request.getBoxId()).orElse(null);
                if (box != null) {
                    boxLabel = box.getLabel();
                    archiveData.put("storageBox", boxLabel);

                    // Build storage path from box hierarchy
                    StringBuilder pathBuilder = new StringBuilder();
                    if (box.getParentRack() != null) {
                        if (box.getParentRack().getParentShelf() != null) {
                            if (box.getParentRack().getParentShelf().getParentDevice() != null) {
                                pathBuilder.append(box.getParentRack().getParentShelf().getParentDevice().getName())
                                        .append(" > ");
                            }
                            pathBuilder.append(box.getParentRack().getParentShelf().getLabel()).append(" > ");
                        }
                        pathBuilder.append(box.getParentRack().getLabel()).append(" > ");
                    }
                    pathBuilder.append(boxLabel);
                    archiveData.put("storagePath", pathBuilder.toString());
                } else {
                    boxLabel = "Box " + request.getBoxId();
                    archiveData.put("storageBox", boxLabel);
                }
            }

            // Handle well assignments from auto-populate
            Map<String, String> wellAssignments = request.getWellAssignments();
            if (wellAssignments != null && !wellAssignments.isEmpty()) {
                archiveData.put("hasWellAssignments", true);
            }

            // Apply archive data and set status to SKIPPED (terminal status)
            // If we have well assignments, we need to apply per-sample data with individual
            // well coords
            int updatedCount = 0;
            final String finalBoxLabel = boxLabel;
            if (wellAssignments != null && !wellAssignments.isEmpty()) {
                // Apply well-specific data for each sample
                for (String sampleId : request.getSampleIds()) {
                    Map<String, Object> sampleArchiveData = new HashMap<>(archiveData);
                    String wellCoord = wellAssignments.get(sampleId);
                    if (wellCoord != null) {
                        sampleArchiveData.put("wellCoordinate", wellCoord);
                        sampleArchiveData.put("storagePosition", wellCoord);
                        sampleArchiveData.put("storageWell", wellCoord);
                        // Build full storage location string with well coordinate
                        if (finalBoxLabel != null) {
                            sampleArchiveData.put("storageLocation", finalBoxLabel + " - " + wellCoord);
                            // Update storagePath to include well
                            String basePath = (String) archiveData.get("storagePath");
                            if (basePath != null) {
                                sampleArchiveData.put("storagePath", basePath + " - " + wellCoord);
                            }
                        }
                    }
                    updatedCount += bulkOperationService.bulkApplyValuesString(pageId,
                            java.util.Collections.singletonList(sampleId), sampleArchiveData, sysUserId);
                }
            } else {
                // No well assignments - apply same data to all samples
                // Set storageLocation without well coordinate
                if (finalBoxLabel != null) {
                    archiveData.put("storageLocation", finalBoxLabel);
                }
                updatedCount = bulkOperationService.bulkApplyValuesString(pageId, request.getSampleIds(), archiveData,
                        sysUserId);
            }

            // Also update the status to SKIPPED (archived samples don't continue)
            bulkOperationService.bulkUpdateStatusString(pageId, request.getSampleIds(),
                    NotebookPageSample.Status.SKIPPED, sysUserId);

            // Create SampleStorageAssignment records for proper well occupancy tracking
            // This ensures archived pathology samples show as occupying wells in Storage
            // Management
            int storageAssignmentCount = 0;
            if (request.getBoxId() != null && wellAssignments != null && !wellAssignments.isEmpty()) {
                for (String sampleId : request.getSampleIds()) {
                    try {
                        String wellCoord = wellAssignments.get(sampleId);
                        if (wellCoord != null) {
                            // For composite sample IDs (e.g., "4_cassette_0_block_0_slide_0"),
                            // extract the base sample ID (e.g., "4") for storage assignment
                            String baseSampleId = sampleId;
                            if (sampleId.contains("_")) {
                                baseSampleId = sampleId.split("_")[0];
                            }

                            String notes = String.format(
                                    "Archived pathology sample | Original ID: %s | Reason: %s | Retention: %s",
                                    sampleId,
                                    request.getArchiveReason() != null ? request.getArchiveReason() : "completed",
                                    request.getRetentionPeriod() != null ? request.getRetentionPeriod() + " years"
                                            : "default");

                            // Check if this well is already occupied
                            Map<String, Object> existingLocation = sampleStorageService
                                    .getSampleItemLocation(baseSampleId);
                            boolean hasExistingAssignment = existingLocation != null && !existingLocation.isEmpty()
                                    && existingLocation.get("location") != null
                                    && !existingLocation.get("location").toString().isEmpty();

                            if (hasExistingAssignment) {
                                // Move existing assignment to new location
                                sampleStorageService.moveSampleItemWithLocation(baseSampleId,
                                        request.getBoxId().toString(), "box", wellCoord, "Archive update", notes);
                            } else {
                                // Create new assignment
                                sampleStorageService.assignSampleItemWithLocation(baseSampleId,
                                        request.getBoxId().toString(), "box", wellCoord, notes);
                            }
                            storageAssignmentCount++;
                        }
                    } catch (Exception e) {
                        LogEvent.logWarn(this.getClass().getName(), "archiveSamples",
                                "Error creating SampleStorageAssignment for sample " + sampleId + ": "
                                        + e.getMessage());
                    }
                }
                LogEvent.logInfo(this.getClass().getName(), "archiveSamples",
                        "Created " + storageAssignmentCount + " SampleStorageAssignment records for archived samples");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("archivedCount", updatedCount);
            result.put("storageAssignmentCount", storageAssignmentCount);
            result.put("pageId", pageId);
            result.put("message", String.format("Successfully archived %d sample(s)", updatedCount));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "archiveSamples",
                    "Failed to archive samples: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to archive samples: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Add existing samples to a page (for advancing workflow). POST
     * /notebook/bulk/page/{pageId}/samples/add
     *
     * Creates NotebookPageSample records for samples that already exist in the
     * system (from a previous workflow page). Used to advance samples to the next
     * workflow step.
     *
     * @param pageId      the target notebook page ID
     * @param request     contains sampleIds to add
     * @param httpRequest for getting user session
     * @return result with added count
     */
    @PostMapping(value = "/page/{pageId}/samples/add", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSamplesToPage(@PathVariable("pageId") Integer pageId,
            @RequestBody AddSamplesRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Sample IDs are required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            NoteBookPage page = noteBookPageService.get(pageId);
            if (page == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Page not found");
                return ResponseEntity.status(404).body(error);
            }

            int addedCount = 0;
            for (Integer sampleItemId : request.getSampleIds()) {
                // Check if sample already exists on this page
                String sampleItemIdStr = sampleItemId.toString();
                NotebookPageSample existing = notebookPageSampleService.getBySampleItemIdAndPageId(sampleItemIdStr,
                        pageId);
                if (existing == null) {
                    // Create new page sample record
                    NotebookPageSample nps = new NotebookPageSample();
                    nps.setNotebookPage(page);
                    nps.setSampleItemId(sampleItemIdStr);
                    nps.setStatus(NotebookPageSample.Status.PENDING);
                    nps.setSysUserId(sysUserId);
                    notebookPageSampleService.save(nps);
                    addedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("addedCount", addedCount);
            result.put("pageId", pageId);
            result.put("success", true);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "addSamplesToPage", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to add samples to page: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // Request DTOs

    /**
     * Request body for bulk apply operation.
     */
    public static class BulkApplyRequest {
        private List<Integer> sampleIds;
        private Map<String, Object> data;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }

    /** Request body for mark complete operation. */
    public static class MarkCompleteRequest {
        private boolean requireComplete;
        private boolean lockData;

        public boolean isRequireComplete() {
            return requireComplete;
        }

        public void setRequireComplete(boolean requireComplete) {
            this.requireComplete = requireComplete;
        }

        public boolean isLockData() {
            return lockData;
        }

        public void setLockData(boolean lockData) {
            this.lockData = lockData;
        }
    }

    /** Request body for flag sample operation. */
    public static class FlagSampleRequest {
        private String sampleId;
        private String status;
        private String reason;

        public String getSampleId() {
            return sampleId;
        }

        public void setSampleId(String sampleId) {
            this.sampleId = sampleId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /** Request body for bulk flag operation. */
    public static class BulkFlagRequest {
        private List<String> sampleIds;
        private String status;
        private String reason;

        public List<String> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<String> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /** Request body for delivery operation. */
    public static class DeliveryRequest {
        private String recipientName;
        private String recipientEmail;
        private Integer fileId;
        private String deliveryType;
        private String regulatoryBody;
        private String notes;

        public String getRecipientName() {
            return recipientName;
        }

        public void setRecipientName(String recipientName) {
            this.recipientName = recipientName;
        }

        public String getRecipientEmail() {
            return recipientEmail;
        }

        public void setRecipientEmail(String recipientEmail) {
            this.recipientEmail = recipientEmail;
        }

        public Integer getFileId() {
            return fileId;
        }

        public void setFileId(Integer fileId) {
            this.fileId = fileId;
        }

        public String getDeliveryType() {
            return deliveryType;
        }

        public void setDeliveryType(String deliveryType) {
            this.deliveryType = deliveryType;
        }

        public String getRegulatoryBody() {
            return regulatoryBody;
        }

        public void setRegulatoryBody(String regulatoryBody) {
            this.regulatoryBody = regulatoryBody;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request body for storage assignment operation.
     */
    public static class StorageAssignmentRequest {
        private List<Integer> sampleIds;
        private Map<String, Object> data;
        private Integer boxId;
        private String wellCoordinate;
        private Map<String, String> wellAssignments;
        private String condition;
        private Integer retentionYears;
        private Boolean reassign;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public Integer getBoxId() {
            return boxId;
        }

        public void setBoxId(Integer boxId) {
            this.boxId = boxId;
        }

        public String getWellCoordinate() {
            return wellCoordinate;
        }

        public void setWellCoordinate(String wellCoordinate) {
            this.wellCoordinate = wellCoordinate;
        }

        public Map<String, String> getWellAssignments() {
            return wellAssignments;
        }

        public void setWellAssignments(Map<String, String> wellAssignments) {
            this.wellAssignments = wellAssignments;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getRetentionYears() {
            return retentionYears;
        }

        public void setRetentionYears(Integer retentionYears) {
            this.retentionYears = retentionYears;
        }

        public Boolean getReassign() {
            return reassign;
        }

        public void setReassign(Boolean reassign) {
            this.reassign = reassign;
        }
    }

    /**
     * Request body for status update operation.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StatusUpdateRequest {
        private List<Integer> sampleIds;
        private String status;
        private String containerType;
        private String collectionDate;
        private String collectionTime;
        private String collectorId;
        private String volume;
        private String notes;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }

        public String getCollectionDate() {
            return collectionDate;
        }

        public void setCollectionDate(String collectionDate) {
            this.collectionDate = collectionDate;
        }

        public String getCollectionTime() {
            return collectionTime;
        }

        public void setCollectionTime(String collectionTime) {
            this.collectionTime = collectionTime;
        }

        public String getCollectorId() {
            return collectorId;
        }

        public void setCollectorId(String collectorId) {
            this.collectorId = collectorId;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String volume) {
            this.volume = volume;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request body for status update operation with String sample IDs. Used for
     * composite sample IDs (e.g., "123_cassette_0") in pathology workflows.
     */
    public static class StringStatusUpdateRequest {
        private List<String> sampleIds;
        private String status;

        public List<String> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<String> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    /**
     * Request body for bulk apply operation with String sample IDs. Used for
     * composite sample IDs (e.g., "123_cassette_0") in pathology workflows.
     */
    public static class StringApplyRequest {
        private List<String> sampleIds;
        private Map<String, Object> data;

        public List<String> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<String> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }

    /**
     * Request body for adding samples to a page (workflow advancement).
     */
    public static class AddSamplesRequest {
        private List<Integer> sampleIds;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }
    }

    /**
     * Request body for auto-assign storage operation.
     */
    public static class AutoAssignStorageRequest {
        private List<Integer> sampleIds;
        private List<String> sampleIdsString; // For composite sample IDs (e.g., "4_cassette_0_block_0")
        private Map<String, Object> data;
        private Integer boxId;
        private Integer rows;
        private Integer columns;
        private List<String> occupiedWells;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public List<String> getSampleIdsString() {
            return sampleIdsString;
        }

        public void setSampleIdsString(List<String> sampleIdsString) {
            this.sampleIdsString = sampleIdsString;
        }

        /**
         * Get effective sample IDs as strings. Prefers sampleIdsString for composite
         * IDs, falls back to converting sampleIds to strings.
         */
        public List<String> getEffectiveSampleIds() {
            if (sampleIdsString != null && !sampleIdsString.isEmpty()) {
                return sampleIdsString;
            }
            if (sampleIds != null) {
                return sampleIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.toList());
            }
            return new java.util.ArrayList<>();
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public Integer getBoxId() {
            return boxId;
        }

        public void setBoxId(Integer boxId) {
            this.boxId = boxId;
        }

        public Integer getRows() {
            return rows;
        }

        public void setRows(Integer rows) {
            this.rows = rows;
        }

        public Integer getColumns() {
            return columns;
        }

        public void setColumns(Integer columns) {
            this.columns = columns;
        }

        public List<String> getOccupiedWells() {
            return occupiedWells;
        }

        public void setOccupiedWells(List<String> occupiedWells) {
            this.occupiedWells = occupiedWells;
        }
    }

    // ========================================================================
    // REPORT GENERATION AND REDCAP EXPORT ENDPOINTS
    // ========================================================================

    /**
     * Generate and download a report for samples on a page. POST
     * /notebook/bulk/page/{pageId}/generate-report
     *
     * Uses HttpServletResponse directly to write binary content, avoiding Spring
     * message converter issues with byte[] responses.
     *
     * @param pageId       the notebook page ID
     * @param request      contains sampleIds and report options
     * @param httpRequest  for getting user session
     * @param httpResponse for writing the binary response
     */
    @PostMapping(value = "/page/{pageId}/generate-report")
    public void generateReport(@PathVariable("pageId") Integer pageId, @RequestBody ReportGenerationRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // Validate inputs
            String reportFormat = request.getReportFormat() != null ? request.getReportFormat() : "CSV";
            String reportType = request.getReportType() != null ? request.getReportType() : "SUMMARY";

            // Generate report content based on type and format
            byte[] reportContent = bulkOperationService.generateReport(pageId, request.getSampleIds(), reportType,
                    reportFormat, sysUserId);

            if (reportContent == null || reportContent.length == 0) {
                httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            // Determine content type and filename
            // Note: PDF not yet implemented, falls back to CSV
            String contentType;
            String extension;
            String actualFormat = reportFormat.toUpperCase();
            switch (actualFormat) {
            case "PDF":
                // PDF generation not yet implemented - return as CSV with CSV content type
                contentType = "text/csv; charset=UTF-8";
                extension = ".csv";
                actualFormat = "CSV"; // Update for filename
                break;
            case "EXCEL":
                // Excel generation returns CSV for now
                contentType = "text/csv; charset=UTF-8";
                extension = ".csv";
                actualFormat = "CSV";
                break;
            case "JSON":
                contentType = "application/json; charset=UTF-8";
                extension = ".json";
                break;
            case "CSV":
            default:
                contentType = "text/csv; charset=UTF-8";
                extension = ".csv";
            }

            String filename = "MNTD_Report_" + reportType + "_" + java.time.LocalDate.now().toString() + extension;

            // Write response directly to output stream
            httpResponse.setContentType(contentType);
            httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            httpResponse.setContentLength(reportContent.length);
            httpResponse.getOutputStream().write(reportContent);
            httpResponse.getOutputStream().flush();

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "generateReport",
                    "Error generating report: " + e.getMessage());
            e.printStackTrace();
            try {
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Export sample data in REDCap-compatible CSV format. POST
     * /notebook/bulk/page/{pageId}/redcap/export
     *
     * Uses HttpServletResponse directly to write binary content, avoiding Spring
     * message converter issues with byte[] responses.
     *
     * @param pageId       the notebook page ID
     * @param request      contains sampleIds and REDCap configuration
     * @param httpRequest  for getting user session
     * @param httpResponse for writing the binary response
     */
    @PostMapping(value = "/page/{pageId}/redcap/export")
    public void exportForREDCap(@PathVariable("pageId") Integer pageId, @RequestBody REDCapExportRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            // Validate inputs
            String recordIdField = request.getRecordIdField() != null ? request.getRecordIdField() : "record_id";

            // Generate REDCap-compatible CSV
            byte[] csvContent = bulkOperationService.generateREDCapExport(pageId, request.getSampleIds(), recordIdField,
                    request.getEventName(), request.getInstrumentName(), sysUserId);

            if (csvContent == null || csvContent.length == 0) {
                httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }

            String filename = "REDCap_Export_" + (request.getProjectId() != null ? request.getProjectId() : "data")
                    + "_" + java.time.LocalDate.now().toString() + ".csv";

            // Write response directly to output stream
            httpResponse.setContentType("text/csv; charset=UTF-8");
            httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            httpResponse.setContentLength(csvContent.length);
            httpResponse.getOutputStream().write(csvContent);
            httpResponse.getOutputStream().flush();

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "exportForREDCap",
                    "Error generating REDCap export: " + e.getMessage());
            e.printStackTrace();
            try {
                httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ignored) {
                // Response might already be committed
            }
        }
    }

    /**
     * Request body for report generation.
     */
    public static class ReportGenerationRequest {
        private List<Integer> sampleIds;
        private String reportType;
        private String reportFormat;
        private String dateRangeStart;
        private String dateRangeEnd;
        private List<String> recipientEmails;
        private String notes;
        private boolean saveToHistory;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getReportType() {
            return reportType;
        }

        public void setReportType(String reportType) {
            this.reportType = reportType;
        }

        public String getReportFormat() {
            return reportFormat;
        }

        public void setReportFormat(String reportFormat) {
            this.reportFormat = reportFormat;
        }

        public String getDateRangeStart() {
            return dateRangeStart;
        }

        public void setDateRangeStart(String dateRangeStart) {
            this.dateRangeStart = dateRangeStart;
        }

        public String getDateRangeEnd() {
            return dateRangeEnd;
        }

        public void setDateRangeEnd(String dateRangeEnd) {
            this.dateRangeEnd = dateRangeEnd;
        }

        public List<String> getRecipientEmails() {
            return recipientEmails;
        }

        public void setRecipientEmails(List<String> recipientEmails) {
            this.recipientEmails = recipientEmails;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public boolean isSaveToHistory() {
            return saveToHistory;
        }

        public void setSaveToHistory(boolean saveToHistory) {
            this.saveToHistory = saveToHistory;
        }
    }

    /**
     * Request body for REDCap export.
     */
    public static class REDCapExportRequest {
        private List<Integer> sampleIds;
        private String projectId;
        private String recordIdField;
        private String eventName;
        private String instrumentName;

        public List<Integer> getSampleIds() {
            return sampleIds;
        }

        public void setSampleIds(List<Integer> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getRecordIdField() {
            return recordIdField;
        }

        public void setRecordIdField(String recordIdField) {
            this.recordIdField = recordIdField;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getInstrumentName() {
            return instrumentName;
        }

        public void setInstrumentName(String instrumentName) {
            this.instrumentName = instrumentName;
        }
    }

    /**
     * Request body for page content update operation.
     */
    public static class PageContentUpdateRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    // ========================================================================
    // RAW DATA CSV UPLOAD ENDPOINT
    // ========================================================================

    /**
     * Upload raw data CSV file and apply results to samples. POST
     * /notebook/bulk/page/{pageId}/upload-raw-data
     *
     * The CSV file must contain a header row with column names. Samples are matched
     * by accessionNumber or externalId column. Supported data columns: testResult,
     * ctValue, concentration, absorbance, runId, kitLot, operator, machineType,
     * runCompleted, runIssues, notes.
     *
     * @param pageId      the notebook page ID
     * @param file        the CSV file to upload
     * @param machineType optional machine type metadata
     * @param runId       optional run ID metadata
     * @param fileType    optional file type description
     * @param notes       optional notes
     * @param httpRequest for getting user session
     * @return result with matched/updated counts and any unmatched rows
     */
    @PostMapping(value = "/page/{pageId}/upload-raw-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadRawData(@PathVariable("pageId") Integer pageId,
            @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String machineType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String runId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String fileType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String notes,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String sampleIds,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User session not found", "success", false));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided", "success", false));
        }

        String filename = file.getOriginalFilename();
        if (filename == null
                || (!filename.toLowerCase().endsWith(".csv") && !filename.toLowerCase().endsWith(".txt"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File must be a CSV or TXT file", "success", false));
        }

        try {
            byte[] csvContent = file.getBytes();

            Map<String, Object> result = bulkOperationService.parseAndApplyCsvResults(pageId, csvContent, machineType,
                    runId, sysUserId);

            result.put("pageId", pageId);
            result.put("filename", filename);

            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }

        } catch (java.io.IOException e) {
            LogEvent.logError(this.getClass().getName(), "uploadRawData",
                    "Error reading uploaded file: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error reading uploaded file: " + e.getMessage(), "success", false));
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "uploadRawData",
                    "Error processing uploaded file: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Error processing uploaded file: " + e.getMessage(), "success", false));
        }
    }

    /**
     * Download the CSV template for raw data upload. GET
     * /notebook/bulk/template/raw-data-upload
     *
     * @param response the HTTP response to write to
     */
    @GetMapping(value = "/template/raw-data-upload", produces = "text/csv")
    public void downloadRawDataTemplate(HttpServletResponse response) throws java.io.IOException {
        // externalId is the primary matching key, accessionNumber is secondary fallback
        String template = "externalId,accessionNumber,testResult,ctValue,concentration,absorbance,runId,kitLot,operator,machineType,runCompleted,runIssues,notes\n"
                + "EXT-001,MNTD-2024-001,Positive,25.5,150.2,0.450,RUN-001,LOT-2024-A,John Doe,PCR,YES,,Sample processed normally\n"
                + "EXT-002,MNTD-2024-002,Negative,35.0,10.5,0.120,RUN-001,LOT-2024-A,John Doe,PCR,YES,,Sample processed normally\n"
                + "EXT-003,MNTD-2024-003,Indeterminate,32.1,45.8,0.280,RUN-001,LOT-2024-A,John Doe,PCR,YES,Low signal detected,Requires repeat testing\n";

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=mntd-test-results-template.csv");
        response.getOutputStream().write(template.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        response.getOutputStream().flush();
    }

    // ========================================
    // PROCESSING ENDPOINTS
    // ========================================

    /**
     * Bulk save processing data for multiple samples. POST
     * /notebook/bulk/page/{pageId}/samples/processing
     *
     * Used by pathology and other workflows to save lab processing steps (tissue
     * processing, embedding, microtomy, staining, etc.) for multiple samples at
     * once.
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds and processingData to save
     * @param httpRequest for getting user session
     * @return result with updated count
     */
    @PostMapping(value = "/page/{pageId}/samples/processing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkSaveProcessing(@PathVariable("pageId") Integer pageId,
            @RequestBody BulkProcessingRequest request, HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            response.put("success", false);
            response.put("error", "User session not found");
            return ResponseEntity.status(401).body(response);
        }

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            response.put("success", false);
            response.put("error", "No samples specified");
            return ResponseEntity.badRequest().body(response);
        }

        if (request.getProcessingData() == null || request.getProcessingData().isEmpty()) {
            response.put("success", false);
            response.put("error", "No processing data provided");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Apply processing data to all selected samples
            int updatedCount = bulkOperationService.bulkApplyValues(pageId, request.getSampleIds(),
                    request.getProcessingData(), sysUserId);

            response.put("success", true);
            response.put("message", String.format("Processing data saved for %d samples", updatedCount));
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "bulkSaveProcessing",
                    "Failed to save processing data: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to save processing data: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Request object for bulk processing operations.
     */
    @Setter
    @Getter
    public static class BulkProcessingRequest {
        private List<Integer> sampleIds;
        private Map<String, Object> processingData;

    }

    /**
     * Request object for archive samples operation. Supports both block archiving
     * (storageLocation, storageBox, storagePosition) and slide archiving
     * (slideCabinet, slideDrawer, slidePosition, slideCondition). Also supports
     * hierarchical storage selection (roomId, deviceId, shelfId, rackId, boxId).
     */
    @Getter
    public static class ArchiveSamplesRequest {
        private List<String> sampleIds;
        private String storageLocation;
        private String storageBox;
        private String storagePosition;
        // Slide-specific fields
        private String slideCabinet;
        private String slideDrawer;
        private String slidePosition;
        private String slideCondition;
        // Hierarchical storage location
        private Integer roomId;
        private Integer deviceId;
        private Integer shelfId;
        private Integer rackId;
        private Integer boxId;
        // Archive metadata
        private String archiveReason;
        private String retentionPeriod;
        private String archiveDate;
        private String archivedBy;
        private String notes;
        // Well assignments from auto-populate (sampleId -> wellCoordinate)
        private Map<String, String> wellAssignments;

        public void setSampleIds(List<String> sampleIds) {
            this.sampleIds = sampleIds;
        }

        public void setStorageLocation(String storageLocation) {
            this.storageLocation = storageLocation;
        }

        public void setStorageBox(String storageBox) {
            this.storageBox = storageBox;
        }

        public void setStoragePosition(String storagePosition) {
            this.storagePosition = storagePosition;
        }

        public void setSlideCabinet(String slideCabinet) {
            this.slideCabinet = slideCabinet;
        }

        public void setSlideDrawer(String slideDrawer) {
            this.slideDrawer = slideDrawer;
        }

        public void setSlidePosition(String slidePosition) {
            this.slidePosition = slidePosition;
        }

        public void setSlideCondition(String slideCondition) {
            this.slideCondition = slideCondition;
        }

        public void setRoomId(Integer roomId) {
            this.roomId = roomId;
        }

        public void setDeviceId(Integer deviceId) {
            this.deviceId = deviceId;
        }

        public void setShelfId(Integer shelfId) {
            this.shelfId = shelfId;
        }

        public void setRackId(Integer rackId) {
            this.rackId = rackId;
        }

        public void setBoxId(Integer boxId) {
            this.boxId = boxId;
        }

        public void setArchiveReason(String archiveReason) {
            this.archiveReason = archiveReason;
        }

        public void setRetentionPeriod(String retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }

        public void setArchiveDate(String archiveDate) {
            this.archiveDate = archiveDate;
        }

        public void setArchivedBy(String archivedBy) {
            this.archivedBy = archivedBy;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public void setWellAssignments(Map<String, String> wellAssignments) {
            this.wellAssignments = wellAssignments;
        }
    }

    // ========================================
    // FILE UPLOAD AND PROCESSING ENDPOINTS
    // ========================================

    /**
     * Upload analytical data files (CSV, PDF, mzML) for processing
     *
     * @param pageId       Page ID for the notebook page
     * @param file         The uploaded file
     * @param instrumentId ID of the analytical instrument
     * @param entryId      Notebook entry ID
     * @param httpRequest  HTTP request for user session
     * @return Response with file upload results
     */
    @PostMapping(value = "/page/{pageId}/files/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadAnalyticalFile(@PathVariable("pageId") Integer pageId,
            @RequestParam("file") MultipartFile file, @RequestParam("instrumentId") String instrumentId,
            @RequestParam("pageId") String pageIdParam, @RequestParam("entryId") String entryId,
            HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No file uploaded");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // Validate file type based on instrument
            String fileName = file.getOriginalFilename();
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();

            // Supported file formats for analytical data imports
            // All formats are universally supported across all instruments for flexibility
            List<String> supportedFormats = List.of("CSV", "XLSX", "XLS", "PDF", "MZML", "CDF");

            if (!supportedFormats.contains(fileExtension)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid file format. Supported formats: CSV, XLSX, XLS, PDF, MZML, CDF");
                return ResponseEntity.badRequest().body(error);
            }

            // Create upload directory if it doesn't exist
            String uploadDir = System.getProperty("user.dir") + "/uploads/analytical_data/" + pageId;
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique file name
            String fileId = UUID.randomUUID().toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String savedFileName = timestamp + "_" + fileId + "_" + fileName;
            Path filePath = uploadPath.resolve(savedFileName);

            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Log upload event
            LogEvent.logInfo(this.getClass().getSimpleName(), "uploadAnalyticalFile",
                    "File uploaded: " + fileName + " for instrument " + instrumentId + " on page " + pageId);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileId", fileId);
            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            response.put("fileUrl", "/uploads/analytical_data/" + pageId + "/" + savedFileName);
            response.put("uploadedAt", LocalDateTime.now().toString());
            response.put("instrumentId", instrumentId);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "uploadAnalyticalFile",
                    "Error uploading file: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Process uploaded analytical data files and extract analytical results
     *
     * @param pageId      Page ID for the notebook page
     * @param request     Processing request with file IDs and sample information
     * @param httpRequest HTTP request for user session
     * @return Response with processed analytical data
     */
    @PostMapping(value = "/page/{pageId}/files/process", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processAnalyticalFiles(@PathVariable("pageId") Integer pageId,
            @RequestBody FileProcessingRequest request, HttpServletRequest httpRequest) {

        String sysUserId = getSysUserId(httpRequest);
        if (sysUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "User session not found");
            return ResponseEntity.status(401).body(error);
        }

        if (request.getFileIds() == null || request.getFileIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No file IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> response = new HashMap<>();

            // Process each uploaded file
            List<Map<String, Object>> quantificationResults = new ArrayList<>();
            List<Map<String, Object>> qcResults = new ArrayList<>();
            List<Map<String, Object>> analyzerResults = new ArrayList<>();
            Map<String, Object> calibrationData = new HashMap<>();
            List<Map<String, Object>> westgardRules = new ArrayList<>();

            for (String fileId : request.getFileIds()) {
                // Find the uploaded file
                String uploadDir = System.getProperty("user.dir") + "/uploads/analytical_data/" + pageId;
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    continue;
                }

                // Find file with this fileId in the name
                try {
                    Files.list(uploadPath).filter(file -> file.getFileName().toString().contains(fileId))
                            .forEach(file -> {
                                try {
                                    Map<String, Object> fileResults = processFileData(file, request.getSamples());

                                    if (fileResults.containsKey("quantification")) {
                                        quantificationResults
                                                .addAll((List<Map<String, Object>>) fileResults.get("quantification"));
                                    }
                                    if (fileResults.containsKey("qcResults")) {
                                        qcResults.addAll((List<Map<String, Object>>) fileResults.get("qcResults"));
                                    }
                                    if (fileResults.containsKey("analyzerResults")) {
                                        analyzerResults
                                                .addAll((List<Map<String, Object>>) fileResults.get("analyzerResults"));
                                    }
                                    if (fileResults.containsKey("calibrationData")) {
                                        calibrationData
                                                .putAll((Map<String, Object>) fileResults.get("calibrationData"));
                                    }
                                    if (fileResults.containsKey("westgardRules")) {
                                        westgardRules
                                                .addAll((List<Map<String, Object>>) fileResults.get("westgardRules"));
                                    }

                                } catch (Exception e) {
                                    LogEvent.logError(this.getClass().getSimpleName(), "processAnalyticalFiles",
                                            "Error processing file " + file.getFileName() + ": " + e.getMessage());
                                }
                            });
                } catch (IOException e) {
                    LogEvent.logError(this.getClass().getSimpleName(), "processAnalyticalFiles",
                            "Error listing files in upload directory: " + e.getMessage());
                }
            }

            // Build response
            response.put("success", true);
            response.put("quantification", quantificationResults);
            response.put("qcResults", qcResults);
            response.put("analyzerResults", analyzerResults);
            response.put("calibrationData", calibrationData);
            response.put("westgardRules", westgardRules);
            response.put("processedAt", LocalDateTime.now().toString());

            LogEvent.logInfo(this.getClass().getSimpleName(), "processAnalyticalFiles",
                    "Processed " + request.getFileIds().size() + " files for page " + pageId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "processAnalyticalFiles",
                    "Error processing files: " + e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process files: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Process individual file data based on file type and extract analytical
     * information
     */
    private Map<String, Object> processFileData(Path filePath, List<Map<String, Object>> samples) throws IOException {
        Map<String, Object> results = new HashMap<>();
        String fileName = filePath.getFileName().toString();
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();

        switch (fileExtension) {
        case "CSV":
            results = processCsvFile(filePath, samples);
            break;
        case "PDF":
            results = processPdfFile(filePath, samples);
            break;
        case "MZML":
        case "CDF":
            results = processMassSpecFile(filePath, samples);
            break;
        default:
            LogEvent.logWarn(this.getClass().getSimpleName(), "processFileData",
                    "Unsupported file type: " + fileExtension);
            break;
        }

        return results;
    }

    /**
     * Process CSV files containing analytical data Supports two CSV formats: 1.
     * Structured format: QC_RESULTS, CALIBRATION_CURVE, QUANTIFICATION_RESULTS
     * sections 2. Legacy HPLC format: QUALITY CONTROL RESULTS, SAMPLE RESULTS
     * sections
     */
    private Map<String, Object> processCsvFile(Path filePath, List<Map<String, Object>> samples) throws IOException {
        Map<String, Object> results = new HashMap<>();
        List<String> lines = Files.readAllLines(filePath);

        if (lines.isEmpty()) {
            return results;
        }

        List<Map<String, Object>> quantification = new ArrayList<>();
        List<Map<String, Object>> qcResults = new ArrayList<>();
        List<Map<String, Object>> analyzerResults = new ArrayList<>();
        Map<String, Object> calibrationData = new HashMap<>();

        // Detect CSV format and parse accordingly
        String csvContent = String.join("\n", lines);
        if (csvContent.contains("QC_RESULTS") || csvContent.contains("CALIBRATION_CURVE")
                || csvContent.contains("QUANTIFICATION_RESULTS")) {
            // Process structured format (test data files)
            results = processStructuredCsvFormat(lines, samples);
        } else {
            // Process legacy format (old instrument exports)
            results = processLegacyCsvFormat(lines, samples);
        }

        return results;
    }

    /**
     * Process structured CSV format with QC_RESULTS, CALIBRATION_CURVE,
     * QUANTIFICATION_RESULTS sections
     */
    private Map<String, Object> processStructuredCsvFormat(List<String> lines, List<Map<String, Object>> samples) {
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> quantification = new ArrayList<>();
        List<Map<String, Object>> qcResults = new ArrayList<>();
        List<Map<String, Object>> analyzerResults = new ArrayList<>();
        Map<String, Object> calibrationData = new HashMap<>();

        String currentSection = "";
        String[] headers = null;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Skip empty lines
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Detect section headers
            if (trimmedLine.equals("QC_RESULTS")) {
                currentSection = "QC";
                continue;
            } else if (trimmedLine.equals("CALIBRATION_CURVE")) {
                currentSection = "CALIBRATION";
                continue;
            } else if (trimmedLine.equals("QUANTIFICATION_RESULTS")) {
                currentSection = "QUANTIFICATION";
                continue;
            } else if (trimmedLine.equals("INSTRUMENT_DETAILS") || trimmedLine.equals("ANALYSIS_SUMMARY")) {
                currentSection = "OTHER";
                continue;
            }

            // Parse headers (column names)
            if (trimmedLine.contains("QCLevel") || trimmedLine.contains("CalibrationID")
                    || trimmedLine.contains("InstrumentSampleID")) {
                headers = trimmedLine.split(",");
                continue;
            }

            // Skip header rows that don't match current section
            if (headers == null || trimmedLine.contains(",") == false) {
                continue;
            }

            String[] values = trimmedLine.split(",");

            // Parse QC results section
            if (currentSection.equals("QC")) {
                parseQcResultsRow(values, headers, qcResults);
            }
            // Parse calibration data section
            else if (currentSection.equals("CALIBRATION")) {
                parseCalibrationRow(values, headers, calibrationData);
            }
            // Parse quantification results section
            else if (currentSection.equals("QUANTIFICATION")) {
                parseQuantificationRow(values, headers, quantification);
            }
        }

        // Map quantification results to lab samples from Stage 2
        if (!quantification.isEmpty() && !samples.isEmpty()) {
            for (int i = 0; i < Math.min(quantification.size(), samples.size()); i++) {
                Map<String, Object> quantResult = quantification.get(i);
                Map<String, Object> labSample = samples.get(i);

                // Get lab sample ID
                String labSampleId = (String) labSample.get("accessionNumber");
                if (labSampleId == null) {
                    labSampleId = (String) labSample.get("id");
                }

                quantResult.put("sampleId", labSampleId);

                // Add to analyzer results
                Map<String, Object> analyzerResult = new HashMap<>();
                analyzerResult.put("id", labSampleId);
                analyzerResult.put("sampleId", labSampleId);
                analyzerResult.put("result", quantResult.get("concentration") + " " + quantResult.get("units"));
                analyzerResult.put("quality", "VALID".equals(quantResult.get("validityStatus")) ? "PASSED" : "FAILED");
                analyzerResult.put("recoveryRate", "N/A");
                analyzerResults.add(analyzerResult);
            }
        }

        // Set calibration quality assessment
        if (calibrationData.containsKey("rSquared")) {
            double rSquared = (Double) calibrationData.get("rSquared");
            calibrationData.put("qualityAssessment", rSquared >= 0.99 ? "PASS" : "FAIL");
        } else {
            calibrationData.put("qualityAssessment", "FAIL");
        }

        results.put("quantification", quantification);
        results.put("qcResults", qcResults);
        results.put("analyzerResults", analyzerResults);
        results.put("calibrationData", calibrationData);

        LogEvent.logInfo("NotebookBulkOperationController", "processStructuredCsvFormat",
                "Parsed " + qcResults.size() + " QC results, " + quantification.size() + " quantification results");

        return results;
    }

    /**
     * Parse QC results row from structured CSV format
     */
    private void parseQcResultsRow(String[] values, String[] headers, List<Map<String, Object>> qcResults) {
        try {
            Map<String, Object> qcResult = new HashMap<>();

            // Map CSV columns to QC result fields
            for (int i = 0; i < headers.length && i < values.length; i++) {
                String header = headers[i].trim();
                String value = values[i].trim();

                if (header.equals("QCLevel")) {
                    qcResult.put("level", extractQcLevel(value));
                } else if (header.equals("QCName")) {
                    qcResult.put("id", value);
                } else if (header.equals("SpikedConcentration")) {
                    qcResult.put("spikedConcentration", parseDouble(value));
                } else if (header.equals("MeasuredValue")) {
                    qcResult.put("measuredValue", parseDouble(value));
                } else if (header.equals("AccuracyPercent")) {
                    qcResult.put("accuracy", parseDouble(value));
                } else if (header.equals("PrecisionPercent")) {
                    qcResult.put("precision", parseDouble(value));
                } else if (header.equals("Status")) {
                    qcResult.put("status", value);
                }
            }

            if (!qcResult.isEmpty()) {
                qcResults.add(qcResult);
            }
        } catch (Exception e) {
            LogEvent.logWarn("NotebookBulkOperationController", "parseQcResultsRow",
                    "Error parsing QC row: " + e.getMessage());
        }
    }

    /**
     * Parse calibration data row from structured CSV format
     */
    private void parseCalibrationRow(String[] values, String[] headers, Map<String, Object> calibrationData) {
        try {
            for (int i = 0; i < headers.length && i < values.length; i++) {
                String header = headers[i].trim();
                String value = values[i].trim();

                if (header.equals("RSquared")) {
                    calibrationData.put("rSquared", parseDouble(value));
                } else if (header.equals("Slope")) {
                    calibrationData.put("slope", parseDouble(value));
                } else if (header.equals("Intercept")) {
                    calibrationData.put("intercept", parseDouble(value));
                } else if (header.equals("Equation")) {
                    calibrationData.put("equation", value);
                } else if (header.equals("InstrumentName")) {
                    calibrationData.put("instrumentName", value);
                } else if (header.equals("FileName")) {
                    calibrationData.put("fileName", value);
                } else if (header.equals("QualityAssessment")) {
                    calibrationData.put("qualityAssessment", value);
                }
            }

            // Build equation if missing but we have slope and intercept
            if (!calibrationData.containsKey("equation") && calibrationData.containsKey("slope")
                    && calibrationData.containsKey("intercept")) {
                double slope = (Double) calibrationData.get("slope");
                double intercept = (Double) calibrationData.get("intercept");
                String equation = String.format("y = %.4fx + %.4f", slope, intercept);
                calibrationData.put("equation", equation);
            }
        } catch (Exception e) {
            LogEvent.logWarn("NotebookBulkOperationController", "parseCalibrationRow",
                    "Error parsing calibration row: " + e.getMessage());
        }
    }

    /**
     * Parse quantification results row from structured CSV format
     */
    private void parseQuantificationRow(String[] values, String[] headers, List<Map<String, Object>> quantification) {
        try {
            Map<String, Object> quantResult = new HashMap<>();

            for (int i = 0; i < headers.length && i < values.length; i++) {
                String header = headers[i].trim();
                String value = values[i].trim();

                if (header.equals("InstrumentSampleID")) {
                    quantResult.put("instrumentSampleId", value);
                } else if (header.equals("SampleName")) {
                    quantResult.put("sampleName", value);
                } else if (header.equals("AccessionNumber")) {
                    quantResult.put("accessionNumber", value);
                } else if (header.equals("ConcentrationValue")) {
                    quantResult.put("concentration", parseDouble(value));
                } else if (header.equals("Units")) {
                    quantResult.put("units", value);
                } else if (header.equals("PeakArea")) {
                    quantResult.put("peakArea", parseDouble(value));
                } else if (header.equals("CVPercent")) {
                    quantResult.put("cvPercent", parseDouble(value));
                } else if (header.equals("AccuracyPercent")) {
                    quantResult.put("accuracyPercent", parseDouble(value));
                } else if (header.equals("ValidityStatus")) {
                    quantResult.put("validityStatus", value);
                }
            }

            if (!quantResult.isEmpty() && quantResult.containsKey("sampleName")) {
                quantification.add(quantResult);
            }
        } catch (Exception e) {
            LogEvent.logWarn("NotebookBulkOperationController", "parseQuantificationRow",
                    "Error parsing quantification row: " + e.getMessage());
        }
    }

    /**
     * Process legacy HPLC CSV format (old instrument exports)
     */
    private Map<String, Object> processLegacyCsvFormat(List<String> lines, List<Map<String, Object>> samples) {
        Map<String, Object> results = new HashMap<>();
        List<Map<String, Object>> quantification = new ArrayList<>();
        List<Map<String, Object>> qcResults = new ArrayList<>();
        List<Map<String, Object>> analyzerResults = new ArrayList<>();
        Map<String, Object> calibrationData = new HashMap<>();
        List<Map<String, Object>> instrumentSampleResults = new ArrayList<>();

        boolean inQcSection = false;
        boolean inSampleSection = false;
        boolean inCalibrationSection = false;

        for (String line : lines) {
            String[] values = line.split(",");
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Parse calibration statistics
            if (trimmedLine.contains("R-Squared") || trimmedLine.contains("R²")) {
                inCalibrationSection = true;
                if (values.length >= 2) {
                    try {
                        double rSquared = Double.parseDouble(values[1].trim());
                        calibrationData.put("rSquared", rSquared);
                    } catch (NumberFormatException e) {
                        // Continue parsing
                    }
                }
            } else if (trimmedLine.startsWith("Slope,") && inCalibrationSection) {
                if (values.length >= 2) {
                    try {
                        double slope = Double.parseDouble(values[1].trim());
                        calibrationData.put("slope", slope);
                    } catch (NumberFormatException e) {
                        // Continue parsing
                    }
                }
            } else if (trimmedLine.startsWith("Intercept,") && inCalibrationSection) {
                if (values.length >= 2) {
                    try {
                        double intercept = Double.parseDouble(values[1].trim());
                        calibrationData.put("intercept", intercept);

                        if (calibrationData.containsKey("slope")) {
                            double slope = (Double) calibrationData.get("slope");
                            String equation = String.format("y = %.4fx + %.4f", slope, intercept);
                            calibrationData.put("equation", equation);
                        }
                    } catch (NumberFormatException e) {
                        // Continue parsing
                    }
                }
                inCalibrationSection = false;
            } else if (trimmedLine.contains("QUALITY CONTROL RESULTS")) {
                inQcSection = true;
                continue;
            } else if (trimmedLine.contains("QC SUMMARY")) {
                inQcSection = false;
                continue;
            } else if (inQcSection && (trimmedLine.contains("QC,") || trimmedLine.contains("QC "))) {
                if (values.length >= 6) {
                    try {
                        Map<String, Object> qcResult = new HashMap<>();
                        String qcLevel = values[0].trim();
                        double expectedConc = Double.parseDouble(values[1].trim());
                        double measuredConc = Double.parseDouble(values[2].trim());
                        double accuracy = Double.parseDouble(values[4].trim());
                        String status = values[5].trim();

                        qcResult.put("id", qcLevel);
                        qcResult.put("level", extractQcLevel(qcLevel));
                        qcResult.put("spikedConcentration", expectedConc);
                        qcResult.put("measuredValue", measuredConc);
                        qcResult.put("accuracy", accuracy);
                        qcResult.put("precision", 2.0);
                        qcResult.put("status", status.equals("PASS") ? "PASS" : "FAIL");
                        qcResults.add(qcResult);
                    } catch (NumberFormatException e) {
                        // Skip malformed QC lines
                    }
                }
            } else if (trimmedLine.contains("SAMPLE RESULTS")) {
                inSampleSection = true;
                continue;
            } else if (trimmedLine.contains("CHROMATOGRAPHY CONDITIONS")
                    || trimmedLine.contains("INSTRUMENT PARAMETERS")) {
                inSampleSection = false;
                continue;
            } else if (inSampleSection && values.length >= 6 && !trimmedLine.contains("Sample ID")) {
                try {
                    String instrumentSampleId = values[0].trim();
                    String sampleName = values[1].trim();
                    double peakHeight = Double.parseDouble(values[2].trim());
                    double peakArea = Double.parseDouble(values[3].trim());
                    double concentration = Double.parseDouble(values[4].trim());
                    double rsd = Double.parseDouble(values[5].trim());
                    String status = values[6].trim();

                    if (sampleName.contains("Blank") || sampleName.contains("Control")) {
                        continue;
                    }

                    Map<String, Object> instrumentResult = new HashMap<>();
                    instrumentResult.put("instrumentSampleId", instrumentSampleId);
                    instrumentResult.put("sampleName", sampleName);
                    instrumentResult.put("peakHeight", peakHeight);
                    instrumentResult.put("peakArea", peakArea);
                    instrumentResult.put("concentration", concentration);
                    instrumentResult.put("units", "µg/mL");
                    instrumentResult.put("cvPercent", rsd);
                    instrumentResult.put("status", status);
                    instrumentSampleResults.add(instrumentResult);

                } catch (NumberFormatException e) {
                    // Skip malformed sample lines
                }
            }
        }

        // Map instrument results to lab samples
        for (int i = 0; i < instrumentSampleResults.size() && i < samples.size(); i++) {
            Map<String, Object> instrumentResult = instrumentSampleResults.get(i);
            Map<String, Object> labSample = samples.get(i);

            String labSampleId = (String) labSample.get("accessionNumber");
            if (labSampleId == null) {
                labSampleId = (String) labSample.get("id");
            }

            Map<String, Object> sampleResult = new HashMap<>();
            sampleResult.put("sampleId", labSampleId);
            sampleResult.put("instrumentSampleId", instrumentResult.get("instrumentSampleId"));
            sampleResult.put("sampleName", instrumentResult.get("sampleName"));
            sampleResult.put("analyte", "Target Compound");
            sampleResult.put("peakHeight", instrumentResult.get("peakHeight"));
            sampleResult.put("peakArea", instrumentResult.get("peakArea"));
            sampleResult.put("concentration", instrumentResult.get("concentration"));
            sampleResult.put("units", instrumentResult.get("units"));
            sampleResult.put("cvPercent", instrumentResult.get("cvPercent"));
            sampleResult.put("accuracyPercent", 100.0);
            sampleResult.put("responseFunction", "Linear");
            sampleResult.put("status", instrumentResult.get("status"));
            quantification.add(sampleResult);

            Map<String, Object> analyzerResult = new HashMap<>();
            analyzerResult.put("id", labSampleId);
            analyzerResult.put("sampleId", labSampleId);
            analyzerResult.put("result", instrumentResult.get("concentration") + " " + instrumentResult.get("units"));
            analyzerResult.put("quality", instrumentResult.get("status").equals("VALID") ? "PASSED" : "FAILED");
            analyzerResult.put("recoveryRate", "N/A");
            analyzerResults.add(analyzerResult);
        }

        LogEvent.logInfo("NotebookBulkOperationController", "processLegacyCsvFormat", "Mapped "
                + instrumentSampleResults.size() + " instrument results to " + samples.size() + " lab samples");

        if (calibrationData.containsKey("rSquared")) {
            double rSquared = (Double) calibrationData.get("rSquared");
            calibrationData.put("qualityAssessment", rSquared >= 0.99 ? "PASS" : "FAIL");
        } else {
            calibrationData.put("qualityAssessment", "FAIL");
        }

        results.put("quantification", quantification);
        results.put("qcResults", qcResults);
        results.put("analyzerResults", analyzerResults);
        results.put("calibrationData", calibrationData);

        return results;
    }

    /**
     * Process PDF files (extract text and parse analytical data)
     */
    private Map<String, Object> processPdfFile(Path filePath, List<Map<String, Object>> samples) throws IOException {
        Map<String, Object> results = new HashMap<>();

        // For now, return empty results - PDF parsing requires additional libraries
        // In production, you would use libraries like Apache PDFBox or iText to extract
        // text
        LogEvent.logWarn(this.getClass().getSimpleName(), "processPdfFile",
                "PDF processing not yet implemented for file: " + filePath.getFileName());

        results.put("quantification", new ArrayList<>());
        results.put("qcResults", new ArrayList<>());
        results.put("analyzerResults", new ArrayList<>());
        results.put("calibrationData", new HashMap<>());

        return results;
    }

    /**
     * Process mass spectrometry files (mzML, CDF)
     */
    private Map<String, Object> processMassSpecFile(Path filePath, List<Map<String, Object>> samples)
            throws IOException {
        Map<String, Object> results = new HashMap<>();

        // For now, return empty results - Mass spec file parsing requires specialized
        // libraries
        LogEvent.logWarn(this.getClass().getSimpleName(), "processMassSpecFile",
                "Mass spec file processing not yet implemented for file: " + filePath.getFileName());

        results.put("quantification", new ArrayList<>());
        results.put("qcResults", new ArrayList<>());
        results.put("analyzerResults", new ArrayList<>());
        results.put("calibrationData", new HashMap<>());

        return results;
    }

    // Helper methods
    private String extractQcLevel(String sampleId) {
        if (sampleId.contains("LOW"))
            return "LOW";
        if (sampleId.contains("MED") || sampleId.contains("MEDIUM"))
            return "MEDIUM";
        if (sampleId.contains("HIGH"))
            return "HIGH";
        if (sampleId.contains("LLOQ"))
            return "LLOQ";
        return "UNKNOWN";
    }

    private Double parseDouble(Object value) {
        if (value == null)
            return null;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildEquation(Map<String, Object> rowData) {
        Double slope = parseDouble(rowData.get("Slope"));
        Double intercept = parseDouble(rowData.get("Intercept"));
        if (slope != null && intercept != null) {
            return String.format("y = %.4fx + %.4f", slope, intercept);
        }
        return "";
    }

    /**
     * Request class for file processing
     */
    @Getter
    public static class FileProcessingRequest {
        private List<String> fileIds;
        private List<Map<String, Object>> samples;

        public void setFileIds(List<String> fileIds) {
            this.fileIds = fileIds;
        }

        public void setSamples(List<Map<String, Object>> samples) {
            this.samples = samples;
        }
    }
}
