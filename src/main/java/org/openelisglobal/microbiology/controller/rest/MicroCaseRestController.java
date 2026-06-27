package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.microbiology.form.MicroCaseActivityRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases")
public class MicroCaseRestController extends BaseRestController {

    private final MicroCaseService caseService;
    private final MicrobiologyCaseAccessService accessService;
    private final UserModuleService userModuleService;
    private final MicroCaseStateService stateService;

    public MicroCaseRestController(MicroCaseService caseService, MicrobiologyCaseAccessService accessService,
            UserModuleService userModuleService, MicroCaseStateService stateService) {
        this.caseService = caseService;
        this.accessService = accessService;
        this.userModuleService = userModuleService;
        this.stateService = stateService;
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')")
    public ResponseEntity<MicroCaseDetailForm> getCaseDetail(@PathVariable String caseId, HttpServletRequest request) {
        if (!accessService.canAccessCase(caseId, getSysUserId(request), userModuleService.isUserAdmin(request))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        MicroCaseDetailForm detail = caseService.getCaseDetail(caseId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'VALIDATION')")
    public ResponseEntity<List<MicroCaseLookupForm>> getCasesForSampleItem(@RequestParam String sampleItemId,
            HttpServletRequest request) {
        if (!accessService.canAccessSampleItem(sampleItemId, getSysUserId(request),
                userModuleService.isUserAdmin(request))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<MicroCaseLookupForm> rows = new ArrayList<>();
        for (MicroCase microCase : caseService.getSiblingCases(sampleItemId)) {
            rows.add(toLookupForm(microCase));
        }
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{caseId}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseDetailForm> recordActivity(@PathVariable String caseId,
            @RequestBody MicroCaseActivityRequestForm request) {
        stateService.advanceStage(caseId, MicroCaseStage.valueOf(request.nextStage), request.performedBy, request.note);
        return ResponseEntity.ok(caseService.getCaseDetail(caseId));
    }

    private MicroCaseLookupForm toLookupForm(MicroCase microCase) {
        MicroCaseLookupForm form = new MicroCaseLookupForm();
        form.id = microCase.getId();
        form.sampleItemId = microCase.getSampleItemId();
        form.workflowType = microCase.getWorkflowType();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        return form;
    }
}
