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
    public MicroCaseSearchPageForm byAccession(@RequestParam String accessionNumber,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        if (accessionNumber.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Accession number is required");
        MicroCaseSearchForm query = new MicroCaseSearchForm();
        query.accessionNumber = accessionNumber;
        query.page = page;
        query.pageSize = pageSize;
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
