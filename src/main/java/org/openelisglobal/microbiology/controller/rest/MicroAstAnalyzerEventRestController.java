package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerEventForm;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerEventRequestForm;
import org.openelisglobal.microbiology.service.MicroAstAnalyzerEventCommand;
import org.openelisglobal.microbiology.service.MicroAstAnalyzerEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/events/ast")
public class MicroAstAnalyzerEventRestController extends MicrobiologyRestControllerSupport {

    private final MicroAstAnalyzerEventService eventService;

    public MicroAstAnalyzerEventRestController(MicroAstAnalyzerEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstAnalyzerEventForm> receive(@RequestBody MicroAstAnalyzerEventRequestForm request,
            HttpServletRequest httpRequest) {
        AnalyzerEvent event = eventService
                .receive(
                        new MicroAstAnalyzerEventCommand(request.externalEventId, request.eventType, request.analyzerId,
                                request.sourceId, request.targetRunId, request.payload),
                        authenticatedUserId(httpRequest));
        HttpStatus status = "FAILED".equals(event.getStatus()) ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(toForm(event));
    }

    private MicroAstAnalyzerEventForm toForm(AnalyzerEvent event) {
        MicroAstAnalyzerEventForm form = new MicroAstAnalyzerEventForm();
        form.id = event.getId();
        form.externalEventId = event.getExternalEventId();
        form.eventType = event.getEventType();
        form.analyzerId = event.getAnalyzerId();
        form.sourceId = event.getSourceId();
        form.targetReference = event.getTargetReference();
        form.status = event.getStatus();
        form.failureReason = event.getFailureReason();
        form.receivedAt = event.getReceivedAt();
        form.processedAt = event.getProcessedAt();
        form.reconciliationUrl = "/AnalyzerResults?view=import-issues";
        return form;
    }
}
