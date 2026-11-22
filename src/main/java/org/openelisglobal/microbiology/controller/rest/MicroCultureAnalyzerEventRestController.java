package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.microbiology.form.MicroAnalyzerEventForm;
import org.openelisglobal.microbiology.form.MicroCultureAnalyzerEventRequestForm;
import org.openelisglobal.microbiology.service.MicroCultureAnalyzerEventCommand;
import org.openelisglobal.microbiology.service.MicroCultureAnalyzerEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/analyzer/events/culture")
public class MicroCultureAnalyzerEventRestController extends MicrobiologyRestControllerSupport {

    private final MicroCultureAnalyzerEventService eventService;

    public MicroCultureAnalyzerEventRestController(MicroCultureAnalyzerEventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ANALYSER_IMPORT')")
    public ResponseEntity<MicroAnalyzerEventForm> receive(@RequestBody MicroCultureAnalyzerEventRequestForm request,
            HttpServletRequest httpRequest) {
        AnalyzerEvent event = eventService
                .receive(
                        new MicroCultureAnalyzerEventCommand(request.externalEventId, request.eventType,
                                request.analyzerId, request.sourceId, request.targetCaseId, request.note),
                        authenticatedUserId(httpRequest));
        HttpStatus status = "FAILED".equals(event.getStatus()) ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(MicroAnalyzerEventForm.from(event));
    }
}
