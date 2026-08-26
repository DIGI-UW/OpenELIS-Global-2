package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.openelisglobal.analyzer.form.AnalyzerMigrationPlanRequest;
import org.openelisglobal.analyzer.service.AnalyzerMigrationManifest;
import org.openelisglobal.analyzer.service.AnalyzerMigrationService;
import org.openelisglobal.common.rest.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/migration")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyzerMigrationRestController extends BaseRestController {

    private final AnalyzerMigrationService migrationService;

    @Autowired
    public AnalyzerMigrationRestController(AnalyzerMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/plan")
    public ResponseEntity<AnalyzerMigrationManifest> plan(@Valid @RequestBody AnalyzerMigrationPlanRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(migrationService.plan(request, getSysUserId(httpRequest)));
    }

    @PostMapping("/apply")
    public ResponseEntity<AnalyzerMigrationManifest> apply(@RequestBody AnalyzerMigrationManifest plan,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(migrationService.apply(plan, getSysUserId(httpRequest)));
    }

    @PostMapping("/verify")
    public ResponseEntity<AnalyzerMigrationManifest> verify(@RequestBody AnalyzerMigrationManifest apply) {
        return ResponseEntity.ok(migrationService.verify(apply));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMigration(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(exception.getMessage()));
    }

    public record ErrorResponse(String error) {
    }
}
