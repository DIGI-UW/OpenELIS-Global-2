package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.openelisglobal.program.service.PathologyRequestConflictException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.service.RequestDraft;
import org.openelisglobal.program.service.RequestStatusChange;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
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
 * Raises a bench request on a case, and completes, cancels or reopens one
 * (FR-10), each as a request of its own rather than a field of the case save,
 * so a request keeps its id, who raised it and when, and a status changed in
 * place. A status change carries the status the page showed, so a page another
 * user has overtaken is told so instead of undoing their change.
 *
 * <p>
 * Both answer the request's id and status; the screen reads the case again for
 * the rest. A refusal answers 400 with its message id, an unknown case or one
 * outside the user's units 404, a page showing a status the request has left
 * 409 {@code conflict: "request"}, and a change that lost the race for the case
 * to a move, a save or another change 409 {@code conflict: "version"}.
 */
@RestController
@RequestMapping(value = "/rest/pathology/caseView/{pathologySampleId}/requests", produces = MediaType.APPLICATION_JSON_VALUE)
public class PathologyRequestRestController extends PathologyRestController {

    private final PathologySampleService pathologySampleService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;

    public PathologyRequestRestController(PathologySampleService pathologySampleService,
            ProgramSampleLabUnitScope programSampleLabUnitScope) {
        this.pathologySampleService = pathologySampleService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
    }

    @PostMapping("")
    public ResponseEntity<Map<String, Object>> raiseRequest(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) RaiseRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(pathologySampleService.raiseRequest(pathologySampleId, body == null ? null : body.toDraft(),
                    sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    @PostMapping("/{requestId}/status")
    public ResponseEntity<Map<String, Object>> changeRequestStatus(
            @PathVariable("pathologySampleId") Integer pathologySampleId, @PathVariable("requestId") Integer requestId,
            @RequestBody(required = false) StatusChangeRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(pathologySampleService.changeRequestStatus(pathologySampleId, requestId,
                    body == null ? null : body.toChange(), sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    /** The page showed a status the request has since left. */
    @ExceptionHandler(PathologyRequestConflictException.class)
    public ResponseEntity<Map<String, Object>> requestConflict(PathologyRequestConflictException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conflict", "request");
        body.put("status", e.getStoredStatus() == null ? null : e.getStoredStatus().name());
        body.put("error", "the request is no longer at the status shown; reload the case");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * A change takes the case's row and raises its version before it writes, so one
     * racing a move, a save or another change usually loses on the case, its
     * version check or a lock the database would not grant, and is told so rather
     * than failing; the race can surface wrapped by the data layer, so the whole
     * cause chain is read.
     */
    private static ResponseEntity<Map<String, Object>> conflictOrRethrow(RuntimeException e) {
        if (PathologyController.concurrentSaveConflict(e)) {
            return versionConflict("case");
        }
        throw e;
    }

    /** The request's id and status as they now stand. */
    private static ResponseEntity<Map<String, Object>> answer(PathologyRequest stored) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", stored.getId());
        body.put("status", stored.getStatus() == null ? null : stored.getStatus().name());
        return ResponseEntity.ok(body);
    }

    /**
     * A bench request as the pathologist raises it. Kind and priority are the names
     * as posted; the service refuses one it does not know.
     */
    public static class RaiseRequest {
        public String kind;
        public Integer targetBlockId;
        public Integer targetSlideId;
        public String stainId;
        public String priority;
        public String instruction;

        RequestDraft toDraft() {
            return new RequestDraft(kind, targetBlockId, targetSlideId, stainId, priority, instruction);
        }
    }

    /**
     * The status the page showed, the one asked for, and why a request is cancelled
     * or what fulfilled it.
     */
    public static class StatusChangeRequest {
        public String from;
        public String status;
        public String cancelReason;
        public Integer resultBlockId;
        public Integer resultSlideId;

        RequestStatusChange toChange() {
            return new RequestStatusChange(from, status, cancelReason, resultBlockId, resultSlideId);
        }
    }
}
