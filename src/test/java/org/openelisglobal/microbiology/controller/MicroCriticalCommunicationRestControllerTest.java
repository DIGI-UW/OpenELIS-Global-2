package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCriticalCommunicationRestController;
import org.openelisglobal.microbiology.form.MicroCriticalCommunicationRequestForm;
import org.openelisglobal.microbiology.service.MicroCriticalCommunicationService;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCriticalCommunicationRestControllerTest {

    @Test
    public void logCommunicationRejectsUnsupportedTargetBeforeCallingTheService() {
        MicroCriticalCommunicationService service = org.mockito.Mockito.mock(MicroCriticalCommunicationService.class);
        MicroCriticalCommunicationRequestForm request = new MicroCriticalCommunicationRequestForm();
        request.targetType = "unsupported";

        try {
            new MicroCriticalCommunicationRestController(service).logCommunication("case-1", request, requestFor("42"));
            fail("Expected an unsupported target type to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("targetType is invalid", exception.getMessage());
        }

        verify(service, never()).logCommunication(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
