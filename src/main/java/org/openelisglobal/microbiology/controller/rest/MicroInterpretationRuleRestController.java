package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroInterpretationRuleForm;
import org.openelisglobal.microbiology.service.MicroInterpretationRuleService;
import org.openelisglobal.microbiology.valueholder.MicroInterpretationRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/admin/interpretation-rules")
@PreAuthorize("hasRole('ADMIN')")
public class MicroInterpretationRuleRestController extends MicrobiologyRestControllerSupport {
    private final MicroInterpretationRuleService service;

    public MicroInterpretationRuleRestController(MicroInterpretationRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<MicroInterpretationRuleForm> get() {
        return service.getRules();
    }

    @PostMapping
    public MicroInterpretationRuleForm save(@RequestBody MicroInterpretationRuleForm form, HttpServletRequest request) {
        return toForm(service.save(form, authenticatedUserId(request)));
    }

    private MicroInterpretationRuleForm toForm(MicroInterpretationRule rule) {
        MicroInterpretationRuleForm form = new MicroInterpretationRuleForm();
        form.id = rule.getId();
        form.antibioticId = rule.getAntibioticId();
        form.organismId = rule.getOrganismId();
        form.organismGroup = rule.getOrganismGroup();
        form.interpretation = rule.getInterpretation();
        form.source = rule.getSource();
        form.active = rule.isActive();
        return form;
    }
}
