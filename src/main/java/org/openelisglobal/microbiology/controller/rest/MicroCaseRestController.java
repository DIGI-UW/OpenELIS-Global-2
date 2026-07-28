package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseActivityRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases")
public class MicroCaseRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseService caseService;
    private final MicroCaseStateService stateService;
    private final MicroCaseOrderDetailService orderDetailService;

    public MicroCaseRestController(MicroCaseService caseService, MicroCaseStateService stateService,
            MicroCaseOrderDetailService orderDetailService) {
        this.caseService = caseService;
        this.stateService = stateService;
        this.orderDetailService = orderDetailService;
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseDetailForm> getCaseDetail(@PathVariable String caseId) {
        MicroCaseDetailForm detail = caseService.getCaseDetail(caseId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroCaseLookupForm>> getCasesForSampleItem(@RequestParam String sampleItemId) {
        List<MicroCaseLookupForm> rows = new ArrayList<>();
        for (MicroCase microCase : caseService.getSiblingCases(sampleItemId)) {
            rows.add(toLookupForm(microCase));
        }
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{caseId}/activities")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseDetailForm> recordActivity(@PathVariable String caseId,
            @RequestBody MicroCaseActivityRequestForm request, HttpServletRequest httpRequest) {
        stateService.advanceStage(caseId, MicroCaseStage.valueOf(request.nextStage), authenticatedUserId(httpRequest),
                request.note);
        return ResponseEntity.ok(caseService.getCaseDetail(caseId));
    }

    @PutMapping("/{caseId}/order-detail")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCaseDetailForm> saveOrderDetail(@PathVariable String caseId,
            @RequestBody MicroCaseOrderDetailRequestForm request, HttpServletRequest httpRequest) {
        orderDetailService.saveOrderDetail(caseId, request, authenticatedUserId(httpRequest));
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
