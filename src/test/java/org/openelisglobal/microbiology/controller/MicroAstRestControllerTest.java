package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroAstRestController;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroAstRestControllerTest {

    @Test
    public void recordReadingRejectsMissingMethodBeforeCallingTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstReadingRequestForm request = new MicroAstReadingRequestForm();
        request.antibioticId = "abx-1";

        try {
            new MicroAstRestController(service).recordReading("run-1", request, requestFor("42"));
            fail("Expected a missing AST method to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("method is required", exception.getMessage());
        }

        verify(service, never()).recordReading(any(), any(), any(), any(), any());
    }

    @Test
    public void overrideReadingRejectsMissingInterpretationBeforeCallingTheService() {
        MicroAstService service = org.mockito.Mockito.mock(MicroAstService.class);
        MicroAstOverrideRequestForm request = new MicroAstOverrideRequestForm();
        request.overrideReason = "confirmed on repeat";

        try {
            new MicroAstRestController(service).overrideReading("reading-1", request, requestFor("42"));
            fail("Expected a missing override interpretation to be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("overrideInterpretation is required", exception.getMessage());
        }

        verify(service, never()).overrideReading(any(), any(), any(), any());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
