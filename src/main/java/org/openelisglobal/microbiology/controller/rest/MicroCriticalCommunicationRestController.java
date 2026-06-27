package org.openelisglobal.microbiology.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroCriticalCommunicationForm;
import org.openelisglobal.microbiology.form.MicroCriticalCommunicationRequestForm;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
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
@RequestMapping("/rest/microbiology")
public class MicroCriticalCommunicationRestController extends BaseRestController {

    private final MicroCriticalCommunicationService communicationService;

    public MicroCriticalCommunicationRestController(MicroCriticalCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/cases/{caseId}/critical-communications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroCriticalCommunicationForm>> getCommunications(@PathVariable String caseId) {
        List<MicroCriticalCommunicationForm> forms = new ArrayList<>();
        for (MicroCriticalCommunication communication : communicationService.getByCaseId(caseId)) {
            forms.add(toForm(communication));
        }
        return ResponseEntity.ok(forms);
    }

    @PostMapping("/cases/{caseId}/critical-communications")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCriticalCommunicationForm> logCommunication(@PathVariable String caseId,
            @RequestBody MicroCriticalCommunicationRequestForm request) {
        return ResponseEntity.ok(toForm(communicationService.logCommunication(caseId, request.recipient,
                request.message, request.followUpNeeded, request.performedBy)));
    }

    @PutMapping("/critical-communications/{communicationId}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroCriticalCommunicationForm> acknowledge(@PathVariable String communicationId,
            @RequestBody MicroCriticalCommunicationRequestForm request) {
        return ResponseEntity.ok(toForm(communicationService.acknowledge(communicationId, request.performedBy)));
    }

    private MicroCriticalCommunicationForm toForm(MicroCriticalCommunication communication) {
        MicroCriticalCommunicationForm form = new MicroCriticalCommunicationForm();
        form.id = communication.getId();
        form.caseId = communication.getCaseId();
        form.recipient = communication.getRecipient();
        form.message = communication.getMessage();
        form.communicatedAt = communication.getCommunicatedAt();
        form.communicatedBy = communication.getCommunicatedBy();
        form.acknowledgementStatus = communication.getAcknowledgementStatus();
        form.acknowledgedAt = communication.getAcknowledgedAt();
        form.acknowledgedBy = communication.getAcknowledgedBy();
        form.followUpNeeded = Boolean.TRUE.equals(communication.getFollowUpNeeded());
        form.correctionOfId = communication.getCorrectionOfId();
        return form;
    }
}
