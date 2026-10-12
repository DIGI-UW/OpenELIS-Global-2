package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseAmendmentForm;
import org.openelisglobal.microbiology.form.MicroCaseAmendmentRequestForm;
import org.openelisglobal.microbiology.form.MicroReportVersionForm;
import org.openelisglobal.microbiology.service.MicroCaseAmendmentService;
import org.openelisglobal.microbiology.service.MicroReportVersionService;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/amendments")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseAmendmentRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseAmendmentService amendmentService;
    private final MicroReportVersionService reportVersionService;

    public MicroCaseAmendmentRestController(MicroCaseAmendmentService amendmentService,
            MicroReportVersionService reportVersionService) {
        this.amendmentService = amendmentService;
        this.reportVersionService = reportVersionService;
    }

    @GetMapping
    public ResponseEntity<List<MicroCaseAmendmentForm>> getHistory(@PathVariable String caseId) {
        return ResponseEntity.ok(amendmentService.getHistory(caseId).stream().map(this::toForm).toList());
    }

    @GetMapping("/report-versions")
    public ResponseEntity<List<MicroReportVersionForm>> getReportVersions(@PathVariable String caseId) {
        return ResponseEntity.ok(reportVersionService.getVersionForms(caseId));
    }

    @PostMapping
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroCaseAmendmentForm> open(@PathVariable String caseId,
            @RequestBody MicroCaseAmendmentRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity
                .ok(toForm(amendmentService.openAmendment(caseId, request.reason, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/current/cancel")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroCaseAmendmentForm> cancel(@PathVariable String caseId,
            @RequestBody MicroCaseAmendmentRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity
                .ok(toForm(amendmentService.cancelAmendment(caseId, request.reason, authenticatedUserId(httpRequest))));
    }

    private MicroCaseAmendmentForm toForm(MicroCaseAmendment amendment) {
        MicroCaseAmendmentForm form = new MicroCaseAmendmentForm();
        form.id = amendment.getId();
        form.caseId = amendment.getCaseId();
        form.sequenceNumber = amendment.getSequenceNumber();
        form.status = amendment.getStatus();
        form.reason = amendment.getReason();
        form.openedAt = amendment.getOpenedAt();
        form.openedBy = amendment.getOpenedBy();
        form.closedAt = amendment.getClosedAt();
        form.closedBy = amendment.getClosedBy();
        form.closingReason = amendment.getClosingReason();
        return form;
    }

}
