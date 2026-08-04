package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.util.Map;
import org.openelisglobal.microbiology.form.MicroBreakpointImportPreviewForm;
import org.openelisglobal.microbiology.form.MicroBreakpointRuleAdminForm;
import org.openelisglobal.microbiology.form.MicroBreakpointStandardAdminForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminPageForm;
import org.openelisglobal.microbiology.form.MicroReferenceAdminQueryForm;
import org.openelisglobal.microbiology.service.MicroBreakpointAdminService;
import org.openelisglobal.microbiology.service.MicroBreakpointImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/admin/breakpoints")
@PreAuthorize("hasRole('ADMIN')")
public class MicroBreakpointAdminRestController extends MicrobiologyRestControllerSupport {

    private final MicroBreakpointAdminService service;
    private final MicroBreakpointImportService importService;

    public MicroBreakpointAdminRestController(MicroBreakpointAdminService service,
            MicroBreakpointImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @GetMapping("/standards")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroBreakpointStandardAdminForm>> getStandards(
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getStandards(query));
    }

    @GetMapping("/standards/{id}/rules")
    public ResponseEntity<MicroReferenceAdminPageForm<MicroBreakpointRuleAdminForm>> getRules(@PathVariable String id,
            @ModelAttribute MicroReferenceAdminQueryForm query) {
        return ResponseEntity.ok(service.getRules(id, query));
    }

    @PostMapping("/standards/{id}/rules")
    public ResponseEntity<MicroBreakpointRuleAdminForm> createRule(@PathVariable String id, HttpServletRequest request,
            @RequestBody MicroBreakpointRuleAdminForm rule) {
        return ResponseEntity.ok(service.saveRule(id, null, rule, authenticatedUserId(request)));
    }

    @PutMapping("/standards/{id}/rules/{ruleId}")
    public ResponseEntity<MicroBreakpointRuleAdminForm> updateRule(@PathVariable String id, @PathVariable String ruleId,
            HttpServletRequest request, @RequestBody MicroBreakpointRuleAdminForm rule) {
        return ResponseEntity.ok(service.saveRule(id, ruleId, rule, authenticatedUserId(request)));
    }

    @PostMapping("/standards/{id}/activate")
    public ResponseEntity<Map<String, String>> activate(@PathVariable String id, @RequestParam Date effectiveDate,
            HttpServletRequest request) {
        service.activate(id, effectiveDate, authenticatedUserId(request));
        return ResponseEntity.ok(Map.of("status", "ACTIVE"));
    }

    @PostMapping("/standards/{id}/archive")
    public ResponseEntity<Map<String, String>> archive(@PathVariable String id, HttpServletRequest request) {
        service.archive(id, authenticatedUserId(request));
        return ResponseEntity.ok(Map.of("status", "ARCHIVED"));
    }

    @PostMapping(value = "/imports/preview", consumes = { "text/csv", MediaType.TEXT_PLAIN_VALUE })
    public ResponseEntity<MicroBreakpointImportPreviewForm> previewImport(@RequestBody String csv) {
        return ResponseEntity.ok(importService.preview(csv));
    }

    @PostMapping("/imports/{previewToken}/apply")
    public ResponseEntity<MicroBreakpointImportPreviewForm> applyImport(@PathVariable String previewToken,
            HttpServletRequest request) {
        return ResponseEntity.ok(importService.apply(previewToken, authenticatedUserId(request)));
    }
}
