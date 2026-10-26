package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroReportingRuleForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleHistoryForm;
import org.openelisglobal.microbiology.form.MicroReportingRulePreviewForm;
import org.openelisglobal.microbiology.form.MicroReportingRulePreviewResultForm;
import org.openelisglobal.microbiology.form.MicroReportingRuleSettingsForm;
import org.openelisglobal.microbiology.service.MicroReportingRuleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/admin/reporting-rules")
@PreAuthorize("hasRole('ADMIN')")
public class MicroReportingRuleRestController extends MicrobiologyRestControllerSupport {
    private final MicroReportingRuleService service;

    public MicroReportingRuleRestController(MicroReportingRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<MicroReportingRuleForm> get() {
        return service.getRules();
    }

    @PostMapping
    public MicroReportingRuleForm save(@RequestBody MicroReportingRuleForm form, HttpServletRequest request) {
        return service.toForm(service.save(form, authenticatedUserId(request)));
    }

    @PostMapping("/preview")
    public MicroReportingRulePreviewResultForm preview(@RequestBody MicroReportingRulePreviewForm form) {
        return service.preview(form);
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
