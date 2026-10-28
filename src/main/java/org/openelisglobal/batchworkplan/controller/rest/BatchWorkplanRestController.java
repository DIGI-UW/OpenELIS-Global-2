package org.openelisglobal.batchworkplan.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.batchworkplan.form.BatchGenerateRequest;
import org.openelisglobal.batchworkplan.form.BatchQcStatusResponse;
import org.openelisglobal.batchworkplan.form.BatchReagentRequest;
import org.openelisglobal.batchworkplan.form.BatchSetupOptionsResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanStatusRequest;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.service.BatchReleaseRefusedException;
import org.openelisglobal.batchworkplan.service.BatchWorkplanNotFoundException;
import org.openelisglobal.batchworkplan.service.BatchWorkplanService;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qa.security.QaPermissions;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/batch-workplans")
@PreAuthorize("hasRole('RESULTS')")
public class BatchWorkplanRestController extends BaseRestController {

    private final BatchWorkplanService batchWorkplanService;

    public BatchWorkplanRestController(BatchWorkplanService batchWorkplanService) {
        this.batchWorkplanService = batchWorkplanService;
    }

    @GetMapping(value = "/pending-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PendingBatchTestResponse> pendingTests(@RequestParam(name = "limit", required = false) Integer limit,
            HttpServletRequest httpRequest) {
        return batchWorkplanService.getPendingTests(limit, getSysUserId(httpRequest));
    }

    /**
     * The caller's batches. Lifecycle housekeeping runs first, in its own
     * transaction, so a batch whose tests were all validated since the last visit
     * is listed as completed (BR-009).
     */
    @GetMapping(value = "/batches", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<BatchWorkplanResponse> batches(HttpServletRequest httpRequest) {
        String sysUserId = getSysUserId(httpRequest);
        batchWorkplanService.refreshLifecycle(sysUserId);
        return batchWorkplanService.getBatches(sysUserId);
    }

    @PostMapping(value = "/batches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchWorkplanResponse> create(@RequestBody BatchWorkplanRequest request,
            HttpServletRequest httpRequest) {
        BatchWorkplanResponse response = batchWorkplanService.createBatch(request, getSysUserId(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/batches/archive-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> archiveAll(HttpServletRequest httpRequest) {
        return Map.of("archived", batchWorkplanService.archiveAll(getSysUserId(httpRequest)));
    }

    @GetMapping(value = "/batches/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse batch(@PathVariable Long id, HttpServletRequest httpRequest) {
        return batchWorkplanService.getBatch(id, getSysUserId(httpRequest));
    }

    @PutMapping(value = "/batches/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse update(@PathVariable Long id, @RequestBody BatchWorkplanRequest request,
            HttpServletRequest httpRequest) {
        return batchWorkplanService.updateBatch(id, request, getSysUserId(httpRequest));
    }

    @PutMapping(value = "/batches/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse transition(@PathVariable Long id, @RequestBody BatchWorkplanStatusRequest request,
            HttpServletRequest httpRequest) {
        return batchWorkplanService.transitionBatch(id, request == null ? null : request.getStatus(),
                getSysUserId(httpRequest));
    }

    @GetMapping(value = "/batches/{id}/setup-options", produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchSetupOptionsResponse setupOptions(@PathVariable Long id, HttpServletRequest httpRequest) {
        return batchWorkplanService.getSetupOptions(id, getSysUserId(httpRequest));
    }

    @PutMapping(value = "/batches/{id}/reagent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse assignReagent(@PathVariable Long id, @RequestBody BatchReagentRequest request,
            HttpServletRequest httpRequest) {
        return batchWorkplanService.assignReagent(id, request, getSysUserId(httpRequest));
    }

    @GetMapping(value = "/batches/{id}/qc-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchQcStatusResponse qcStatus(@PathVariable Long id, HttpServletRequest httpRequest) {
        return batchWorkplanService.getQcStatus(id, getSysUserId(httpRequest));
    }

    /**
     * Release a batch and stamp it for printing. A refused release answers 409 with
     * the QC standing (the page shows the override dialog from it), or 403 when the
     * caller confirmed an override they may not make.
     */
    @PostMapping(value = "/batches/{id}/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse generate(@PathVariable Long id,
            @RequestBody(required = false) BatchGenerateRequest request, HttpServletRequest httpRequest) {
        return batchWorkplanService.generate(id, request != null && request.override(), mayOverrideQc(),
                getSysUserId(httpRequest));
    }

    @ExceptionHandler(BatchWorkplanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(BatchWorkplanNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(BatchReleaseRefusedException.class)
    public ResponseEntity<Map<String, Object>> refused(BatchReleaseRefusedException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("reason", e.getReason().name());
        body.put("qc", e.getQc());
        HttpStatus status = e.getReason() == BatchReleaseRefusedException.Reason.OVERRIDE_NOT_PERMITTED
                ? HttpStatus.FORBIDDEN
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(body);
    }

    private boolean mayOverrideQc() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if (QaPermissions.KEY_QC_OVERRIDE.equals(authority.getAuthority())
                    || "ROLE_GLOBAL_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
