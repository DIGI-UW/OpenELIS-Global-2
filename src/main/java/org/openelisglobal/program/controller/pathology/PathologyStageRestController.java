package org.openelisglobal.program.controller.pathology;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.StaleStateException;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.PathologyStageConflictException;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.util.EnumNames;
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
 * case twice, and a send to the pathologist names the open bench requests the
 * page listed, so one raised behind the page is shown before it is passed over.
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
                EnumNames.valueNamed(PathologyStatus.class, body == null ? null : body.from), sysUserId,
                body == null || body.acknowledgedRequestIds == null ? List.of() : body.acknowledgedRequestIds);
        return ResponseEntity.ok(Map.of("status", entered.getStage().name()));
    }

    @PostMapping("/return")
    public ResponseEntity<Map<String, Object>> returnToPreviousStage(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) ReturnRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        PathologyStageEvent entered = pathologySampleService.returnToPreviousStage(pathologySampleId,
                EnumNames.valueNamed(PathologyStatus.class, body == null ? null : body.from),
                body == null ? null : body.reason, sysUserId);
        return ResponseEntity.ok(Map.of("status", entered.getStage().name()));
    }

    /**
     * Reopens a signed-out case, with the stage the page showed and the reason, in
     * the shapes a return answers with.
     */
    @PostMapping("/reopen")
    public ResponseEntity<Map<String, Object>> reopen(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) ReturnRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        PathologyStageEvent entered = pathologySampleService.reopen(pathologySampleId,
                EnumNames.valueNamed(PathologyStatus.class, body == null ? null : body.from),
                body == null ? null : body.reason, sysUserId);
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
    public ResponseEntity<Map<String, Object>> onVersionConflict(RuntimeException e) {
        return versionConflict("case");
    }

    /**
     * The stage the page showed the case at and, when the move would send the case
     * to the pathologist with bench requests open, the ids of the open requests the
     * page listed and the user chose to send past (FR-10.11). A page that lists
     * none sends the stage alone.
     */
    public static class AdvanceRequest {
        public String from;
        public List<Integer> acknowledgedRequestIds;
    }

    /**
     * The stage the page showed the case at, and why it is sent back or reopened.
     */
    public static class ReturnRequest {
        public String from;
        public String reason;
    }
}
