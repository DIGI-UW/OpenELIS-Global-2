package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroContainerLabelForm;
import org.openelisglobal.microbiology.service.MicroContainerLabelService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/containers")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroContainerLabelRestController extends MicrobiologyRestControllerSupport {
    private final MicroContainerLabelService labels;

    public MicroContainerLabelRestController(MicroContainerLabelService labels) {
        this.labels = labels;
    }

    @GetMapping("/presets")
    public java.util.List<MicroContainerLabelForm.Preset> presets() {
        return labels.presets();
    }

    @GetMapping("/resolve")
    public MicroContainerLabelForm.Target resolve(@RequestParam String code, HttpServletRequest request) {
        return labels.resolve(code, authenticatedUserId(request));
    }

    @PostMapping("/labels")
    public ResponseEntity<byte[]> print(@RequestBody MicroContainerLabelForm body, HttpServletRequest request) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .body(labels.print(body, authenticatedUserId(request)));
    }
}
