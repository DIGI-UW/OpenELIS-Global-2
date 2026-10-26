package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.MicroCultureService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/cultures")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCultureRestController extends MicrobiologyRestControllerSupport {
    private final MicroCultureService service;

    public MicroCultureRestController(MicroCultureService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MicroCultureForm>> get(@PathVariable String caseId) {
        return ResponseEntity.ok(service.getRows(caseId));
    }

    @GetMapping("/options")
    public ResponseEntity<MicroCultureOptionsForm> options(@PathVariable String caseId) {
        return ResponseEntity.ok(service.getOptions(caseId));
    }

    @PostMapping
    public ResponseEntity<List<MicroCultureForm>> inoculate(@PathVariable String caseId,
            @RequestBody MicroCultureRequestForm body, HttpServletRequest request) {
        return ResponseEntity.ok(service.inoculate(caseId, body, authenticatedUserId(request)));
    }

    @PostMapping("/{rowId}/{action}")
    public ResponseEntity<List<MicroCultureForm>> act(@PathVariable String caseId, @PathVariable String rowId,
            @PathVariable String action, @RequestBody MicroCultureRequestForm body, HttpServletRequest request) {
        return ResponseEntity.ok(service.act(caseId, rowId, action, body, authenticatedUserId(request)));
    }
}
