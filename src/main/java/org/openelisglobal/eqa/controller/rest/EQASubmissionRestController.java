package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQACycleSubmissionService;
import org.openelisglobal.eqa.valueholder.EQACycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQASubmissionRestController extends BaseRestController {

    @Autowired
    private EQACycleSubmissionService cycleSubmissionService;

    /**
     * Manual fallback. The provider's reference is mandatory: without it a manual
     * submission is a claim rather than a record.
     */
    @PostMapping(value = "/cycles/{cycleId}/submit-manual", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PARTICIPANT)
    public ResponseEntity<?> submitManually(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestParam(required = false) Long labEnrollmentId, @RequestBody Map<String, String> body) {
        String reference = body.get("manualSubmissionReference");
        try {
            EQACycle cycle = cycleSubmissionService.submitManually(cycleId, labEnrollmentId, reference,
                    getSysUserId(request));
            return ResponseEntity.ok(Map.of("cycleId", cycle.getId(), "status", cycle.getStatus().name(), "channel",
                    "MANUAL", "manualSubmissionReference", reference.trim()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * The reviewer's release on a scheme that reviews every cycle before it goes.
     * The click sends over the automatic channel and stamps each result row, which
     * a state transition on its own does not do.
     *
     * <p>
     * Provenance is not taken from the request body: the submission is recorded
     * against the session user.
     */
    @PostMapping(value = "/cycles/{cycleId}/review-submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.PARTICIPANT)
    public ResponseEntity<?> submitAfterReview(HttpServletRequest request, @PathVariable Long cycleId) {
        try {
            EQACycle cycle = cycleSubmissionService.submitAfterReview(cycleId, getSysUserId(request));
            return ResponseEntity
                    .ok(Map.of("cycleId", cycle.getId(), "status", cycle.getStatus().name(), "channel", "FHIR"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    /** Export bundle: what a lab uploads to the provider by hand. */
    @GetMapping(value = "/cycles/{cycleId}/export-bundle", produces = "text/csv")
    public ResponseEntity<String> exportBundle(@PathVariable Long cycleId,
            @RequestParam(required = false) Long labEnrollmentId) {
        String csv = cycleSubmissionService.exportBundleCsv(cycleId, labEnrollmentId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"eqa-cycle-" + cycleId + "-results.csv\"")
                .body(csv);
    }

    /**
     * Score intake: the provider's verdicts, with the Z-score the score tiers read.
     * Provider act, so it takes the manage grant.
     */
    /**
     * Scores by file: the provider's scores CSV pasted or uploaded from My Cycles —
     * {@code {"csv": "test,analyte_name,...", "labEnrollmentId": 3}}.
     */
    @PostMapping(value = "/cycles/{cycleId}/score-intake/csv", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public ResponseEntity<?> intakeScoresCsv(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(cycleSubmissionService.intakeScoresCsv(cycleId, longField(body, "labEnrollmentId"),
                    stringField(body, "csv"), getSysUserId(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/cycles/{cycleId}/score-intake", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(EQAGuards.MANAGE)
    public ResponseEntity<?> intakeScores(HttpServletRequest request, @PathVariable Long cycleId,
            @RequestParam(required = false) Long labEnrollmentId, @RequestBody List<Map<String, Object>> scores) {
        try {
            int scored = cycleSubmissionService.intakeScores(cycleId, labEnrollmentId, scores, getSysUserId(request));
            return ResponseEntity.ok(Map.of("cycleId", cycleId, "scored", scored));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
