package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.MicroIsolateWorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/isolates")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroIsolateRestController extends MicrobiologyRestControllerSupport {
    private final MicroIsolateWorkspaceService service;

    public MicroIsolateRestController(MicroIsolateWorkspaceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<MicroIsolateWorkspaceForm> get(@PathVariable String caseId, HttpServletRequest r) {
        return ResponseEntity.ok(service.get(caseId, authenticatedUserId(r)));
    }

    @GetMapping("/options")
    public ResponseEntity<MicroIsolateWorkspaceForm.Options> options(@PathVariable String caseId) {
        return ResponseEntity.ok(service.options(caseId));
    }

    @PostMapping
    public ResponseEntity<MicroIsolateWorkspaceForm> pick(@PathVariable String caseId,
            @RequestBody MicroIsolateWorkspaceRequestForm body, HttpServletRequest r) {
        return ResponseEntity.ok(service.pick(caseId, body, authenticatedUserId(r)));
    }

    @PostMapping("/referrals")
    public ResponseEntity<MicroIsolateWorkspaceForm> refer(@PathVariable String caseId,
            @RequestBody MicroIsolateWorkspaceRequestForm body, HttpServletRequest r) {
        return ResponseEntity.ok(service.refer(caseId, body, authenticatedUserId(r)));
    }

    @PostMapping({ "/{isolateId}/reidentify", "/{isolateId}/acknowledge" })
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroIsolateWorkspaceForm> validate(@PathVariable String caseId,
            @PathVariable String isolateId, @RequestBody MicroIsolateWorkspaceRequestForm body, HttpServletRequest r) {
        String action = r.getRequestURI().substring(r.getRequestURI().lastIndexOf('/') + 1);
        return ResponseEntity.ok(service.act(caseId, isolateId, action, body, authenticatedUserId(r)));
    }

    @PostMapping({ "/{isolateId}/identify", "/{isolateId}/sample-item", "/{isolateId}/received-origin",
            "/{isolateId}/referral", "/{isolateId}/test" })
    public ResponseEntity<MicroIsolateWorkspaceForm> act(@PathVariable String caseId, @PathVariable String isolateId,
            @RequestBody MicroIsolateWorkspaceRequestForm body, HttpServletRequest r) {
        String action = r.getRequestURI().substring(r.getRequestURI().lastIndexOf('/') + 1);
        return ResponseEntity.ok(service.act(caseId, isolateId, action, body, authenticatedUserId(r)));
    }
}
