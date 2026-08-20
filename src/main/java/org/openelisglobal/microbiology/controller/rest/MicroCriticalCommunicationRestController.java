package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCriticalCommunicationForm;
import org.openelisglobal.microbiology.form.MicroCriticalCommunicationRequestForm;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationTargetType;
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
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCriticalCommunicationRestController extends MicrobiologyRestControllerSupport {

    private final MicroCriticalCommunicationService communicationService;

    public MicroCriticalCommunicationRestController(MicroCriticalCommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    @GetMapping("/cases/{caseId}/critical-communications")
    public ResponseEntity<List<MicroCriticalCommunicationForm>> getCommunications(@PathVariable String caseId) {
        List<MicroCriticalCommunicationForm> forms = new ArrayList<>();
        for (MicroCriticalCommunication communication : communicationService.getByCaseId(caseId)) {
            forms.add(toForm(communication));
        }
        return ResponseEntity.ok(forms);
    }

    @PostMapping("/cases/{caseId}/critical-communications")
    public ResponseEntity<MicroCriticalCommunicationForm> logCommunication(@PathVariable String caseId,
            @RequestBody MicroCriticalCommunicationRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toForm(communicationService.logCommunication(caseId, targetType(request.targetType),
                request.targetId, request.recipient, request.recipientContact, request.communicationMethod,
                request.message, request.followUpNeeded, authenticatedUserId(httpRequest))));
    }

    @PutMapping("/critical-communications/{communicationId}/acknowledge")
    public ResponseEntity<MicroCriticalCommunicationForm> acknowledge(@PathVariable String communicationId,
            @RequestBody MicroCriticalCommunicationRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity
                .ok(toForm(communicationService.acknowledge(communicationId, authenticatedUserId(httpRequest))));
    }

    @PutMapping("/critical-communications/{communicationId}/close")
    public ResponseEntity<MicroCriticalCommunicationForm> close(@PathVariable String communicationId,
            @RequestBody MicroCriticalCommunicationRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toForm(
                communicationService.close(communicationId, request.resolutionNote, authenticatedUserId(httpRequest))));
    }

    private MicroCriticalCommunicationTargetType targetType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MicroCriticalCommunicationTargetType.CASE;
        }
        return MicroCriticalCommunicationTargetType.valueOf(value);
    }

    private MicroCriticalCommunicationForm toForm(MicroCriticalCommunication communication) {
        MicroCriticalCommunicationForm form = new MicroCriticalCommunicationForm();
        form.id = communication.getId();
        form.caseId = communication.getCaseId();
        form.targetType = communication.getTargetType();
        form.targetId = communication.getTargetId();
        form.recipient = communication.getRecipient();
        form.recipientContact = communication.getRecipientContact();
        form.communicationMethod = communication.getCommunicationMethod();
        form.message = communication.getMessage();
        form.communicatedAt = communication.getCommunicatedAt();
        form.communicatedBy = communication.getCommunicatedBy();
        form.acknowledgementStatus = communication.getAcknowledgementStatus();
        form.acknowledgedAt = communication.getAcknowledgedAt();
        form.acknowledgedBy = communication.getAcknowledgedBy();
        form.closedAt = communication.getClosedAt();
        form.closedBy = communication.getClosedBy();
        form.resolutionNote = communication.getResolutionNote();
        form.alertId = communication.getAlertId();
        form.followUpNeeded = Boolean.TRUE.equals(communication.getFollowUpNeeded());
        form.correctionOfId = communication.getCorrectionOfId();
        return form;
    }
}
