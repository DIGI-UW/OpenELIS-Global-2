package org.openelisglobal.program.controller;

import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.service.GenericProgramDisplayService;
import org.openelisglobal.program.valueholder.ProgramSampleDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ProgramDashboardController {

    @Autowired
    private GenericProgramDisplayService genericProgramDisplayService;

    @GetMapping("/programSamplesList")
    public ResponseEntity<DashboardSummary> getAllProgramSamples() {
        DashboardSummary summary = genericProgramDisplayService.getAllProgramSamples();
        return ResponseEntity.ok(summary);
    }

    @GetMapping(value = "/programSample/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProgramSampleDisplayItem> getProgramSampleDisplayItem(@PathVariable int id) {
        ProgramSampleDisplayItem psdi = genericProgramDisplayService.getProgramSampleById(id);

        return ResponseEntity.ok(psdi);
    }

}