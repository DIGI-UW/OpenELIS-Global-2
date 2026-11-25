package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/bench")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroBenchRestController extends MicrobiologyRestControllerSupport {
    private final MicroBenchService bench;
    private final MicroBenchCaseService cases;

    public MicroBenchRestController(MicroBenchService bench, MicroBenchCaseService cases) {
        this.bench = bench;
        this.cases = cases;
    }

    @InitBinder("query")
    public void bind(org.springframework.web.bind.WebDataBinder binder) {
        binder.initDirectFieldAccess();
    }

    @GetMapping
    public MicroBenchPageForm search(@ModelAttribute("query") MicroBenchQueryForm query, HttpServletRequest request) {
        return bench.search(query, authenticatedUserId(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RESULTS')")
    public List<MicroBenchResultForm> apply(@RequestBody MicroBenchRequestForm body, HttpServletRequest request) {
        return bench.apply(body, authenticatedUserId(request));
    }

    @PostMapping("/{caseId}/{actionId}/undo")
    @PreAuthorize("hasAnyRole('ADMIN','RESULTS')")
    public MicroBenchResultForm undo(@PathVariable String caseId, @PathVariable String actionId,
            HttpServletRequest request) {
        return cases.undo(caseId, actionId, authenticatedUserId(request));
    }
}
