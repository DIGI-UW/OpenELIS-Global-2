package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseTimelineRestController;
import org.openelisglobal.microbiology.form.MicroCaseActivityForm;
import org.openelisglobal.microbiology.form.MicroCaseNoteRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseTimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseTimelineRestControllerTest {

    @Test
    public void addNoteUsesAuthenticatedActor() {
        MicroCaseTimelineService service = org.mockito.Mockito.mock(MicroCaseTimelineService.class);
        MicroCaseNoteRequestForm request = new MicroCaseNoteRequestForm();
        request.text = "Plate reading remains negative";
        MicroCaseActivityForm form = new MicroCaseActivityForm();
        form.note = request.text;
        when(service.addNote("case-1", request.text, "42")).thenReturn(form);

        ResponseEntity<MicroCaseActivityForm> response = new MicroCaseTimelineRestController(service).addNote("case-1",
                request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(request.text, response.getBody().note);
        verify(service).addNote("case-1", request.text, "42");
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
