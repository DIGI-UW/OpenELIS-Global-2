package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;
import org.openelisglobal.microbiology.service.MicroWhonetReadinessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/whonet-readiness")
public class MicroWhonetReadinessRestController extends BaseRestController {

    private final MicroWhonetReadinessService whonetReadinessService;

    public MicroWhonetReadinessRestController(MicroWhonetReadinessService whonetReadinessService) {
        this.whonetReadinessService = whonetReadinessService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroWhonetReadinessForm> getReadiness(@PathVariable String caseId) {
        return ResponseEntity.ok(whonetReadinessService.getReadiness(caseId));
    }
}
