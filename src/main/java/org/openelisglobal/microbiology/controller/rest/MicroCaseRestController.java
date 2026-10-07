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
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
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
@PreAuthorize("isAuthenticated()")
public class MicroCaseRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseService caseService;
    private final MicrobiologyCaseAccessService accessService;
    private final MicroCaseStateService stateService;
    private final MicroCaseOrderDetailService orderDetailService;

    public MicroCaseRestController(MicroCaseService caseService, MicrobiologyCaseAccessService accessService,
            MicroCaseStateService stateService, MicroCaseOrderDetailService orderDetailService) {
        this.caseService = caseService;
        this.accessService = accessService;
        this.stateService = stateService;
        this.orderDetailService = orderDetailService;
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<MicroCaseDetailForm> getCaseDetail(@PathVariable String caseId, HttpServletRequest request) {
        if (!accessService.canReadCase(caseId, authenticatedUserId(request))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        MicroCaseDetailForm detail = getCaseDetailForUser(caseId, authenticatedUserId(request));
        if (detail == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<List<MicroCaseLookupForm>> getCasesForSampleItem(@RequestParam String sampleItemId,
            HttpServletRequest request) {
        authenticatedUserId(request);
        List<MicroCaseLookupForm> rows = new ArrayList<>();
        for (MicroCase microCase : caseService.getSiblingCases(sampleItemId)) {
            rows.add(toLookupForm(microCase));
        }
        return ResponseEntity.ok(rows);
    }

    @PostMapping("/{caseId}/activities")
    public ResponseEntity<MicroCaseDetailForm> recordActivity(@PathVariable String caseId,
            @RequestBody MicroCaseActivityRequestForm request, HttpServletRequest httpRequest) {
        accessService.requireResults(caseId, authenticatedUserId(httpRequest));
        MicroCaseStage nextStage = requiredEnum(MicroCaseStage.class, request.nextStage, "nextStage");
        if (request.sourceSampleItemId != null) {
            stateService.advanceStage(caseId, nextStage, authenticatedUserId(httpRequest), request.note,
                    lotSelections(request.lotSelections), request.sourceSampleItemId);
        } else if (request.lotSelections == null || request.lotSelections.isEmpty()) {
            stateService.advanceStage(caseId, nextStage, authenticatedUserId(httpRequest), request.note);
        } else {
            stateService.advanceStage(caseId, nextStage, authenticatedUserId(httpRequest), request.note,
                    lotSelections(request.lotSelections));
        }
        return ResponseEntity.ok(getCaseDetailForUser(caseId, authenticatedUserId(httpRequest)));
    }

    @PutMapping("/{caseId}/order-detail")
    public ResponseEntity<MicroCaseDetailForm> saveOrderDetail(@PathVariable String caseId,
            @RequestBody MicroCaseOrderDetailRequestForm request, HttpServletRequest httpRequest) {
        accessService.requireResults(caseId, authenticatedUserId(httpRequest));
        orderDetailService.saveOrderDetail(caseId, request, authenticatedUserId(httpRequest));
        return ResponseEntity.ok(getCaseDetailForUser(caseId, authenticatedUserId(httpRequest)));
    }

    private MicroCaseDetailForm getCaseDetailForUser(String caseId, String userId) {
        MicroCaseDetailForm detail = caseService.getCaseDetail(caseId);
        if (detail != null) {
            detail.canEnterResults = accessService.canEnterResults(caseId, userId);
            detail.canValidateResults = accessService.canValidateResults(caseId, userId);
        }
        return detail;
    }

    private MicroCaseLookupForm toLookupForm(MicroCase microCase) {
        MicroCaseLookupForm form = new MicroCaseLookupForm();
        form.id = microCase.getId();
        form.sampleId = microCase.getSampleId();
        form.testSectionId = microCase.getTestSectionId();
        form.stage = microCase.getStage();
        form.priority = microCase.getPriority();
        return form;
    }
}
