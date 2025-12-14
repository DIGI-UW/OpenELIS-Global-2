package org.openelisglobal.pharmaceutical.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.service.AssayRunService;
import org.openelisglobal.pharmaceutical.valueholder.AssayRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/pharmaceutical/assay-runs")
public class AssayRunRestController extends BaseRestController {

    @Autowired
    private AssayRunService assayRunService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getAll() {
        try {
            List<AssayRun> assayRuns = assayRunService.getAll();
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> getById(@PathVariable Integer id) {
        try {
            AssayRun assayRun = assayRunService.get(id);
            if (assayRun == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(assayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getBySampleId(@PathVariable Integer sampleId) {
        try {
            List<AssayRun> assayRuns = assayRunService.findBySampleId(sampleId);
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getByStatus(@PathVariable AssayRun.AssayStatus status) {
        try {
            List<AssayRun> assayRuns = assayRunService.findByStatus(status);
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/pending-review", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getPendingReview() {
        try {
            List<AssayRun> assayRuns = assayRunService.findPendingReview();
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/oos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getOosAssayRuns(@RequestParam(defaultValue = "true") Boolean oosFlag) {
        try {
            List<AssayRun> assayRuns = assayRunService.findByOosFlag(oosFlag);
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/notebook/{notebookPageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AssayRun>> getByNotebookPageId(@PathVariable Integer notebookPageId) {
        try {
            List<AssayRun> assayRuns = assayRunService.findByNotebookPageId(notebookPageId);
            return ResponseEntity.ok(assayRuns);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/can-approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApprovalStatusResponse> canApprove(@PathVariable Integer id) {
        try {
            boolean canApprove = assayRunService.canApprove(id);
            return ResponseEntity.ok(new ApprovalStatusResponse(canApprove));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/sample/{sampleId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> initiateAssayRun(
            @PathVariable Integer sampleId,
            @Valid @RequestBody AssayRun assayRun,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun initiatedAssayRun = assayRunService.initiateAssayRun(sampleId, assayRun, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(initiatedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/results", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> recordResults(
            @PathVariable Integer id,
            @Valid @RequestBody AssayResultsRequest resultsRequest,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun updatedAssayRun = assayRunService.recordAssayResults(
                    id,
                    resultsRequest.getRawResults(),
                    resultsRequest.getCalculatedResults(),
                    resultsRequest.getOosFlag(),
                    userId);
            return ResponseEntity.ok(updatedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/submit-review", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> submitForReview(
            @PathVariable Integer id,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun updatedAssayRun = assayRunService.submitForReview(id, userId);
            return ResponseEntity.ok(updatedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> approve(
            @PathVariable Integer id,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun approvedAssayRun = assayRunService.approveAssayRun(id, userId);
            return ResponseEntity.ok(approvedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> reject(
            @PathVariable Integer id,
            @RequestParam String rejectionReason,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun rejectedAssayRun = assayRunService.rejectAssayRun(id, rejectionReason, userId);
            return ResponseEntity.ok(rejectedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/link-notebook", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssayRun> linkToNotebook(
            @PathVariable Integer id,
            @RequestParam Integer notebookPageId,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            AssayRun linkedAssayRun = assayRunService.linkToNotebook(id, notebookPageId, userId);
            return ResponseEntity.ok(linkedAssayRun);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            assayRunService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class AssayResultsRequest {
        private String rawResults;
        private String calculatedResults;
        private Boolean oosFlag;
    }

    @Getter
    public static class ApprovalStatusResponse {
        private Boolean canApprove;

        public ApprovalStatusResponse(Boolean canApprove) {
            this.canApprove = canApprove;
        }
    }
}
