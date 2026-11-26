package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.util.EnumNames;
import org.openelisglobal.program.valueholder.pathology.CoverslipQc;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.StainStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Records what staining found for a case's slides, and how their coverslip
 * check went, as requests of their own rather than fields of the case save, so
 * each names who recorded it and when (FR-8.1 to FR-8.4). One slide is a list
 * of one, so a row's action and a batch over selected rows take the same path.
 *
 * <p>
 * A refusal answers 400 with its message id, an unknown case or one outside the
 * user's units 404, and a request that lost the race for the case to another
 * mark, a move or a save landing in the same moment 409
 * {@code conflict: "version"}.
 */
@RestController
@RequestMapping(value = "/rest/pathology/caseView/{pathologySampleId}/slides", produces = MediaType.APPLICATION_JSON_VALUE)
public class PathologySlideWorkRestController extends PathologyRestController {

    private final PathologySampleService pathologySampleService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;

    public PathologySlideWorkRestController(PathologySampleService pathologySampleService,
            ProgramSampleLabUnitScope programSampleLabUnitScope) {
        this.pathologySampleService = pathologySampleService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
    }

    @PostMapping("/stain-status")
    public ResponseEntity<Map<String, Object>> recordStainStatus(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) StainStatusRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(
                    pathologySampleService.recordStainStatus(pathologySampleId, body == null ? null : body.slideIds,
                            EnumNames.valueNamed(StainStatus.class, body == null ? null : body.status), sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    @PostMapping("/coverslip-qc")
    public ResponseEntity<Map<String, Object>> recordCoverslipQc(
            @PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody(required = false) CoverslipQcRequest body, HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, pathologySampleService.get(pathologySampleId));
        try {
            return answer(
                    pathologySampleService.recordCoverslipQc(pathologySampleId, body == null ? null : body.slideIds,
                            EnumNames.valueNamed(CoverslipQc.class, body == null ? null : body.result), sysUserId));
        } catch (RuntimeException e) {
            return conflictOrRethrow(e);
        }
    }

    /**
     * A mark takes the case's row and raises its version before it writes any
     * slide, so a mark racing another mark, a move or a save usually loses on the
     * case: its version check, or a lock the database would not grant. A slide's
     * own version check is the rarer loser. Either way the request says so rather
     * than fail, and since the race can surface wrapped by the data layer, the
     * whole cause chain is read.
     */
    private static ResponseEntity<Map<String, Object>> conflictOrRethrow(RuntimeException e) {
        if (PathologyController.concurrentSaveConflict(e)) {
            return versionConflict("case");
        }
        throw e;
    }

    /** The named slides as they now stand; the screen reads the case again. */
    private static ResponseEntity<Map<String, Object>> answer(List<PathologySlide> slides) {
        return ResponseEntity.ok(Map.of("slides",
                slides.stream().map(PathologySlideWorkRestController::toMap).collect(Collectors.toList())));
    }

    private static Map<String, Object> toMap(PathologySlide slide) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", slide.getId());
        row.put("designation", slide.getDesignation());
        row.put("barcode", slide.getBarcode());
        row.put("blockId", slide.getBlockId());
        row.put("active", slide.isActive());
        row.put("stainStatus", slide.getStainStatus() == null ? null : slide.getStainStatus().name());
        row.put("coverslipQc", slide.getCoverslipQc() == null ? null : slide.getCoverslipQc().name());
        return row;
    }

    /** The slides marked, and what staining found for them. */
    public static class StainStatusRequest {
        public List<Integer> slideIds;
        public String status;
    }

    /** The slides checked, and how their coverslip check went. */
    public static class CoverslipQcRequest {
        public List<Integer> slideIds;
        public String result;
    }
}
