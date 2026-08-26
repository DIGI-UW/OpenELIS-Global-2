package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroReportProjectionForm;
import org.openelisglobal.microbiology.form.MicroReportReleaseForm;
import org.openelisglobal.microbiology.service.MicroReportProjectionResult;
import org.openelisglobal.microbiology.service.MicroReportProjectionService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/release")
public class MicroReportReleaseRestController extends MicrobiologyRestControllerSupport {

    private final MicroReportReleaseService releaseService;
    private final MicroReportProjectionService projectionService;

    public MicroReportReleaseRestController(MicroReportReleaseService releaseService,
            MicroReportProjectionService projectionService) {
        this.releaseService = releaseService;
        this.projectionService = projectionService;
    }

    @GetMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroReportProjectionForm> preview(@PathVariable String caseId) {
        return ResponseEntity.ok(toProjectionForm(projectionService.preview(caseId)));
    }

    @PostMapping("/preliminary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroReportReleaseForm> releasePreliminary(@PathVariable String caseId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toForm(releaseService.releasePreliminary(caseId, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/final")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroReportReleaseForm> releaseFinal(@PathVariable String caseId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toForm(releaseService.releaseFinal(caseId, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/amended")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS')")
    public ResponseEntity<MicroReportReleaseForm> releaseAmended(@PathVariable String caseId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toForm(releaseService.releaseAmended(caseId, authenticatedUserId(httpRequest))));
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

    private MicroReportProjectionForm toProjectionForm(MicroReportProjectionResult projection) {
        MicroReportProjectionForm form = new MicroReportProjectionForm();
        form.content = projection.getContent();
        form.reportableContent = projection.hasReportableContent();
        form.mappingConfigured = projection.isMappingConfigured();
        form.projectedResultIds.addAll(projection.getProjectedResultIds());
        return form;
    }
}
