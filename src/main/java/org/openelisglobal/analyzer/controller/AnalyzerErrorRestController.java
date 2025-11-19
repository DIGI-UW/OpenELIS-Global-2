package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Analyzer Error management
 * 
 * Task Reference: T095
 * 
 * Handles operations for error dashboard and reprocessing workflow: - List
 * errors with filtering - Get error by ID - Acknowledge errors - Reprocess
 * errors - Batch acknowledge
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerErrorRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerErrorRestController.class);

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * GET /rest/analyzer/errors
     * 
     * List analyzer errors with filtering and pagination
     * 
     * Query Parameters: - page, size, search, errorType, severity, status,
     * analyzerId, startDate, endDate, sort
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrors(@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String search,
            @RequestParam(required = false) String errorType, @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status, @RequestParam(required = false) String analyzerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam(required = false) String sort) {
        try {
            // Parse enum values
            AnalyzerError.ErrorType errorTypeEnum = errorType != null
                    ? AnalyzerError.ErrorType.valueOf(errorType.toUpperCase())
                    : null;
            AnalyzerError.Severity severityEnum = severity != null
                    ? AnalyzerError.Severity.valueOf(severity.toUpperCase())
                    : null;
            AnalyzerError.ErrorStatus statusEnum = status != null
                    ? AnalyzerError.ErrorStatus.valueOf(status.toUpperCase())
                    : null;

            // Get filtered errors
            List<AnalyzerError> errors = analyzerErrorService.getErrorsByFilters(analyzerId, errorTypeEnum,
                    severityEnum, statusEnum, startDate, endDate);

            // Apply search filter if provided
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                errors = errors.stream().filter(error -> {
                    String errorMsg = error.getErrorMessage();
                    if (errorMsg != null && errorMsg.toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    // Note: Accessing analyzer.name may cause LazyInitializationException
                    // In production, we'd eagerly fetch analyzer in DAO query
                    try {
                        if (error.getAnalyzer() != null && error.getAnalyzer().getName() != null
                                && error.getAnalyzer().getName().toLowerCase().contains(searchLower)) {
                            return true;
                        }
                    } catch (Exception e) {
                        // If lazy loading fails, skip analyzer name check
                    }
                    return false;
                }).collect(java.util.stream.Collectors.toList());
            }

            // Calculate statistics - use current filtered results for now
            // TODO: Implement proper statistics query in DAO
            List<AnalyzerError> allErrors = errors; // Use filtered results for statistics
            long totalErrors = allErrors.size();
            long unacknowledged = allErrors.stream()
                    .filter(e -> e.getStatus() == AnalyzerError.ErrorStatus.UNACKNOWLEDGED).count();
            long critical = allErrors.stream().filter(e -> e.getSeverity() == AnalyzerError.Severity.CRITICAL).count();
            long last24Hours = allErrors.stream().filter(e -> {
                if (e.getLastupdated() == null)
                    return false;
                long hoursAgo = (System.currentTimeMillis() - e.getLastupdated().getTime()) / (1000 * 60 * 60);
                return hoursAgo <= 24;
            }).count();

            // Build response
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("content", errors);
            data.put("totalElements", errors.size());
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalErrors", totalErrors);
            statistics.put("unacknowledged", unacknowledged);
            statistics.put("critical", critical);
            statistics.put("last24Hours", last24Hours);
            data.put("statistics", statistics);
            response.put("data", data);
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer errors", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /rest/analyzer/errors/{id}
     * 
     * Get error by ID
     */
    @GetMapping("/errors/{id}")
    public ResponseEntity<Map<String, Object>> getError(@PathVariable String id) {
        try {
            AnalyzerError error = analyzerErrorService.getErrorById(id);

            if (error == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "AnalyzerError not found: " + id);
                errorResponse.put("status", "error");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", errorToMap(error));
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/errors/{id}/acknowledge
     * 
     * Acknowledge error
     */
    @PostMapping("/errors/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeError(@PathVariable String id,
            @RequestParam(required = false) String userId) {
        try {
            String actualUserId = userId != null ? userId : "SYSTEM"; // TODO: Get from security context
            analyzerErrorService.acknowledgeError(id, actualUserId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Error acknowledged successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error acknowledging analyzer error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            logger.error("Error acknowledging analyzer error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/errors/{id}/reprocess
     * 
     * Reprocess error message after mapping created
     */
    @PostMapping("/errors/{id}/reprocess")
    public ResponseEntity<Map<String, Object>> reprocessError(@PathVariable String id) {
        try {
            boolean success = analyzerErrorService.reprocessError(id);

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("success", success);
            data.put("message", success ? "Message reprocessed successfully" : "Reprocessing failed");
            response.put("data", data);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error reprocessing analyzer error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            logger.error("Error reprocessing analyzer error", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/errors/batch-acknowledge
     * 
     * Acknowledge multiple errors in batch
     */
    @PostMapping("/errors/batch-acknowledge")
    public ResponseEntity<Map<String, Object>> batchAcknowledgeErrors(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> errorIds = (List<String>) request.get("errorIds");
            String userId = request.containsKey("userId") ? (String) request.get("userId") : "SYSTEM"; // TODO: Get from
                                                                                                       // security
                                                                                                       // context

            if (errorIds == null || errorIds.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "errorIds is required");
                error.put("status", "error");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            int acknowledged = 0;
            List<String> failed = new ArrayList<>();
            for (String errorId : errorIds) {
                try {
                    analyzerErrorService.acknowledgeError(errorId, userId);
                    acknowledged++;
                } catch (Exception e) {
                    logger.warn("Failed to acknowledge error: " + errorId, e);
                    failed.add(errorId);
                }
            }

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("acknowledged", acknowledged);
            data.put("failed", failed);
            response.put("data", data);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error batch acknowledging analyzer errors", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Convert AnalyzerError to Map for JSON response
     */
    private Map<String, Object> errorToMap(AnalyzerError error) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", error.getId());
        if (error.getAnalyzer() != null) {
            Map<String, Object> analyzerMap = new HashMap<>();
            analyzerMap.put("id", error.getAnalyzer().getId());
            analyzerMap.put("name", error.getAnalyzer().getName());
            map.put("analyzer", analyzerMap);
        }
        map.put("errorType", error.getErrorType().name());
        map.put("severity", error.getSeverity().name());
        map.put("errorMessage", error.getErrorMessage());
        map.put("status", error.getStatus().name());
        if (error.getLastupdated() != null) {
            map.put("timestamp", error.getLastupdated().toInstant().toString());
        }
        if (error.getAcknowledgedBy() != null) {
            map.put("acknowledgedBy", error.getAcknowledgedBy());
        }
        if (error.getAcknowledgedAt() != null) {
            map.put("acknowledgedAt", error.getAcknowledgedAt().toInstant().toString());
        }
        return map;
    }
}
