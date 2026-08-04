package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroAntibioticAdminForm;
import org.openelisglobal.microbiology.form.MicroAstPanelAdminForm;
import org.openelisglobal.microbiology.form.MicroCultureSetupAdminForm;
import org.openelisglobal.microbiology.form.MicroOrganismAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.service.MicrobiologyReferenceAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/admin/reference")
@PreAuthorize("hasRole('ADMIN')")
public class MicrobiologyReferenceAdminRestController extends MicrobiologyRestControllerSupport {

    private final MicrobiologyReferenceAdminService service;

    public MicrobiologyReferenceAdminRestController(MicrobiologyReferenceAdminService service) {
        this.service = service;
    }

    @GetMapping("/organisms")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroOrganismAdminForm>> getOrganisms(
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getOrganisms(query));
    }

    @PostMapping("/organisms")
    public ResponseEntity<MicroOrganismAdminForm> createOrganism(HttpServletRequest request,
            @RequestBody MicroOrganismAdminForm organism) {
        return ResponseEntity.ok(service.saveOrganism(null, organism, authenticatedUserId(request)));
    }

    @PutMapping("/organisms/{id}")
    public ResponseEntity<MicroOrganismAdminForm> updateOrganism(@PathVariable String id, HttpServletRequest request,
            @RequestBody MicroOrganismAdminForm organism) {
        return ResponseEntity.ok(service.saveOrganism(id, organism, authenticatedUserId(request)));
    }

    @PatchMapping("/organisms/{id}/active")
    public ResponseEntity<MicroOrganismAdminForm> setOrganismActive(@PathVariable String id,
            @RequestParam boolean active, HttpServletRequest request) {
        return ResponseEntity.ok(service.setOrganismActive(id, active, authenticatedUserId(request)));
    }

    @GetMapping("/antibiotics")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroAntibioticAdminForm>> getAntibiotics(
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getAntibiotics(query));
    }

    @PostMapping("/antibiotics")
    public ResponseEntity<MicroAntibioticAdminForm> createAntibiotic(HttpServletRequest request,
            @RequestBody MicroAntibioticAdminForm antibiotic) {
        return ResponseEntity.ok(service.saveAntibiotic(null, antibiotic, authenticatedUserId(request)));
    }

    @PutMapping("/antibiotics/{id}")
    public ResponseEntity<MicroAntibioticAdminForm> updateAntibiotic(@PathVariable String id,
            HttpServletRequest request, @RequestBody MicroAntibioticAdminForm antibiotic) {
        return ResponseEntity.ok(service.saveAntibiotic(id, antibiotic, authenticatedUserId(request)));
    }

    @PatchMapping("/antibiotics/{id}/active")
    public ResponseEntity<MicroAntibioticAdminForm> setAntibioticActive(@PathVariable String id,
            @RequestParam boolean active, HttpServletRequest request) {
        return ResponseEntity.ok(service.setAntibioticActive(id, active, authenticatedUserId(request)));
    }

    @GetMapping("/ast-panels")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroAstPanelAdminForm>> getAstPanels(
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getAstPanels(query));
    }

    @GetMapping("/ast-panels/{id}")
    public ResponseEntity<MicroAstPanelAdminForm> getAstPanel(@PathVariable String id) {
        return ResponseEntity.ok(service.getAstPanel(id));
    }

    @PostMapping("/ast-panels")
    public ResponseEntity<MicroAstPanelAdminForm> createAstPanel(HttpServletRequest request,
            @RequestBody MicroAstPanelAdminForm panel) {
        return ResponseEntity.ok(service.createPanel(panel, authenticatedUserId(request)));
    }

    @PostMapping("/ast-panels/{id}/versions")
    public ResponseEntity<MicroAstPanelAdminForm> publishAstPanelVersion(@PathVariable String id,
            HttpServletRequest request, @RequestBody MicroAstPanelAdminForm panel) {
        return ResponseEntity.ok(service.publishPanelVersion(id, panel, authenticatedUserId(request)));
    }

    @GetMapping("/culture-setups")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroCultureSetupAdminForm>> getCultureSetups(
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getCultureSetups(query));
    }

    @PostMapping("/culture-setups")
    public ResponseEntity<MicroCultureSetupAdminForm> createCultureSetup(HttpServletRequest request,
            @RequestBody MicroCultureSetupAdminForm setup) {
        return ResponseEntity.ok(service.saveCultureSetup(null, setup, authenticatedUserId(request)));
    }

    @PutMapping("/culture-setups/{id}")
    public ResponseEntity<MicroCultureSetupAdminForm> updateCultureSetup(@PathVariable String id,
            HttpServletRequest request, @RequestBody MicroCultureSetupAdminForm setup) {
        return ResponseEntity.ok(service.saveCultureSetup(id, setup, authenticatedUserId(request)));
    }
}
