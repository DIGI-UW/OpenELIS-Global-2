package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerActivationBlocker;
import org.openelisglobal.analyzer.service.AnalyzerActivationResult;
import org.openelisglobal.analyzer.service.AnalyzerActivationService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerActivationRestControllerTest {

    @Mock
    private AnalyzerActivationService service;

    private AnalyzerActivationRestController controller;

    @Before
    public void setUp() {
        controller = new AnalyzerActivationRestController(service);
    }

    @Test
    public void returnsSideEffectFreeReadiness() {
        AnalyzerActivationResult readiness = new AnalyzerActivationResult("77", Analyzer.AnalyzerStatus.VALIDATION,
                true, false, List.of());
        when(service.readiness("77")).thenReturn(readiness);

        ResponseEntity<AnalyzerActivationResult> response = controller.readiness("77");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(readiness, response.getBody());
    }

    @Test
    public void activatesThroughTheSoleLifecycleServiceWithTheAuthenticatedActor() {
        AnalyzerActivationResult activated = new AnalyzerActivationResult("77", Analyzer.AnalyzerStatus.ACTIVE, true,
                true, List.of());
        when(service.activate("77", "17")).thenReturn(activated);

        ResponseEntity<AnalyzerActivationResult> response = controller.activate("77", authenticatedRequest(17));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(activated, response.getBody());
        verify(service).activate("77", "17");
    }

    @Test
    public void returnsEveryActivationBlockerWithoutChangingStatus() {
        AnalyzerActivationResult blocked = new AnalyzerActivationResult("77", Analyzer.AnalyzerStatus.VALIDATION, false,
                false, List.of(new AnalyzerActivationBlocker("analyzer.activation.blocker.mappings"),
                        new AnalyzerActivationBlocker("analyzer.activation.blocker.connection")));
        when(service.activate("77", "17")).thenReturn(blocked);

        ResponseEntity<AnalyzerActivationResult> response = controller.activate("77", authenticatedRequest(17));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals(blocked, response.getBody());
    }

    private static MockHttpServletRequest authenticatedRequest(int systemUserId) {
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(systemUserId);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(IActionConstants.USER_SESSION_DATA, user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }
}
