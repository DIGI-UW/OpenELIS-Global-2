package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseRestControllerTest {

    @Test
    public void getCaseDetailReturnsCompiledServiceDto() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        MicroCaseDetailForm detail = new MicroCaseDetailForm();
        detail.id = "case-1";
        detail.stage = MicroCaseStage.RECEIVED.name();
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("case-1", "7", false)).thenReturn(true);
        when(service.getCaseDetail("case-1")).thenReturn(detail);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(service, accessService,
                userModuleService).getCaseDetail("case-1", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("case-1", response.getBody().id);
        assertEquals(MicroCaseStage.RECEIVED.name(), response.getBody().stage);
    }

    @Test
    public void getCaseDetailReturns404WhenMissing() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("missing", "7", false)).thenReturn(true);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(service, accessService,
                userModuleService).getCaseDetail("missing", request);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    public void getCaseDetailRejectsUserWithoutLabUnitAccessBeforeLoadingPatientData() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessCase("case-1", "7", false)).thenReturn(false);

        ResponseEntity<MicroCaseDetailForm> response = new MicroCaseRestController(service, accessService,
                userModuleService).getCaseDetail("case-1", request);

        assertEquals(403, response.getStatusCode().value());
        verify(service, never()).getCaseDetail("case-1");
    }

    private MockHttpServletRequest authenticatedRequest(int systemUserId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(systemUserId);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return request;
    }
}
