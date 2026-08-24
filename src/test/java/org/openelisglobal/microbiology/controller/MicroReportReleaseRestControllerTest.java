package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.controller.rest.MicroReportReleaseRestController;
import org.openelisglobal.microbiology.service.MicroReportProjectionService;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroReportReleaseRestControllerTest {

    @Test
    public void releaseActionsNeedNoUnusedRequestBody() throws Exception {
        MicroReportReleaseService releaseService = org.mockito.Mockito.mock(MicroReportReleaseService.class);
        MicroReportProjectionService projectionService = org.mockito.Mockito.mock(MicroReportProjectionService.class);
        MicroCase microCase = new MicroCase();
        microCase.setId("case-1");
        when(releaseService.releasePreliminary("case-1", "42")).thenReturn(microCase);
        when(releaseService.releaseFinal("case-1", "42")).thenReturn(microCase);
        when(releaseService.releaseAmended("case-1", "42")).thenReturn(microCase);
        MicroReportReleaseRestController controller = new MicroReportReleaseRestController(releaseService,
                projectionService);

        controller.releasePreliminary("case-1", requestFor("42"));
        controller.releaseFinal("case-1", requestFor("42"));
        controller.releaseAmended("case-1", requestFor("42"));

        verify(releaseService).releasePreliminary("case-1", "42");
        verify(releaseService).releaseFinal("case-1", "42");
        verify(releaseService).releaseAmended("case-1", "42");
        PreAuthorize authorization = MicroReportReleaseRestController.class
                .getMethod("releaseAmended", String.class, jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class);
        assertEquals("hasAnyRole('ADMIN', 'RESULTS')", authorization.value());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
