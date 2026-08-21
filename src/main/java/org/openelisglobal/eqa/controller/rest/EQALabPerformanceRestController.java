package org.openelisglobal.eqa.controller.rest;

import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.eqa.service.EQALabPerformanceService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab EQA Performance (OGC-611, FR-V2.3-07): the Coverage snapshot and Recent
 * Cycles list in one read, because the page renders them from one rollup.
 */
@RestController
@RequestMapping("/rest/eqa")
@PreAuthorize(EQAGuards.READ)
public class EQALabPerformanceRestController extends BaseRestController {

    private final EQALabPerformanceService labPerformanceService;

    public EQALabPerformanceRestController(EQALabPerformanceService labPerformanceService) {
        this.labPerformanceService = labPerformanceService;
    }

    @GetMapping(value = "/lab-performance", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> labPerformance() {
        return labPerformanceService.getLabPerformance();
    }
}
