package org.openelisglobal.notebook.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.notebook.service.NotebookBulkOperationService;
import org.openelisglobal.notebook.service.NotebookPageSampleService;
import org.openelisglobal.notebook.service.ResultCompilationService;
import org.openelisglobal.notebook.service.ResultCompilationService.ExportOptions;
import org.openelisglobal.notebook.service.ResultCompilationService.ValidationSummary;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.notebook.valueholder.NotebookPageSample.Status;
import org.openelisglobal.notebook.valueholder.ValidationStatus;
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

/**
 * REST controller for notebook bulk operations. Handles bulk data entry, value
 * application, and page progress tracking.
 *
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

    /**
     * Bulk apply values to multiple samples on a page. POST
     * /notebook/bulk/page/{pageId}/samples/apply
     *
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
     * Get progress information for a notebook page. GET
     * /notebook/bulk/page/{pageId}/progress
     *
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
        result.put("validPercentage", summary.validPercentage());
        result.put("invalidPercentage", summary.invalidPercentage());
        result.put("inconclusivePercentage", summary.inconclusivePercentage());

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
     * Record delivery of results. POST /notebook/bulk/notebook/{notebookId}/deliver
     *
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
                request.getRecipientEmail(), request.getFileId(), sysUserId);

        return ResponseEntity.ok(Map.of("success", true, "deliveryId", deliveryId, "notebookId", notebookId,
                "recipientName", request.getRecipientName()));
    }

    /**
     * Get delivery history for a notebook. GET
     * /notebook/bulk/notebook/{notebookId}/delivery-history
     *
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
     *
     * @param pageId      the notebook page ID
     * @param request     contains sampleIds, data, boxId, and wellCoordinate
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

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getBoxId() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Box ID is required for storage assignment");
            return ResponseEntity.badRequest().body(error);
        }

        // Use the new storage assignment service method that persists to
        // SampleStorageAssignment
        Map<String, Object> result = bulkOperationService.assignSamplesToStorage(pageId, request.getSampleIds(),
                request.getBoxId(), request.getWellCoordinate(), request.getData(), sysUserId);

        result.put("pageId", pageId);
        result.put("boxId", request.getBoxId());
        result.put("wellCoordinate", request.getWellCoordinate());

        if (result.containsKey("errors") && !((java.util.List<?>) result.get("errors")).isEmpty()) {
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

        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "No sample IDs provided");
            return ResponseEntity.badRequest().body(error);
        }

        if (request.getBoxId() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Box ID is required for auto-assignment");
            return ResponseEntity.badRequest().body(error);
        }

        // Use the new auto-assign storage service method that persists to
        // SampleStorageAssignment
        Map<String, Object> result = bulkOperationService.autoAssignSamplesToStorage(pageId, request.getSampleIds(),
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

        Map<String, Object> result = new HashMap<>();
        result.put("updatedCount", updatedCount);
        result.put("pageId", pageId);
        result.put("status", status.name());
        result.put("success", true);

        return ResponseEntity.ok(result);
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

    /**
     * Request body for mark complete operation.
     */
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

    /**
     * Request body for flag sample operation.
     */
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

    /**
     * Request body for bulk flag operation.
     */
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

    /**
     * Request body for delivery operation.
     */
    public static class DeliveryRequest {
        private String recipientName;
        private String recipientEmail;
        private Integer fileId;

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
    }

    /**
     * Request body for storage assignment operation.
     */
    public static class StorageAssignmentRequest {
        private List<Integer> sampleIds;
        private Map<String, Object> data;
        private Integer boxId;
        private String wellCoordinate;

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
    }

    /**
     * Request body for status update operation.
     */
    public static class StatusUpdateRequest {
        private List<Integer> sampleIds;
        private String status;

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
    }

    /**
     * Request body for auto-assign storage operation.
     */
    public static class AutoAssignStorageRequest {
        private List<Integer> sampleIds;
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
}
