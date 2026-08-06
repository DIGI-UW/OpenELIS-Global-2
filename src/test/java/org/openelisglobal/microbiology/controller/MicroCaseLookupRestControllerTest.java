package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicroCaseWorkflowService;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseLookupRestControllerTest {

    @Test
    public void getCasesForSampleItemReturnsSiblingCaseLookupRows() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        MicroCase bacteriology = caseRow("case-1", MicroWorkflowType.BACTERIOLOGY);
        MicroCase tb = caseRow("case-2", MicroWorkflowType.MYCOBACTERIOLOGY_TB);
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessSampleItem("1001", "7", false)).thenReturn(true);
        when(service.getSiblingCases("1001")).thenReturn(List.of(bacteriology, tb));

        ResponseEntity<List<MicroCaseLookupForm>> response = new MicroCaseRestController(service, accessService,
                userModuleService, org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class)).getCasesForSampleItem("1001", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertEquals("case-1", response.getBody().get(0).id);
        assertEquals(MicroWorkflowType.BACTERIOLOGY.name(), response.getBody().get(0).workflowType);
    }

    @Test
    public void getCasesForSampleItemRejectsUserWithoutLabUnitAccessBeforeLoadingPatientData() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        UserModuleService userModuleService = org.mockito.Mockito.mock(UserModuleService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        when(userModuleService.isUserAdmin(request)).thenReturn(false);
        when(accessService.canAccessSampleItem("1001", "7", false)).thenReturn(false);

        ResponseEntity<List<MicroCaseLookupForm>> response = new MicroCaseRestController(service, accessService,
                userModuleService, org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class),
                org.mockito.Mockito.mock(MicroCaseWorkflowService.class)).getCasesForSampleItem("1001", request);

        assertEquals(403, response.getStatusCode().value());
        verify(service, never()).getSiblingCases("1001");
    }

    private MicroCase caseRow(String caseId, MicroWorkflowType workflowType) {
        MicroCase microCase = new MicroCase();
        microCase.setId(caseId);
        microCase.setSampleItemId("1001");
        microCase.setWorkflowType(workflowType.name());
        return microCase;
    }

    private MockHttpServletRequest authenticatedRequest(int systemUserId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData userSessionData = new UserSessionData();
        userSessionData.setSytemUserId(systemUserId);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, userSessionData);
        return request;
    }
}
