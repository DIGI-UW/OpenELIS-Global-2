package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroAstWorkspaceRestController extends MicrobiologyRestControllerSupport {
    private final MicroAstWorkspaceService workspace;
    private final MicroPatientReportService reports;
    private final MicroDstService dst;

    public MicroAstWorkspaceRestController(MicroAstWorkspaceService workspace, MicroPatientReportService reports,
            MicroDstService dst) {
        this.workspace = workspace;
        this.reports = reports;
        this.dst = dst;
    }

    @GetMapping("/ast")
    public MicroAstWorkspaceForm get(@PathVariable String caseId, HttpServletRequest request) {
        return workspace.get(caseId, authenticatedUserId(request));
    }

    @PostMapping("/ast/entry")
    public MicroAstWorkspaceForm enter(@PathVariable String caseId,
            @jakarta.validation.Valid @RequestBody MicroAstWorkspaceForm.Action action, HttpServletRequest request) {
        return workspace.act(caseId, action, authenticatedUserId(request), false);
    }

    @PostMapping("/ast/validation")
    @PreAuthorize(SUPERVISOR_ACCESS)
    public MicroAstWorkspaceForm validate(@PathVariable String caseId,
            @jakarta.validation.Valid @RequestBody MicroAstWorkspaceForm.Action action, HttpServletRequest request) {
        return workspace.act(caseId, action, authenticatedUserId(request), true);
    }

    @PostMapping("/dst/resolve")
    @PreAuthorize(SUPERVISOR_ACCESS)
    public MicroDstForm resolve(@PathVariable String caseId,
            @jakarta.validation.Valid @RequestBody MicroDstForm.ResolutionRequest action, HttpServletRequest request) {
        return dst.resolve(caseId, action, authenticatedUserId(request));
    }

    @GetMapping("/report")
    public MicroPatientReportForm report(@PathVariable String caseId) {
        return reports.display(caseId);
    }

    @GetMapping("/report/versions")
    public java.util.List<MicroPatientReportForm.Version> versions(@PathVariable String caseId) {
        return reports.versions(caseId);
    }

    @PostMapping("/analyses/{analysisId}/report-selection")
    @PreAuthorize(SUPERVISOR_ACCESS)
    public MicroPatientReportForm select(@PathVariable String caseId, @PathVariable String analysisId,
            @RequestBody MicroPatientReportForm.Selection selection, HttpServletRequest request) {
        reports.selectAnalysis(caseId, analysisId, selection.selected, authenticatedUserId(request));
        return reports.display(caseId);
    }
}
