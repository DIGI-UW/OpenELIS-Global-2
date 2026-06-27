package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroReportReleaseForm;
import org.openelisglobal.microbiology.form.MicroReportReleaseRequestForm;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/release")
public class MicroReportReleaseRestController extends BaseRestController {

    private final MicroReportReleaseService releaseService;

    public MicroReportReleaseRestController(MicroReportReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @PostMapping("/preliminary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroReportReleaseForm> releasePreliminary(@PathVariable String caseId,
            @RequestBody MicroReportReleaseRequestForm request) {
        return ResponseEntity.ok(toForm(releaseService.releasePreliminary(caseId, request.performedBy)));
    }

    @PostMapping("/final")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroReportReleaseForm> releaseFinal(@PathVariable String caseId,
            @RequestBody MicroReportReleaseRequestForm request) {
        return ResponseEntity.ok(toForm(releaseService.releaseFinal(caseId, request.performedBy)));
    }

    private MicroReportReleaseForm toForm(MicroCase microCase) {
        MicroReportReleaseForm form = new MicroReportReleaseForm();
        form.caseId = microCase.getId();
        form.finalReleaseState = microCase.getFinalReleaseState();
        form.stage = microCase.getStage();
        form.closedAt = microCase.getClosedAt();
        form.closedBy = microCase.getClosedBy();
        return form;
    }
}
