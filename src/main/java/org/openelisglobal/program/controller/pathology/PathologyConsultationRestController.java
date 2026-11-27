package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.program.service.ConsultationAnswer;
import org.openelisglobal.program.service.ConsultationDraft;
import org.openelisglobal.program.service.PathologyCaseRuleException;
import org.openelisglobal.program.service.PathologyConsultationConflictException;
import org.openelisglobal.program.service.PathologyConsultationService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.pathology.PathologyConsultation;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asks a pathologist for a second opinion on a case, records the outcome, or
 * withdraws the request (FR-11), each as a request of its own rather than a
 * field of the case save, so a second opinion keeps its id, who asked whom and
 * when, and never holds or changes the case.
 *
 * <p>
 * Each answers the second opinion's id and status; the screen reads the case
 * again for the rest. A refusal answers 400 with its message id, an unknown
 * case or one outside the user's units 404, a second opinion no longer awaited
 * 409 {@code conflict: "consultation"}, and one that lost a race to another
 * change or to a second request for the same colleague 409
 * {@code conflict: "version"}.
 */
@RestController
@RequestMapping(value = "/rest/pathology/caseView/{pathologySampleId}/consultations", produces = MediaType.APPLICATION_JSON_VALUE)
public class PathologyConsultationRestController extends PathologyRestController {

    private final PathologyConsultationService pathologyConsultationService;
    private final PathologySampleService pathologySampleService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;

    public PathologyConsultationRestController(PathologyConsultationService pathologyConsultationService,
            PathologySampleService pathologySampleService, ProgramSampleLabUnitScope programSampleLabUnitScope) {
        this.pathologyConsultationService = pathologyConsultationService;
        this.pathologySampleService = pathologySampleService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> requestConsultation(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) ConsultationRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(pathologyConsultationService.request(pathologySampleId, body == null ? null : body.toDraft(),
                    sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    @PostMapping("/{consultationId}/answer")
    public ResponseEntity<Map<String, Object>> answerConsultation(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @PathVariable("consultationId") Integer consultationId, @RequestBody(required = false) OutcomeRequest body,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(pathologyConsultationService.answer(pathologySampleId, consultationId,
                    body == null ? null : body.toAnswer(), sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    @PostMapping("/{consultationId}/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawConsultation(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @PathVariable("consultationId") Integer consultationId, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(pathologyConsultationService.withdraw(pathologySampleId, consultationId, sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    /**
     * The page showed a second opinion still awaited that has since been closed.
     */
    @ExceptionHandler(PathologyConsultationConflictException.class)
    public ResponseEntity<Map<String, Object>> consultationConflict(PathologyConsultationConflictException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conflict", "consultation");
        body.put("status", e.getStoredStatus() == null ? null : e.getStoredStatus().name());
        body.put("error", "the second opinion is no longer awaited; reload the case");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * A reason that names no entry of the list the page offered: the page is out of
     * date, so the caller is told to reload, as the case save answers it.
     */
    @ExceptionHandler(PathologyCaseRuleException.class)
    public ResponseEntity<Map<String, Object>> caseRule(PathologyCaseRuleException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * Two requests to one colleague that cross store one row, through a unique
     * index on the awaited ones, and an answer or withdrawal racing another loses
     * the row's version check; either is told so rather than failing. The race can
     * surface wrapped by the data layer, so the whole cause chain is read.
     */
    private static ResponseEntity<Map<String, Object>> conflictOrRethrow(RuntimeException e) {
        if (PathologyController.concurrentSaveConflict(e)) {
            return versionConflict("second opinion");
        }
        throw e;
    }

    /** The second opinion's id and status as they now stand. */
    private static ResponseEntity<Map<String, Object>> answer(PathologyConsultation stored) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", stored.getId());
        body.put("status", stored.getStatus() == null ? null : stored.getStatus().name());
        return ResponseEntity.ok(body);
    }

    /**
     * A second opinion as the pathologist asks for it; the service checks each
     * value.
     */
    public static class ConsultationRequest {
        public String consultedUserId;
        public String reasonId;
        public Boolean preTreatment;

        ConsultationDraft toDraft() {
            return new ConsultationDraft(consultedUserId, reasonId, preTreatment);
        }
    }

    /** The outcome as posted, by name, and the note. */
    public static class OutcomeRequest {
        public String outcome;
        public String note;

        ConsultationAnswer toAnswer() {
            return new ConsultationAnswer(outcome, note);
        }
    }
}
