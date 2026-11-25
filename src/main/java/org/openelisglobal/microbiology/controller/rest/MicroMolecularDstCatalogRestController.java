package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroDstForm;
import org.openelisglobal.microbiology.service.MicroMolecularDstCatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/admin/dst-bindings")
@PreAuthorize("hasRole('ADMIN')")
public class MicroMolecularDstCatalogRestController extends MicrobiologyRestControllerSupport {
    private final MicroMolecularDstCatalogService service;

    public MicroMolecularDstCatalogRestController(MicroMolecularDstCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public MicroDstForm.Catalog get() {
        return service.getCatalog();
    }

    @PostMapping
    public MicroDstForm.Binding save(@RequestBody MicroDstForm.Binding binding, HttpServletRequest request) {
        return service.save(binding, authenticatedUserId(request));
    }
}
