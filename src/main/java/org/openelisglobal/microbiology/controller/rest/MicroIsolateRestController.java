package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroIdentificationEventForm;
import org.openelisglobal.microbiology.form.MicroIsolateForm;
import org.openelisglobal.microbiology.form.MicroIsolateRequestForm;
import org.openelisglobal.microbiology.service.MicroIdentificationHistoryService;
import org.openelisglobal.microbiology.service.MicroIsolateService;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationEvent;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/isolates")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroIsolateRestController extends MicrobiologyRestControllerSupport {

    private final MicroIsolateService isolateService;
    private final MicroIdentificationHistoryService identificationHistoryService;

    public MicroIsolateRestController(MicroIsolateService isolateService,
            MicroIdentificationHistoryService identificationHistoryService) {
        this.isolateService = isolateService;
        this.identificationHistoryService = identificationHistoryService;
    }

    @PostMapping
    public ResponseEntity<MicroIsolateForm> createIsolate(@RequestBody MicroIsolateRequestForm request,
            HttpServletRequest httpRequest) {
        MicroIsolate isolate = isolateService.createIsolate(request.caseId, request.isolateLabel, request.gramStain,
                request.colonyMorphology, significance(request.significance), authenticatedUserId(httpRequest));
        return ResponseEntity.ok(toForm(isolate));
    }

    @PutMapping("/{isolateId}/identification")
    public ResponseEntity<MicroIsolateForm> updateIdentification(@PathVariable String isolateId,
            @RequestBody MicroIsolateRequestForm request, HttpServletRequest httpRequest) {
        MicroIsolate isolate = isolateService.updateIdentification(isolateId, request.organismId,
                request.preliminaryOrganismText, significance(request.significance),
                identificationStatus(request.identificationStatus), request.identificationMethod,
                request.identificationConfidence, request.identificationReason, authenticatedUserId(httpRequest));
        return ResponseEntity.ok(toForm(isolate));
    }

    @GetMapping("/{isolateId}/identification-history")
    public ResponseEntity<List<MicroIdentificationEventForm>> getIdentificationHistory(@PathVariable String isolateId) {
        return ResponseEntity
                .ok(identificationHistoryService.getHistory(isolateId).stream().map(this::toForm).toList());
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
        form.gramStain = isolate.getGramStain();
        form.colonyMorphology = isolate.getColonyMorphology();
        form.identificationMethod = isolate.getIdentificationMethod();
        form.identificationConfidence = isolate.getIdentificationConfidence();
        form.significance = isolate.getSignificance();
        form.identificationStatus = isolate.getIdentificationStatus();
        form.createdAt = isolate.getCreatedAt();
        return form;
    }

    private MicroIdentificationEventForm toForm(MicroIsolateIdentificationEvent event) {
        MicroIdentificationEventForm form = new MicroIdentificationEventForm();
        form.id = event.getId();
        form.isolateId = event.getIsolateId();
        form.amendmentId = event.getAmendmentId();
        form.eventType = event.getEventType();
        form.previousOrganismId = event.getPreviousOrganismId();
        form.previousOrganismText = event.getPreviousOrganismText();
        form.previousSignificance = event.getPreviousSignificance();
        form.previousIdentificationStatus = event.getPreviousIdentificationStatus();
        form.previousIdentificationMethod = event.getPreviousIdentificationMethod();
        form.previousIdentificationConfidence = event.getPreviousIdentificationConfidence();
        form.newOrganismId = event.getNewOrganismId();
        form.newOrganismText = event.getNewOrganismText();
        form.newSignificance = event.getNewSignificance();
        form.newIdentificationStatus = event.getNewIdentificationStatus();
        form.newIdentificationMethod = event.getNewIdentificationMethod();
        form.newIdentificationConfidence = event.getNewIdentificationConfidence();
        form.reason = event.getReason();
        form.changedAt = event.getChangedAt();
        form.changedBy = event.getChangedBy();
        return form;
    }
}
