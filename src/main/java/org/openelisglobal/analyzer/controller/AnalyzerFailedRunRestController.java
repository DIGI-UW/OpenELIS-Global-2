package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.analyzerresults.service.AnalyzerFailedRunService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A reviewer's decision that a held analyzer run failed and needs repeating.
 */
@RestController
@RequestMapping("/rest/analyzer/results")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerFailedRunRestController extends BaseRestController {

    private final AnalyzerFailedRunService failedRunService;

    public AnalyzerFailedRunRestController(AnalyzerFailedRunService failedRunService) {
        this.failedRunService = failedRunService;
    }

    @PostMapping("/{id}/failed-run")
    public ResponseEntity<Map<String, String>> dismissAsFailedRun(@PathVariable String id, HttpServletRequest request) {
        String analysisId = failedRunService.dismissAsFailedRun(id, getSysUserId(request));
        return ResponseEntity.ok(Map.of("analysisId", analysisId));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> refuse(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(ObjectNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }
}
