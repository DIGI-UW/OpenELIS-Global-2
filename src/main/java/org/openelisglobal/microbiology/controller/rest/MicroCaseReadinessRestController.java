package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.service.MicroCaseReadinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases")
public class MicroCaseReadinessRestController extends BaseRestController {

    private final MicroCaseReadinessService readinessService;

    public MicroCaseReadinessRestController(MicroCaseReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @GetMapping("/{caseId}/readiness")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseReadinessForm> getReadiness(@PathVariable String caseId) {
        return ResponseEntity.ok(readinessService.getReadiness(caseId));
    }
}
