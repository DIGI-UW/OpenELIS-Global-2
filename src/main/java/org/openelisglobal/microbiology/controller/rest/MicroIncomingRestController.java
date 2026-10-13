package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.MicroIncomingResultService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/incoming")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroIncomingRestController extends MicrobiologyRestControllerSupport {
    private final MicroIncomingResultService incoming;

    public MicroIncomingRestController(MicroIncomingResultService incoming) {
        this.incoming = incoming;
    }

    @GetMapping
    public List<MicroIncomingForm> list(@PathVariable String caseId, HttpServletRequest request) {
        return incoming.list(caseId, authenticatedUserId(request));
    }

    @PostMapping("/{id}/place")
    @PreAuthorize("hasAnyRole('ADMIN','RESULTS')")
    public MicroIncomingForm place(@PathVariable String caseId, @PathVariable String id,
            @RequestBody MicroIncomingPlacementForm body, HttpServletRequest request) {
        return incoming.place(caseId, id, body, authenticatedUserId(request), request);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('ADMIN','RESULTS')")
    public MicroIncomingForm fail(@PathVariable String caseId, @PathVariable String id,
            @RequestBody MicroIncomingPlacementForm body, HttpServletRequest request) {
        return incoming.fail(caseId, id, body.reason, authenticatedUserId(request));
    }
}
