package org.openelisglobal.microbiology.controller.rest;

import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.service.MicroWorklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/worklist")
public class MicroWorklistRestController extends BaseRestController {

    private final MicroWorklistService worklistService;

    public MicroWorklistRestController(MicroWorklistService worklistService) {
        this.worklistService = worklistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')")
    public ResponseEntity<MicroWorklistPageForm> getWorklistRows(@RequestParam(required = false) String grain,
            @RequestParam(required = false) String status, @RequestParam(required = false) String workflow,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) List<String> specimen,
            @RequestParam(required = false) List<String> organism, @RequestParam(required = false) List<String> origin,
            @RequestParam(required = false) List<String> significance, @RequestParam(required = false) String stage,
            @RequestParam(required = false) String urgency, @RequestParam(required = false) String due,
            @RequestParam(required = false) String q, @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize) {
        MicroWorklistQueryForm query = new MicroWorklistQueryForm();
        query.grain = grain;
        query.status = status;
        query.workflow = workflow;
        query.from = from;
        query.to = to;
        query.specimen = specimen;
        query.organism = organism;
        query.origin = origin;
        query.significance = significance;
        query.stage = stage;
        query.urgency = urgency;
        query.due = due;
        query.q = q;
        query.sort = sort;
        if (page != null) {
            query.page = page;
        }
        if (pageSize != null) {
            query.pageSize = pageSize;
        }
        return ResponseEntity.ok(worklistService.getWorklistPage(query));
    }
}
