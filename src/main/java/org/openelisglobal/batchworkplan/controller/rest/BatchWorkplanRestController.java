package org.openelisglobal.batchworkplan.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.batchworkplan.form.BatchWorkplanRequest;
import org.openelisglobal.batchworkplan.form.BatchWorkplanResponse;
import org.openelisglobal.batchworkplan.form.BatchWorkplanStatusRequest;
import org.openelisglobal.batchworkplan.form.PendingBatchTestResponse;
import org.openelisglobal.batchworkplan.service.BatchWorkplanService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rest/batch-workplans")
@PreAuthorize("hasRole('RESULTS')")
public class BatchWorkplanRestController extends BaseRestController {

    private final BatchWorkplanService batchWorkplanService;

    public BatchWorkplanRestController(BatchWorkplanService batchWorkplanService) {
        this.batchWorkplanService = batchWorkplanService;
    }

    @GetMapping(value = "/pending-tests", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PendingBatchTestResponse> pendingTests(@RequestParam(name = "limit", required = false) Integer limit) {
        return batchWorkplanService.getPendingTests(limit);
    }

    @GetMapping(value = "/batches", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<BatchWorkplanResponse> batches() {
        return batchWorkplanService.getBatches();
    }

    @PostMapping(value = "/batches", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BatchWorkplanResponse> create(@RequestBody BatchWorkplanRequest request,
            HttpServletRequest httpRequest) {
        try {
            BatchWorkplanResponse response = batchWorkplanService.createBatch(request, getSysUserId(httpRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PutMapping(value = "/batches/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BatchWorkplanResponse transition(@PathVariable Long id, @RequestBody BatchWorkplanStatusRequest request,
            HttpServletRequest httpRequest) {
        try {
            return batchWorkplanService.transitionBatch(id, request == null ? null : request.getStatus(),
                    getSysUserId(httpRequest));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
