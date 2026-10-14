package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.MicroCaseWorkspaceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseWorkspaceRestController extends MicrobiologyRestControllerSupport {
    private final MicroCaseWorkspaceService workspace;
    private final org.openelisglobal.microbiology.service.MicroCaseContextService context;

    public MicroCaseWorkspaceRestController(MicroCaseWorkspaceService workspace,
            org.openelisglobal.microbiology.service.MicroCaseContextService context) {
        this.workspace = workspace;
        this.context = context;
    }

    @InitBinder("query")
    public void bind(org.springframework.web.bind.WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/search")
    public MicroCaseSearchPageForm search(@ModelAttribute("query") MicroCaseSearchForm query,
            HttpServletRequest request) {
        return workspace.search(query, authenticatedUserId(request));
    }

    @GetMapping("/by-accession")
    public MicroCaseSearchPageForm byAccession(@RequestParam String accessionNumber, HttpServletRequest request) {
        MicroCaseSearchForm query = new MicroCaseSearchForm();
        query.accessionNumber = accessionNumber;
        return workspace.search(query, authenticatedUserId(request));
    }

    @GetMapping("/{caseId}/shell")
    public MicroCaseShellForm get(@PathVariable String caseId, HttpServletRequest request) {
        return workspace.get(caseId, authenticatedUserId(request));
    }

    @PostMapping("/{caseId}/context")
    public MicroCaseShellForm context(@PathVariable String caseId, @RequestBody MicroCaseContextRequestForm body,
            HttpServletRequest request) {
        String actor = authenticatedUserId(request);
        context.update(caseId, body, actor);
        return workspace.get(caseId, actor);
    }

    @PostMapping("/{caseId}/transfer")
    public MicroCaseShellForm transfer(@PathVariable String caseId, @RequestBody MicroCaseTransferForm body,
            HttpServletRequest request) {
        return workspace.transfer(caseId, body.labUnitId, authenticatedUserId(request));
    }
}
