package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseProtocolRestController;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseProtocolChangeRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.service.MicroCaseProtocolService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseProtocolRestControllerTest {

    @Test
    public void optionsReturnServiceCompiledProtocolChoices() {
        MicroCaseProtocolService protocolService = mock(MicroCaseProtocolService.class);
        MicroCaseProtocolOptionForm option = new MicroCaseProtocolOptionForm();
        option.id = "method-1";
        when(protocolService.getProtocolOptions("case-1")).thenReturn(List.of(option));

        ResponseEntity<List<MicroCaseProtocolOptionForm>> response = new MicroCaseProtocolRestController(
                protocolService, mock(MicroCaseService.class)).getOptions("case-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("method-1", response.getBody().get(0).id);
    }

    @Test
    public void changeIgnoresSubmittedActorAndUsesAuthenticatedActor() throws Exception {
        MicroCaseProtocolService protocolService = mock(MicroCaseProtocolService.class);
        MicroCaseService caseService = mock(MicroCaseService.class);
        MicroCaseProtocolChangeRequestForm request = new ObjectMapper().readValue(
                "{\"cultureMethodId\":\"method-2\",\"reason\":\"Bench observation\",\"performedBy\":\"999\"}",
                MicroCaseProtocolChangeRequestForm.class);
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.cultureMethodId = "method-2";
        when(caseService.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseProtocolRestController(protocolService, caseService)
                .changeProtocol("case-1", request, requestFor("42"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals("method-2", response.getBody().cultureMethodId);
        verify(protocolService).changeProtocol("case-1", "method-2", "Bench observation", "42");
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
