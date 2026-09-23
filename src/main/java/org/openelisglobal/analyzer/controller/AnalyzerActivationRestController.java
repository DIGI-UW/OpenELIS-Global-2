package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.analyzer.service.AnalyzerActivationResult;
import org.openelisglobal.analyzer.service.AnalyzerActivationService;
import org.openelisglobal.analyzer.service.AnalyzerDeactivationResult;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/analyzers")
@PreAuthorize("hasAnyRole('ANALYSER_IMPORT', 'ADMIN')")
public class AnalyzerActivationRestController extends BaseRestController {

    private final AnalyzerActivationService service;

    public AnalyzerActivationRestController(AnalyzerActivationService service) {
        this.service = service;
    }

    @GetMapping("/{id}/activation-readiness")
    public ResponseEntity<AnalyzerActivationResult> readiness(@PathVariable String id) {
        return ResponseEntity.ok(service.readiness(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<AnalyzerActivationResult> activate(@PathVariable String id, HttpServletRequest request) {
        AnalyzerActivationResult result = service.activate(id, getSysUserId(request));
        return ResponseEntity.status(result.activated() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<AnalyzerDeactivationResult> deactivate(@PathVariable String id, HttpServletRequest request) {
        AnalyzerDeactivationResult result = service.deactivate(id, getSysUserId(request));
        return ResponseEntity.status(result.deactivated() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY).body(result);
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<AnalyzerActivationResult> reactivate(@PathVariable String id, HttpServletRequest request) {
        AnalyzerActivationResult result = service.reactivate(id, getSysUserId(request));
        return ResponseEntity.status(result.activated() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY).body(result);
    }
}
