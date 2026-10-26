package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroReportingRuleForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleHistoryForm;
import org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm;
import org.openelisglobal.microbiology.form.MicroReportingRulePreviewResultForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleSettingsForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.service.MicroReportingRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/admin/reporting-rules")
@PreAuthorize("hasRole('ADMIN')")
public class MicroReportingRuleRestController extends MicrobiologyRestControllerSupport {
    private final MicroReportingRuleService service;
    private final MicroAstService astService;

    public MicroReportingRuleRestController(MicroReportingRuleService service, MicroAstService astService) {
        this.service = service;
        this.astService = astService;
    }

    @GetMapping
    public List<MicroReportingRuleForm> get() {
        return service.getRules();
    }

    @PostMapping
    public MicroReportingRuleForm save(@RequestBody MicroReportingRuleForm form, HttpServletRequest request) {
        return service.toForm(service.save(form, authenticatedUserId(request)));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"reporting-rules.csv\"")
                .body(service.exportCsv());
    }

    @PostMapping("/preview")
    public MicroReportingRulePreviewResultForm preview(@RequestBody MicroReportingRulePreviewForm form) {
        return astService.previewReportingRules(form);
    }

    @GetMapping("/{id}/history")
    public List<MicroReportingRuleHistoryForm> history(@PathVariable String id) {
        return service.getHistory(id);
    }

    @GetMapping("/settings")
    public MicroReportingRuleSettingsForm getSettings() {
        return service.getSettings();
    }

    @PutMapping("/settings")
    public MicroReportingRuleSettingsForm saveSettings(@RequestBody MicroReportingRuleSettingsForm body,
            HttpServletRequest request) {
        return service.saveSettings(body.writtenFor, authenticatedUserId(request));
    }
}
