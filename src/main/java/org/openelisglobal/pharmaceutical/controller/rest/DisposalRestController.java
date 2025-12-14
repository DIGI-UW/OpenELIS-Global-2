package org.openelisglobal.pharmaceutical.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.pharmaceutical.service.DisposalWorkflowService;
import org.openelisglobal.pharmaceutical.valueholder.DisposalRecord;
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
@RequestMapping("/rest/pharmaceutical/disposal")
public class DisposalRestController extends BaseRestController {

    @Autowired
    private DisposalWorkflowService disposalWorkflowService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DisposalRecord>> getAll() {
        try {
            List<DisposalRecord> records = disposalWorkflowService.getAll();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> getById(@PathVariable Integer id) {
        try {
            DisposalRecord record = disposalWorkflowService.get(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample/{sampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DisposalRecord>> getBySampleId(@PathVariable Integer sampleId) {
        try {
            List<DisposalRecord> records = disposalWorkflowService.findBySampleId(sampleId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/status/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DisposalRecord>> getByStatus(@PathVariable DisposalRecord.DisposalStatus status) {
        try {
            List<DisposalRecord> records = disposalWorkflowService.findByStatus(status);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/pending-approvals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DisposalRecord>> getPendingApprovals() {
        try {
            List<DisposalRecord> records = disposalWorkflowService.findPendingApprovals();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/sample/{sampleId}/can-dispose", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalEligibilityResponse> canRequestDisposal(@PathVariable Integer sampleId) {
        try {
            boolean canDispose = disposalWorkflowService.canRequestDisposal(sampleId);
            return ResponseEntity.ok(new DisposalEligibilityResponse(canDispose));
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/request", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> requestDisposal(
            @RequestBody DisposalRequest disposalRequest,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            DisposalRecord record = disposalWorkflowService.requestDisposal(
                    disposalRequest.getSampleId(),
                    disposalRequest.getReason(),
                    disposalRequest.getMethod(),
                    disposalRequest.getJustification(),
                    userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> approve(
            @PathVariable Integer id,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            DisposalRecord approvedRecord = disposalWorkflowService.approveDisposal(id, userId);
            return ResponseEntity.ok(approvedRecord);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> reject(
            @PathVariable Integer id,
            @RequestParam String rejectionReason,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            DisposalRecord rejectedRecord = disposalWorkflowService.rejectDisposal(id, rejectionReason, userId);
            return ResponseEntity.ok(rejectedRecord);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{id}/execute", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> execute(
            @PathVariable Integer id,
            @RequestBody DisposalExecutionRequest executionRequest,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            DisposalRecord executedRecord = disposalWorkflowService.executeDisposal(
                    id,
                    executionRequest.getWitnessId(),
                    executionRequest.getDisposalNotes(),
                    userId);
            return ResponseEntity.ok(executedRecord);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DisposalRecord> schedule(
            @PathVariable Integer id,
            @RequestParam Long scheduledTimestamp,
            HttpServletRequest request) {
        try {
            UserSessionData usd = (UserSessionData) request.getSession().getAttribute(USER_SESSION_DATA);
            String userId = String.valueOf(usd.getSystemUserId());

            DisposalRecord scheduledRecord = disposalWorkflowService.scheduleDisposal(
                    id,
                    new Timestamp(scheduledTimestamp),
                    userId);
            return ResponseEntity.ok(scheduledRecord);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/certificate", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateCertificate(@PathVariable Integer id) {
        try {
            String certificate = disposalWorkflowService.generateDisposalCertificate(id);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            disposalWorkflowService.delete(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LogEvent.logError(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Setter
    @Getter
    public static class DisposalRequest {
        private Integer sampleId;
        private DisposalRecord.DisposalReason reason;
        private DisposalRecord.DisposalMethod method;
        private String justification;
    }

    @Setter
    @Getter
    public static class DisposalExecutionRequest {
        private String witnessId;
        private String disposalNotes;
    }

    @Getter
    public static class DisposalEligibilityResponse {
        private Boolean canDispose;

        public DisposalEligibilityResponse(Boolean canDispose) {
            this.canDispose = canDispose;
        }
    }
}
