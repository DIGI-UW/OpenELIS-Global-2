package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.microbiology.form.MicroCaseActivityRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.form.MicroCaseOrderDetailRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseWorkflowChangeRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroCaseWorkflowService;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
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
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseService caseService;
    private final MicrobiologyCaseAccessService accessService;
    private final UserModuleService userModuleService;
    private final MicroCaseStateService stateService;
    private final MicroCaseOrderDetailService orderDetailService;
    private final MicroCaseWorkflowService workflowService;

    public MicroCaseRestController(MicroCaseService caseService, MicrobiologyCaseAccessService accessService,
            UserModuleService userModuleService, MicroCaseStateService stateService,
            MicroCaseOrderDetailService orderDetailService, MicroCaseWorkflowService workflowService) {
        this.caseService = caseService;
        this.accessService = accessService;
        this.userModuleService = userModuleService;
        this.stateService = stateService;
        this.orderDetailService = orderDetailService;
        this.workflowService = workflowService;
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<MicroCaseDetailForm> getCaseDetail(@PathVariable String caseId, HttpServletRequest request) {
        if (!accessService.canAccessCase(caseId, authenticatedUserId(request),
                userModuleService.isUserAdmin(request))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        MicroCaseDetailForm detail = caseService.getCaseDetail(caseId);
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<List<MicroCaseLookupForm>> getCasesForSampleItem(@RequestParam String sampleItemId,
            HttpServletRequest request) {
        if (!accessService.canAccessSampleItem(sampleItemId, authenticatedUserId(request),
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
    public ResponseEntity<MicroCaseDetailForm> recordActivity(@PathVariable String caseId,
            @RequestBody MicroCaseActivityRequestForm request, HttpServletRequest httpRequest) {
        MicroCaseStage nextStage = requiredEnum(MicroCaseStage.class, request.nextStage, "nextStage");
        if (request.lotSelections == null || request.lotSelections.isEmpty()) {
            stateService.advanceStage(caseId, nextStage, authenticatedUserId(httpRequest), request.note);
        } else {
            stateService.advanceStage(caseId, nextStage, authenticatedUserId(httpRequest), request.note,
                    lotSelections(request.lotSelections));
        }
        return ResponseEntity.ok(caseService.getCaseDetail(caseId));
    }

    @PutMapping("/{caseId}/order-detail")
    public ResponseEntity<MicroCaseDetailForm> saveOrderDetail(@PathVariable String caseId,
            @RequestBody MicroCaseOrderDetailRequestForm request, HttpServletRequest httpRequest) {
        orderDetailService.saveOrderDetail(caseId, request, authenticatedUserId(httpRequest));
        return ResponseEntity.ok(caseService.getCaseDetail(caseId));
    }

    @PutMapping("/{caseId}/workflow")
    public ResponseEntity<MicroCaseDetailForm> changeWorkflow(@PathVariable String caseId,
            @RequestBody MicroCaseWorkflowChangeRequestForm request, HttpServletRequest httpRequest) {
        MicroWorkflowType workflowType = request.workflowType == null ? null
                : MicroWorkflowType.valueOf(request.workflowType);
        workflowService.changeWorkflow(caseId, workflowType, request.cultureMethodId, request.reason,
                request.preserveExistingWorkConfirmed, authenticatedUserId(httpRequest));
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
