package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroCaseRestController;
import org.openelisglobal.microbiology.form.MicroCaseLookupForm;
import org.openelisglobal.microbiology.service.MicroCaseOrderDetailService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.openelisglobal.microbiology.service.MicroCaseStateService;
import org.openelisglobal.microbiology.service.MicrobiologyCaseAccessService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class MicroCaseLookupRestControllerTest {

    @Test
    public void getCasesForSampleItemReturnsSiblingCaseLookupRows() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        MicrobiologyCaseAccessService accessService = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        MockHttpServletRequest request = authenticatedRequest(7);
        MicroCase bacteriology = caseRow("case-1", "9");
        MicroCase tb = caseRow("case-2", "10");
        when(service.getSiblingCases("1001")).thenReturn(List.of(bacteriology, tb));

        ResponseEntity<List<MicroCaseLookupForm>> response = new MicroCaseRestController(service, accessService,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class)).getCasesForSampleItem("1001", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
        assertEquals("case-1", response.getBody().get(0).id);
        assertEquals("9", response.getBody().get(0).testSectionId);
        assertEquals("10", response.getBody().get(1).testSectionId);
    }

    @Test
    public void directCaseLinkReturnsReadOnlyPermissionsForAuthenticatedUserWithoutUnitRoles() {
        MicroCaseService service = org.mockito.Mockito.mock(MicroCaseService.class);
        var access = org.mockito.Mockito.mock(MicrobiologyCaseAccessService.class);
        var detail = new org.openelisglobal.microbiology.form.MicroCaseDetailForm();
        detail.id = "case-1";
        when(access.canReadCase("case-1", "7")).thenReturn(true);
        when(service.getCaseDetail("case-1")).thenReturn(detail);
        var controller = new MicroCaseRestController(service, access,
                org.mockito.Mockito.mock(MicroCaseStateService.class),
                org.mockito.Mockito.mock(MicroCaseOrderDetailService.class));

        var response = controller.getCaseDetail("case-1", authenticatedRequest(7));

        assertEquals(200, response.getStatusCode().value());
        org.junit.Assert.assertFalse(response.getBody().canEnterResults);
        org.junit.Assert.assertFalse(response.getBody().canValidateResults);
        verify(access, never()).requireResults("case-1", "7");
        verify(access, never()).requireValidation("case-1", "7");
    }

    private MicroCase caseRow(String caseId, String unitId) {
        MicroCase microCase = new MicroCase();
        microCase.setId(caseId);
        microCase.setSampleId("100");
        microCase.setTestSectionId(unitId);
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
