package org.openelisglobal.analyzer.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.openelisglobal.analyzer.form.AnalyzerMigrationReferenceRequest;
import org.openelisglobal.analyzer.service.AnalyzerMigrationReferenceService;
import org.openelisglobal.analyzer.service.AnalyzerMigrationReferenceView;
import org.openelisglobal.analyzer.service.AnalyzerMigrationSourceService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Temporary read-only endpoint for the quiesced one-time analyzer cutover. */
@RestController
@RequestMapping("/rest/analyzer/migration")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AnalyzerMigrationRestController extends BaseRestController {

    private final AnalyzerMigrationSourceService sourceService;
    private final AnalyzerMigrationReferenceService referenceService;

    public AnalyzerMigrationRestController(AnalyzerMigrationSourceService sourceService,
            AnalyzerMigrationReferenceService referenceService) {
        this.sourceService = sourceService;
        this.referenceService = referenceService;
    }

    @GetMapping("/source")
    public ResponseEntity<ObjectNode> source() {
        return ResponseEntity.ok(sourceService.snapshot());
    }

    @PutMapping("/analyzers/{id}/reference")
    public ResponseEntity<AnalyzerMigrationReferenceView> attachReference(@PathVariable String id,
            @Valid @RequestBody AnalyzerMigrationReferenceRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(referenceService.attach(id, request, getSysUserId(httpRequest)));
    }

    @GetMapping("/analyzers/{id}/reference")
    public ResponseEntity<AnalyzerMigrationReferenceView> getReference(@PathVariable String id) {
        return ResponseEntity.ok(referenceService.get(id));
    }
}
