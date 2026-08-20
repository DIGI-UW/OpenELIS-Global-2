package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseInoculationRestController;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.form.MicroCaseInoculationRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseInoculationService;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseInoculationRestControllerTest {

    @Test
    public void recordUsesAuthenticatedActorAndReturnsCreatedRecord() {
        MicroCaseInoculationService service = org.mockito.Mockito.mock(MicroCaseInoculationService.class);
        MicroCaseInoculationRequestForm request = new MicroCaseInoculationRequestForm();
        request.containerIdentifier = "BOTTLE-001";
        request.media = "Blood agar";
        MicroCaseInoculation inoculation = new MicroCaseInoculation();
        inoculation.setId("inoculation-1");
        MicroCaseInoculationForm form = new MicroCaseInoculationForm();
        form.id = "inoculation-1";
        when(service.record(eq("case-1"), eq(null), eq("BOTTLE-001"), eq("Blood agar"), eq(null), eq(null),
                eq(List.of()), eq("42"))).thenReturn(inoculation);
        when(service.getByCaseId("case-1")).thenReturn(List.of(form));

        ResponseEntity<MicroCaseInoculationForm> response = new MicroCaseInoculationRestController(service)
                .record("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("inoculation-1", response.getBody().id);
        verify(service).record("case-1", null, "BOTTLE-001", "Blood agar", null, null, List.of(), "42");
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
