package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openelisglobal.analyzer.valueholder.AnalyzerEvent;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroAstAnalyzerEventRestController;
import org.openelisglobal.microbiology.form.MicroAnalyzerEventForm;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerEventRequestForm;
import org.openelisglobal.microbiology.form.MicroAstAnalyzerResultRequestForm;
import org.openelisglobal.microbiology.service.MicroAstAnalyzerEventCommand;
import org.openelisglobal.microbiology.service.MicroAstAnalyzerEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroAstAnalyzerEventRestControllerTest {

    @Test
    public void appliedEventReturnsAcceptedAndUsesTheAuthenticatedActor() {
        MicroAstAnalyzerEventService service = org.mockito.Mockito.mock(MicroAstAnalyzerEventService.class);
        AnalyzerEvent applied = event("event-1", "APPLIED");
        when(service.receive(any(MicroAstAnalyzerEventCommand.class), org.mockito.ArgumentMatchers.eq("42")))
                .thenReturn(applied);
        MicroAstAnalyzerEventRequestForm request = request("event-1");

        ResponseEntity<MicroAnalyzerEventForm> response = new MicroAstAnalyzerEventRestController(service)
                .receive(request, requestFor("42"));

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("APPLIED", response.getBody().status);
        ArgumentCaptor<MicroAstAnalyzerEventCommand> command = ArgumentCaptor
                .forClass(MicroAstAnalyzerEventCommand.class);
        verify(service).receive(command.capture(), org.mockito.ArgumentMatchers.eq("42"));
        assertEquals("analyzer-1", command.getValue().analyzerId());
        assertEquals("card-42", command.getValue().sourceId());
    }

    @Test
    public void failedEventReturnsReconciliationLocationWithoutLosingTheFailure() {
        MicroAstAnalyzerEventService service = org.mockito.Mockito.mock(MicroAstAnalyzerEventService.class);
        AnalyzerEvent failed = event("event-2", "FAILED");
        failed.setFailureReason("AST_ANALYZER_RUN_NOT_MATCHED");
        when(service.receive(any(MicroAstAnalyzerEventCommand.class), org.mockito.ArgumentMatchers.eq("42")))
                .thenReturn(failed);

        ResponseEntity<MicroAnalyzerEventForm> response = new MicroAstAnalyzerEventRestController(service)
                .receive(request("event-2"), requestFor("42"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("AST_ANALYZER_RUN_NOT_MATCHED", response.getBody().failureReason);
        assertEquals("/AnalyzerResults?view=import-issues", response.getBody().reconciliationUrl);
    }

    private MicroAstAnalyzerEventRequestForm request(String externalEventId) {
        MicroAstAnalyzerEventRequestForm request = new MicroAstAnalyzerEventRequestForm();
        request.externalEventId = externalEventId;
        request.eventType = "AST_RESULT_AVAILABLE";
        request.analyzerId = "analyzer-1";
        request.sourceId = "card-42";
        request.payload = new MicroAstAnalyzerResultRequestForm();
        return request;
    }

    private AnalyzerEvent event(String externalEventId, String status) {
        AnalyzerEvent event = new AnalyzerEvent();
        event.setExternalEventId(externalEventId);
        event.setStatus(status);
        return event;
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
