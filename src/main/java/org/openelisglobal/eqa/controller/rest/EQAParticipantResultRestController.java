package org.openelisglobal.eqa.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQACycleService;
import org.openelisglobal.eqa.service.EQAParticipantResultService;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Participant-result lifecycle API (OGC-609, FR-V2.1-05). Paths follow the
 * established /rest/eqa contract style.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize("hasAuthority('qa.view.eqa') or hasRole('GLOBAL_ADMIN')")
public class EQAParticipantResultRestController extends BaseRestController {

    private final EQAParticipantResultService resultService;
    private final EQACycleService cycleService;

    public EQAParticipantResultRestController(EQAParticipantResultService resultService, EQACycleService cycleService) {
        this.resultService = resultService;
        this.cycleService = cycleService;
    }

    @GetMapping(value = "/cycles/{cycleId}/participant-results", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> resultsForCycle(@PathVariable Long cycleId,
            @RequestParam(required = false) Long labEnrollmentId) {
        return resultService.getResultDtos(cycleId, labEnrollmentId);
    }

    @PostMapping(value = "/participant-results", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.eqa.participant') or hasAnyRole('RECEPTION', 'RESULTS', 'GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> createDraft(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long cycleId = longField(body, "cycleId");
        Long roundId = longField(body, "roundId");
        Long labEnrollmentId = longField(body, "labEnrollmentId");
        Long analyteId = longField(body, "analyteId");
        if (cycleId == null || roundId == null || labEnrollmentId == null || analyteId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "cycleId, roundId, labEnrollmentId and analyteId are required"));
        }

        EQAParticipantResult draft = new EQAParticipantResult();
        draft.setCycle(cycleService.get(cycleId));
        EQARound round = new EQARound();
        round.setId(roundId);
        draft.setRound(round);
        draft.setLabEnrollmentId(labEnrollmentId);
        draft.setAnalyteId(analyteId);
        draft.setAnalysisId(longField(body, "analysisId"));
        draft.setAssignedAnalystId(longField(body, "assignedAnalystId"));
        draft.setResultValue(stringField(body, "resultValue"));
        draft.setResultUnit(stringField(body, "resultUnit"));
        draft.setEnteredBy(longOrNull(getSysUserId(request)));
        draft.setSysUserId(getSysUserId(request));

        EQAParticipantResult saved = resultService.saveDraft(draft);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", saved.getId(), "submissionStatus", saved.getSubmissionStatus().name()));
    }

    @PatchMapping(value = "/participant-results/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.eqa.participant') or hasAnyRole('RECEPTION', 'RESULTS', 'GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> transition(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String target = stringField(body, "target");
        if (target == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "target is required"));
        }
        EQASubmissionStatus targetStatus;
        try {
            targetStatus = EQASubmissionStatus.valueOf(target.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown submission status"));
        }

        EQAParticipantResult updated = resultService.transitionStatus(id, targetStatus, getSysUserId(request));
        return ResponseEntity
                .ok(Map.of("id", updated.getId(), "submissionStatus", updated.getSubmissionStatus().name()));
    }

    /** Score intake is a provider/officer act, so it takes the manage grant. */
    @PatchMapping(value = "/participant-results/{id}/score", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('qa.manage.eqa') or hasRole('GLOBAL_ADMIN')")
    public ResponseEntity<Map<String, Object>> score(HttpServletRequest request, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String performance = stringField(body, "performance");
        if (performance == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "performance is required"));
        }
        EQAPerformanceStatus verdict;
        try {
            verdict = EQAPerformanceStatus.valueOf(performance.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown performance status"));
        }

        EQAParticipantResult scored = resultService.recordScore(id, verdict, longField(body, "eqaResultId"),
                getSysUserId(request));
        return ResponseEntity.ok(Map.of("id", scored.getId(), "submissionStatus", scored.getSubmissionStatus().name()));
    }

    private String stringField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long longField(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number");
        }
    }

    private Long longOrNull(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(ObjectNotFoundException e) {
        return Map.of("error", "EQA participant result or cycle not found");
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleIllegalMove(IllegalStateException e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> handleBadInput(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    /**
     * analyteId / labEnrollmentId / analysisId are raw FK columns (unlike cycle and
     * round, which are pre-resolved), and (round, enrollment, analyte) is unique —
     * surface those DB refusals as a conflict instead of a bare 500 (UAT finding,
     * 2026-08-18). Narrowed to constraint violations so a real DB failure still
     * reads as a 500.
     */
    @ExceptionHandler(org.hibernate.exception.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConstraintViolation(org.hibernate.exception.ConstraintViolationException e) {
        return Map.of("error", "The result conflicts with existing data: a referenced record (analyte, enrollment,"
                + " analysis) does not exist, or a result for this round, enrollment and analyte already exists");
    }
}
