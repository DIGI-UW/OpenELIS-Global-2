package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.microbiology.form.MicroCaseQuestionnaireForm;
import org.openelisglobal.microbiology.service.MicroCaseQuestionnaireService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/questionnaire")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseQuestionnaireRestController extends MicrobiologyRestControllerSupport {
    private final MicroCaseQuestionnaireService service;

    public MicroCaseQuestionnaireRestController(MicroCaseQuestionnaireService service) {
        this.service = service;
    }

    @GetMapping
    public MicroCaseQuestionnaireForm get(@PathVariable String caseId) {
        return service.get(caseId);
    }

    @PostMapping
    public MicroCaseQuestionnaireForm save(@PathVariable String caseId, @RequestBody MicroCaseQuestionnaireForm body,
            HttpServletRequest request) {
        return service.save(caseId, body, authenticatedUserId(request));
    }
}
