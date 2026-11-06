package org.openelisglobal.program.controller.pathology;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.StaleStateException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologyStageConflictException;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyStageEvent;
import org.springframework.dao.OptimisticLockingFailureException;
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
 * Sends a pathology case on to its next stage, or back to an earlier one, as
 * its own request rather than a field of the case save, so every move names who
 * made it and when (FR-2.4, FR-2.5). The request carries the stage the page
 * showed, so a page another user has overtaken is told so instead of moving the
 * case twice.
 */
@RestController
@RequestMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
public class PathologyStageRestController extends PathologyRestController {

    private final PathologySampleService pathologySampleService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;

    public PathologyStageRestController(PathologySampleService pathologySampleService,
            ProgramSampleLabUnitScope programSampleLabUnitScope) {
        this.pathologySampleService = pathologySampleService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
    }

    @PostMapping("/advance")
    public ResponseEntity<Map<String, Object>> advance(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) AdvanceRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        PathologyStageEvent entered = pathologySampleService.advance(pathologySampleId,
                stageNamed(body == null ? null : body.from), sysUserId);
        return ResponseEntity.ok(Map.of("status", entered.getStage().name()));
    }

    @PostMapping("/return")
    public ResponseEntity<Map<String, Object>> returnToPreviousStage(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) ReturnRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        PathologyStageEvent entered = pathologySampleService.returnToPreviousStage(pathologySampleId,
                stageNamed(body == null ? null : body.from), body == null ? null : body.reason, sysUserId);
        return ResponseEntity.ok(Map.of("status", entered.getStage().name()));
    }

    /** The page showed a stage the case has already left. */
    @ExceptionHandler(PathologyStageConflictException.class)
    public ResponseEntity<Map<String, Object>> stageConflict(PathologyStageConflictException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("conflict", "stage");
        body.put("status", e.getStoredStage() == null ? null : e.getStoredStage().name());
        body.put("error", "the case is no longer at the stage shown; reload the case");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Two moves of one case in the same moment: the one that lost the case's
     * version check is told so rather than seeing a server error.
     */
    @ExceptionHandler({ OptimisticLockingFailureException.class, OptimisticLockException.class,
            StaleStateException.class })
    public ResponseEntity<Map<String, Object>> versionConflict(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("conflict", "version", "error",
                "another change to this case landed at the same moment; reload the case and try again"));
    }

    /**
     * The stage the request names, or null when it names none or one that does not
     * exist; the service refuses a move it cannot check against the page.
     */
    private static PathologyStatus stageNamed(String from) {
        if (StringUtils.isBlank(from)) {
            return null;
        }
        try {
            return PathologyStatus.valueOf(from.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The stage the page showed the case at. */
    public static class AdvanceRequest {
        public String from;
    }

    /** The stage the page showed the case at, and why it is sent back. */
    public static class ReturnRequest {
        public String from;
        public String reason;
    }
}
