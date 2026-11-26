package org.openelisglobal.program.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.service.PriorResultsService;
import org.openelisglobal.program.service.ProgramSampleLabUnitScope;
import org.openelisglobal.program.valueholder.AnatomicPathologyBench;
import org.openelisglobal.program.valueholder.PriorResults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The prior results panel of each case view: the patient's earlier
 * anatomic-pathology cases, asked for by the case on screen. A case the user
 * may not read is not found, and its patient's cases are never read.
 */
@RestController
public class PriorResultsRestController extends BaseRestController {

    private final PriorResultsService priorResultsService;
    private final ProgramSampleLabUnitScope programSampleLabUnitScope;

    public PriorResultsRestController(PriorResultsService priorResultsService,
            ProgramSampleLabUnitScope programSampleLabUnitScope) {
        this.priorResultsService = priorResultsService;
        this.programSampleLabUnitScope = programSampleLabUnitScope;
    }

    @GetMapping(value = "/rest/pathology/caseView/{id}/priorResults", produces = MediaType.APPLICATION_JSON_VALUE)
    public PriorResults pathology(@PathVariable("id") Integer id,
            @RequestParam(name = "all", required = false) String all, HttpServletRequest request) {
        return priorResults(AnatomicPathologyBench.PATHOLOGY, id, all, request);
    }

    @GetMapping(value = "/rest/immunohistochemistry/caseView/{id}/priorResults", produces = MediaType.APPLICATION_JSON_VALUE)
    public PriorResults immunohistochemistry(@PathVariable("id") Integer id,
            @RequestParam(name = "all", required = false) String all, HttpServletRequest request) {
        return priorResults(AnatomicPathologyBench.IMMUNOHISTOCHEMISTRY, id, all, request);
    }

    @GetMapping(value = "/rest/cytology/caseView/{id}/priorResults", produces = MediaType.APPLICATION_JSON_VALUE)
    public PriorResults cytology(@PathVariable("id") Integer id,
            @RequestParam(name = "all", required = false) String all, HttpServletRequest request) {
        return priorResults(AnatomicPathologyBench.CYTOLOGY, id, all, request);
    }

    /**
     * Only the exact value "true" asks for every row; anything else is the capped
     * list.
     */
    private PriorResults priorResults(AnatomicPathologyBench bench, Integer id, String all,
            HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        programSampleLabUnitScope.requireVisible(sysUserId, priorResultsService.caseOf(bench, id));
        return priorResultsService.priorResultsOf(bench, id, "true".equals(all), sysUserId);
    }
}
