package org.openelisglobal.analyzer.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzerresults.service.AnalyzerFailedRunService;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

@RunWith(MockitoJUnitRunner.class)
public class AnalyzerFailedRunRestControllerTest {

    @Mock
    private AnalyzerFailedRunService failedRunService;

    @InjectMocks
    private AnalyzerFailedRunRestController controller;

    @Test
    public void dismissingAFailedRunNamesTheReviewerAndReturnsTheTestItWasRecordedOn() {
        when(failedRunService.dismissAsFailedRun("1004", "17")).thenReturn("77");

        ResponseEntity<Map<String, String>> response = controller.dismissAsFailedRun("1004", authenticatedRequest(17));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("77", response.getBody().get("analysisId"));
        verify(failedRunService).dismissAsFailedRun("1004", "17");
    }

    @Test
    public void aRefusedDismissalIsAConflictTheReviewerCanRead() {
        ResponseEntity<Map<String, String>> response = controller
                .refuse(new IllegalStateException("The failed run matches no single test on an order"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("The failed run matches no single test on an order", response.getBody().get("error"));
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
