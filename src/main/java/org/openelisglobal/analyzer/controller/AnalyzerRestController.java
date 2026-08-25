package org.openelisglobal.analyzer.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerErrorService;
import org.openelisglobal.analyzer.service.AnalyzerFieldService;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.Analyzer.AnalyzerStatus;
import org.openelisglobal.analyzer.valueholder.AnalyzerError;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Analyzer management. Handles CRUD operations for
 * analyzers using the 2-table model (Analyzer + AnalyzerType).
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerRestController.class);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerFieldService analyzerFieldService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerQueryService analyzerQueryService;

    @Autowired
    private org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService analyzerOrderDispatchService;

    @Autowired
    private AnalyzerErrorService analyzerErrorService;

    /**
     * GET /rest/analyzer/analyzers/{id}/fields Get all fields for an analyzer.
     */
    @GetMapping("/analyzers/{id}/fields")
    public ResponseEntity<List<Map<String, Object>>> getFields(@PathVariable String id) {
        try {
            List<org.openelisglobal.analyzer.valueholder.AnalyzerField> fields = analyzerFieldService
                    .getFieldsByAnalyzerId(id);
            List<Map<String, Object>> response = new ArrayList<>();
            for (org.openelisglobal.analyzer.valueholder.AnalyzerField field : fields) {
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                fieldMap.put("id", field.getId());
                fieldMap.put("fieldName", field.getFieldName());
                fieldMap.put("astmRef", field.getAstmRef());
                fieldMap.put("fieldType", field.getFieldType() != null ? field.getFieldType().toString() : null);
                fieldMap.put("unit", field.getUnit());
                fieldMap.put("isActive", field.getIsActive());
                response.add(fieldMap);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving fields for analyzer: {}", id, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/query Start an asynchronous query job for
     * an analyzer.
     */
    @PostMapping("/analyzers/{id}/query")
    public ResponseEntity<Map<String, Object>> queryAnalyzer(@PathVariable String id) {
        try {
            String jobId = analyzerQueryService.startQuery(id);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("jobId", jobId);
            response.put("analyzerId", id);
            response.put("status", "started");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (LIMSRuntimeException e) {
            // Push-only analyzers or missing TCP config → 422
            logger.warn("Cannot query analyzer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error starting query job for analyzer: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * POST /rest/analyzer/analyzers/{id}/send-order Dispatch an outbound LIS-
     * initiated order to the given analyzer via the bridge.
     *
     * <p>
     * Body: {@code { accessionNumber: string, patientId?: string, testCodes:
     * string[] }}. Returns HTTP 200 on successful bridge accept, 502 on bridge-side
     * failure (failed ACK, connection refused), 400 on validation, 422 on
     * configuration problems (missing IP/port, missing bridge URL).
     */
    @PostMapping("/analyzers/{id}/send-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendOrder(@PathVariable String id,
            @RequestBody Map<String, Object> body) {
        // OE2 is analyzer-agnostic: it sends only {accessionNumber}. The backend
        // resolves the accession's ordered tests → their LOINCs and posts a
        // LOINC order to the bridge, which owns LOINC→analyzer-code + message
        // building. No test codes cross this boundary.
        String accessionNumber = body.get("accessionNumber") instanceof String s ? s : null;
        try {
            org.openelisglobal.analyzer.service.AnalyzerOrderDispatchService.DispatchResult result = analyzerOrderDispatchService
                    .dispatchOrder(id, accessionNumber);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", result.success ? "DISPATCHED" : "FAILED");
            response.put("protocol", result.protocol);
            response.put("analyzerId", id);
            response.put("accessionNumber", accessionNumber);
            response.put("loincCodes", result.loincCodes);
            if (!result.success) {
                response.put("error", result.error);
            }
            return result.success ? ResponseEntity.ok(response)
                    : ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (java.io.IOException e) {
            logger.warn("Bridge IO failure dispatching order for analyzer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error dispatching order for analyzer {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AnalyzerControllerHelper.wrapError(e.getMessage()));
        }
    }

    /**
     * GET /rest/analyzer/analyzers/{id}/query/{jobId}/status Get query job status.
     */
    @GetMapping("/analyzers/{id}/query/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getQueryStatus(@PathVariable String id, @PathVariable String jobId) {
        try {
            Map<String, Object> status = analyzerQueryService.getStatus(id, jobId);
            if (status == null) {
                Map<String, Object> error = new LinkedHashMap<>();
                error.put("error", "Query job not found or expired: " + jobId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting query status for analyzer: {}, job: {}", id, jobId, e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * POST /rest/analyzer/discovered-sources Report an unknown analyzer source
     * discovered by the bridge. Creates a PENDING_REGISTRATION stub if no analyzer
     * with this sourceId exists. Idempotent via UNIQUE constraint on
     * discovered_source_id: duplicate inserts return the existing stub.
     */
    @PostMapping("/discovered-sources")
    public ResponseEntity<Map<String, Object>> reportDiscoveredSource(@RequestBody Map<String, String> body) {
        String sourceId = body.get("sourceId");
        String protocol = body.get("protocol");
        String protocolHint = body.get("protocolHint");
        String transport = body.get("transport");

        if (sourceId == null || sourceId.isBlank()) {
            return ResponseEntity.badRequest().body(AnalyzerControllerHelper.wrapError("sourceId is required"));
        }

        // Build display name with length safety (Analyzer.name is VARCHAR(100))
        String displayName = (protocolHint != null && !protocolHint.isBlank()) ? protocolHint
                : "Unknown (" + sourceId + ")";
        if (displayName.length() > 100) {
            displayName = displayName.substring(0, 97) + "...";
        }

        Analyzer stub = new Analyzer();
        stub.ensureFhirUuid();
        stub.setName(displayName);
        stub.setStatus(AnalyzerStatus.PENDING_REGISTRATION);
        stub.setDiscoveredSourceId(sourceId);

        // Try insert. UNIQUE index on discovered_source_id handles races.
        // On duplicate, catch the constraint violation and return existing stub.
        String analyzerId;
        try {
            analyzerId = analyzerService.insert(stub);
        } catch (Exception e) {
            if (isDuplicateKeyViolation(e)) {
                Optional<Analyzer> existing = analyzerService.findByDiscoveredSourceId(sourceId);
                if (existing.isPresent()) {
                    Analyzer found = existing.get();
                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("analyzerId", found.getId());
                    response.put("status", found.getStatus().name());
                    response.put("alreadyExists", true);
                    return ResponseEntity.ok(response);
                }
            }
            throw e;
        }

        // Error dashboard entry — best-effort (stub is the critical data)
        try {
            Analyzer created = analyzerService.get(analyzerId);
            String errorMsg = String.format(
                    "Unregistered source discovered: sourceId=%s, protocol=%s, transport=%s, hint=%s", sourceId,
                    protocol, transport, protocolHint);
            analyzerErrorService.createError(created, AnalyzerError.ErrorType.UNREGISTERED_SOURCE,
                    AnalyzerError.Severity.WARNING, errorMsg, null);
        } catch (Exception e) {
            logger.warn("Failed to create error entry for discovered source {}: {}", sourceId, e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("analyzerId", analyzerId);
        response.put("status", AnalyzerStatus.PENDING_REGISTRATION.name());
        response.put("alreadyExists", false);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private boolean isDuplicateKeyViolation(Throwable e) {
        while (e != null) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("duplicate key") || msg.contains("unique constraint"))) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

}
