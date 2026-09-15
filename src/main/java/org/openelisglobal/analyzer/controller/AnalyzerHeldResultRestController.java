package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportService;
import org.openelisglobal.analyzerimport.service.AnalyzerNormalizedResultImportSummary;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Explicit operator action; transport retries must use the receipt-protected
 * ingestion endpoint.
 */
@RestController
@RequestMapping("/rest/analyzer/analyzers")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerHeldResultRestController extends BaseRestController {
    private final AnalyzerNormalizedResultImportService service;

    public AnalyzerHeldResultRestController(AnalyzerNormalizedResultImportService service) {
        this.service = service;
    }

    @PostMapping("/{analyzerId}/held-results/{resultId}/reprocess")
    public ResponseEntity<AnalyzerNormalizedResultImportSummary> reprocess(@PathVariable String analyzerId,
            @PathVariable String resultId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(service.reprocessHeldResult(analyzerId, resultId, getSysUserId(request)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
