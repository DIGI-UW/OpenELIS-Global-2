package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
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

    /**
     * GET /rest/analyzer/errors
     *
     * List analyzer errors with filtering. Parameters are ordered logically:
     * filters first, then text search, then pagination, then sort.
     *
     * Statistics are global (independent of filters) so the dashboard always shows
     * an accurate overall picture.
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrors(@RequestParam(required = false) String analyzerId,
            @RequestParam(required = false) AnalyzerError.ErrorType errorType,
            @RequestParam(required = false) AnalyzerError.Severity severity,
            @RequestParam(required = false) AnalyzerError.ErrorStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam(required = false) String search, @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size, @RequestParam(required = false) String sort) {
        try {
            // DAO-level filtering (all non-null params combined with AND logic)
            List<AnalyzerError> errors = analyzerErrorService.getErrorsByFilters(analyzerId, errorType, severity,
                    status, startDate, endDate);

            // Text search filter (in-memory — analyzer name is eagerly fetched by DAO)
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                errors = errors.stream().filter(error -> {
                    String errorMsg = error.getErrorMessage();
                    if (errorMsg != null && errorMsg.toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    if (error.getAnalyzer() != null && error.getAnalyzer().getName() != null
                            && error.getAnalyzer().getName().toLowerCase().contains(searchLower)) {
                        return true;
                    }
                    return false;
                }).collect(java.util.stream.Collectors.toList());
            }

            // Convert errors to maps for JSON response
            List<Map<String, Object>> errorMaps = errors.stream().map(this::errorToMap)
                    .collect(java.util.stream.Collectors.toList());

            // Global statistics (independent of filters)
            Map<String, Long> statistics = analyzerErrorService.getErrorStatistics();

            // Build response
            Map<String, Object> response = new LinkedHashMap<>();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("content", errorMaps);
            data.put("totalElements", errorMaps.size());
            data.put("statistics", statistics);
            response.put("status", "success");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving analyzer errors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AnalyzerControllerHelper.wrapError("AnalyzerError not found: " + id));
            }

            return ResponseEntity.ok(AnalyzerControllerHelper.wrapResponse(errorToMap(error)));
        } catch (Exception e) {
            logger.error("Error retrieving analyzer error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/errors/{id}/acknowledge
     * 
     * Acknowledge error
     */
    @PostMapping("/errors/{id}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeError(@PathVariable String id, HttpServletRequest request) {
        try {
            String actualUserId = getSysUserId(request);
            if (actualUserId == null || actualUserId.trim().isEmpty()) {
                actualUserId = "SYSTEM";
            }
            analyzerErrorService.acknowledgeError(id, actualUserId);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Error acknowledged successfully");
            return ResponseEntity.ok(response);
        } catch (LIMSRuntimeException e) {
            logger.error("Error acknowledging analyzer error", e);
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error acknowledging analyzer error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
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

            if (!success) {
                // Reprocessing failed — return error status, not contradictory 200
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(AnalyzerControllerHelper.wrapError("Reprocessing failed for error: " + id));
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", "Message reprocessed successfully");
            return ResponseEntity.ok(AnalyzerControllerHelper.wrapResponse(data));
        } catch (LIMSRuntimeException e) {
            logger.error("Error reprocessing analyzer error", e);
            return AnalyzerControllerHelper.mapExceptionToResponse(e);
        } catch (Exception e) {
            logger.error("Error reprocessing analyzer error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/errors/batch-acknowledge
     * 
     * Acknowledge multiple errors in batch
     */
    @PostMapping("/errors/batch-acknowledge")
    public ResponseEntity<Map<String, Object>> batchAcknowledgeErrors(@RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        try {
            @SuppressWarnings("unchecked")
            List<String> errorIds = (List<String>) request.get("errorIds");
            String userId = getSysUserId(httpRequest);
            if (userId == null || userId.trim().isEmpty()) {
                userId = "SYSTEM";
            }

            if (errorIds == null || errorIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AnalyzerControllerHelper.wrapError("errorIds is required"));
            }

            int acknowledged = 0;
            List<String> failed = new ArrayList<>();
            for (String errorId : errorIds) {
                try {
                    analyzerErrorService.acknowledgeError(errorId, userId);
                    acknowledged++;
                } catch (Exception e) {
                    logger.warn("Failed to acknowledge error: {}", errorId, e);
                    failed.add(errorId);
                }
            }

            Map<String, Object> response = new LinkedHashMap<>();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("acknowledged", acknowledged);
            data.put("failed", failed);
            response.put("data", data);
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error batch acknowledging analyzer errors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * Convert AnalyzerError to Map for JSON response
     */
    private Map<String, Object> errorToMap(AnalyzerError error) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", error.getId());
        if (error.getAnalyzer() != null) {
            Map<String, Object> analyzerMap = new LinkedHashMap<>();
            analyzerMap.put("id", error.getAnalyzer().getId());
            analyzerMap.put("name", error.getAnalyzer().getName());
            map.put("analyzer", analyzerMap);
            // Also include analyzerId at top level for frontend convenience
            map.put("analyzerId", error.getAnalyzer().getId());
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
        if (error.getRawMessage() != null) {
            map.put("rawMessage", error.getRawMessage());
        }
        return map;
    }
}
