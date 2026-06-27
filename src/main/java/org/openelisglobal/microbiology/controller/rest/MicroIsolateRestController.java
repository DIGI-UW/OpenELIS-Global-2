package org.openelisglobal.microbiology.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/isolates")
public class MicroIsolateRestController extends BaseRestController {

    private final MicroIsolateService isolateService;

    public MicroIsolateRestController(MicroIsolateService isolateService) {
        this.isolateService = isolateService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroIsolateForm> createIsolate(@RequestBody MicroIsolateRequestForm request) {
        MicroIsolate isolate = isolateService.createIsolate(request.caseId, request.isolateLabel, request.organismId,
                request.preliminaryOrganismText, significance(request.significance), request.performedBy);
        return ResponseEntity.ok(toForm(isolate));
    }

    @PutMapping("/{isolateId}/identification")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroIsolateForm> updateIdentification(@PathVariable String isolateId,
            @RequestBody MicroIsolateRequestForm request) {
        MicroIsolate isolate = isolateService.updateIdentification(isolateId, request.organismId,
                request.preliminaryOrganismText, significance(request.significance),
                identificationStatus(request.identificationStatus), request.performedBy);
        return ResponseEntity.ok(toForm(isolate));
    }

    private MicroIsolateSignificance significance(String significance) {
        if (significance == null || significance.trim().isEmpty()) {
            return MicroIsolateSignificance.UNKNOWN;
        }
        return MicroIsolateSignificance.valueOf(significance);
    }

    private MicroIsolateIdentificationStatus identificationStatus(String identificationStatus) {
        if (identificationStatus == null || identificationStatus.trim().isEmpty()) {
            return MicroIsolateIdentificationStatus.PRELIMINARY;
        }
        return MicroIsolateIdentificationStatus.valueOf(identificationStatus);
    }

    private MicroIsolateForm toForm(MicroIsolate isolate) {
        MicroIsolateForm form = new MicroIsolateForm();
        form.id = isolate.getId();
        form.caseId = isolate.getCaseId();
        form.isolateLabel = isolate.getIsolateLabel();
        form.organismId = isolate.getOrganismId();
        form.preliminaryOrganismText = isolate.getPreliminaryOrganismText();
        form.significance = isolate.getSignificance();
        form.identificationStatus = isolate.getIdentificationStatus();
        form.createdAt = isolate.getCreatedAt();
        return form;
    }
}
