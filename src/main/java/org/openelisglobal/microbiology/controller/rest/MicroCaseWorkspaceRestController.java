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

    public MicroCaseWorkspaceRestController(MicroCaseWorkspaceService workspace) {
        this.workspace = workspace;
    }

    @InitBinder
    public void bind(org.springframework.web.bind.WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping("/search")
    public MicroCaseSearchPageForm search(@ModelAttribute MicroCaseSearchForm query, HttpServletRequest request) {
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

    @PostMapping("/{caseId}/transfer")
    public MicroCaseShellForm transfer(@PathVariable String caseId, @RequestBody MicroCaseTransferForm body,
            HttpServletRequest request) {
        return workspace.transfer(caseId, body.labUnitId, authenticatedUserId(request));
    }
}
