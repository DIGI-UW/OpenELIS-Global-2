package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroCaseNonconformanceRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseNonconformanceResult;
import org.openelisglobal.microbiology.service.MicroCaseNonconformanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases")
public class MicroCaseNonconformanceRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseNonconformanceService nonconformanceService;

    public MicroCaseNonconformanceRestController(MicroCaseNonconformanceService nonconformanceService) {
        this.nonconformanceService = nonconformanceService;
    }

    @PostMapping("/{caseId}/nonconformances")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseNonconformanceResult> report(@PathVariable String caseId,
            @RequestBody MicroCaseNonconformanceRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(nonconformanceService.report(caseId, request, authenticatedUserId(httpRequest)));
    }
}
