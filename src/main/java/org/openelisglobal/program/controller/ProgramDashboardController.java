package org.openelisglobal.program.controller;

import java.util.List;
import org.openelisglobal.program.bean.DashboardSummary;
import org.openelisglobal.program.service.GenericProgramService;
import org.openelisglobal.program.valueholder.ProgramUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/programs")
public class ProgramDashboardController {

    @Autowired
    private GenericProgramService genericProgramService;

    @GetMapping("/entries")
    public ResponseEntity<List<ProgramUtil>> getAllProgramEntries() {
        List<ProgramUtil> entries = genericProgramService.getAllProgramEntries();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/entries/{programSampleId}")
    public ResponseEntity<ProgramUtil> getProgramEntry(@PathVariable Integer programSampleId) {
        ProgramUtil entry = genericProgramService.getProgramEntry(programSampleId);
        return ResponseEntity.ok(entry);
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary() {
        DashboardSummary summary = genericProgramService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}