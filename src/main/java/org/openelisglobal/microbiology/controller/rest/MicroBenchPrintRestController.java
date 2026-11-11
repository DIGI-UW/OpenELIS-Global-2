package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroBenchPrintForm;
import org.openelisglobal.microbiology.service.MicroBenchPrintService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/bench/sheets")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroBenchPrintRestController extends MicrobiologyRestControllerSupport {
    private final MicroBenchPrintService print;

    public MicroBenchPrintRestController(MicroBenchPrintService print) {
        this.print = print;
    }

    @PostMapping
    public MicroBenchPrintForm.SavedSheet save(@RequestBody MicroBenchPrintForm body, HttpServletRequest request) {
        return print.print(body, authenticatedUserId(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> open(@PathVariable String id, HttpServletRequest request) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .body(print.open(id, authenticatedUserId(request)));
    }
}
