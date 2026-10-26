package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerLifecycleService;
import org.openelisglobal.analyzer.service.AnalyzerRequestException;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Retiring, restoring and deleting an analyzer. */
@RestController
@RequestMapping("/rest/analyzer/analyzers")
@PreAuthorize("hasRole('GLOBAL_ADMIN')")
public class AnalyzerLifecycleRestController extends BaseRestController {

    private final AnalyzerLifecycleService lifecycleService;

    public AnalyzerLifecycleRestController(AnalyzerLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<Void> retire(@PathVariable String id, HttpServletRequest request) {
        lifecycleService.retire(id, getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable String id, HttpServletRequest request) {
        lifecycleService.restore(id, getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {
        lifecycleService.delete(id, getSysUserId(request));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> refuse(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(AnalyzerRequestException.body(exception, "Invalid analyzer request"));
    }
}
