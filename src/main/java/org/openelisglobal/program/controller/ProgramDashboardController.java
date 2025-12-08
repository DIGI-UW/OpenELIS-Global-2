package org.openelisglobal.program.controller;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.service.GenericProgramDisplayService;
import org.openelisglobal.program.valueholder.ProgramSampleDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ProgramDashboardController extends BaseRestController {

    @Autowired
    private GenericProgramDisplayService genericProgramDisplayService;

    @GetMapping("/programSamplesList")
    public ResponseEntity<DashboardSummary> getAllProgramSamples(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String filter) {

        DashboardSummary summary = genericProgramDisplayService.getAllProgramSamples(filter, size, page);
        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/programSample/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgramSampleDisplayItem> getProgramSampleDisplayItem(@PathVariable int id) {
        ProgramSampleDisplayItem psdi = genericProgramDisplayService.getProgramSampleById(id);

        return ResponseEntity.ok(psdi);
    }

}