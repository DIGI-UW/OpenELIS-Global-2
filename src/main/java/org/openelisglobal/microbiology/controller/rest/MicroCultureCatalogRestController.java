package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.microbiology.form.MicroCultureOptionsForm;
import org.openelisglobal.microbiology.service.MicroCultureService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/culture-catalog")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCultureCatalogRestController extends MicrobiologyRestControllerSupport {
    private final MicroCultureService service;

    public MicroCultureCatalogRestController(MicroCultureService service) {
        this.service = service;
    }

    @GetMapping
    public MicroCultureOptionsForm get() {
        return service.getCatalog();
    }
}
