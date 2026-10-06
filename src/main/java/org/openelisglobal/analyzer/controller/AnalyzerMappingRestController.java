package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationRequest;
import org.openelisglobal.analyzer.service.AnalyzerMappingConfirmationView;
import org.openelisglobal.analyzer.service.AnalyzerMappingEditorService;
import org.openelisglobal.analyzer.service.AnalyzerMappingUpdate;
import org.openelisglobal.analyzer.service.AnalyzerMappingView;
import org.openelisglobal.analyzer.service.AnalyzerRequestException;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One analyzer's own mapping to the local catalog: read it, edit it, confirm
 * it.
 */
@RestController
@RequestMapping("/rest/analyzer/analyzers/{id}/mapping")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerMappingRestController extends BaseRestController {

    private final AnalyzerMappingEditorService mappingEditorService;

    public AnalyzerMappingRestController(AnalyzerMappingEditorService mappingEditorService) {
        this.mappingEditorService = mappingEditorService;
    }

    @GetMapping
    public ResponseEntity<AnalyzerMappingView> getMapping(@PathVariable String id) {
        return ResponseEntity.ok(mappingEditorService.getMapping(id));
    }

    @PutMapping
    public ResponseEntity<AnalyzerMappingView> saveMapping(@PathVariable String id,
            @RequestBody AnalyzerMappingUpdate update, HttpServletRequest request) {
        return ResponseEntity.ok(mappingEditorService.saveMapping(id, update, getSysUserId(request)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<AnalyzerMappingConfirmationView> confirmMapping(@PathVariable String id,
            @RequestBody AnalyzerMappingConfirmationRequest confirmation, HttpServletRequest request) {
        return ResponseEntity.ok(mappingEditorService.confirmMapping(id, confirmation, getSysUserId(request)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(AnalyzerRequestException.body(exception, "Invalid analyzer mapping request"));
    }
}
