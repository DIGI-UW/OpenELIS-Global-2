package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.form.MicroAnalyzerEventForm;
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
    public ResponseEntity<MicroAnalyzerEventForm> receive(@RequestBody MicroAstAnalyzerEventRequestForm request,
            HttpServletRequest httpRequest) {
        AnalyzerEvent event = eventService
                .receive(
                        new MicroAstAnalyzerEventCommand(request.externalEventId, request.eventType, request.analyzerId,
                                request.sourceId, request.targetRunId, request.payload),
                        authenticatedUserId(httpRequest));
        HttpStatus status = "FAILED".equals(event.getStatus()) ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(MicroAnalyzerEventForm.from(event));
    }
}
