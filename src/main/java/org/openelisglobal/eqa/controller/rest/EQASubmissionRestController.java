package org.openelisglobal.eqa.controller.rest;

import java.util.Map;
import org.openelisglobal.eqa.service.EQAFhirSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa")
public class EQASubmissionRestController {

    @Autowired
    private EQAFhirSubmissionService fhirSubmissionService;

    @PostMapping(value = "/distributions/{distributionId}/submit/{organizationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> submitViaFhir(@PathVariable Long distributionId, @PathVariable Long organizationId) {
        try {
            if (fhirSubmissionService.isSubmissionLate(distributionId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Submission deadline has passed", "supervisorApprovalRequired", true,
                                "distributionId", distributionId));
            }

            Map<String, Object> result = fhirSubmissionService.submitResultsViaFhir(distributionId, organizationId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Submission failed: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/distributions/{distributionId}/submit/{organizationId}/approve-late", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> approveLateSubmission(@PathVariable Long distributionId, @PathVariable Long organizationId,
            @RequestBody Map<String, String> body) {
        try {
            String justification = body.get("justification");
            String supervisorUserId = body.get("supervisorUserId");

            if (justification == null || justification.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Justification is required for late submission approval"));
            }

            if (supervisorUserId == null || supervisorUserId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Supervisor user ID is required"));
            }

            Map<String, Object> result = fhirSubmissionService.approveLateSubmission(distributionId, organizationId,
                    justification, supervisorUserId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Late approval failed: " + e.getMessage()));
        }
    }
}
