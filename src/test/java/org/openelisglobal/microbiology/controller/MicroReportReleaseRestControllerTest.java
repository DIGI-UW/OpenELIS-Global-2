package org.openelisglobal.microbiology.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.microbiology.service.MicroReportReleaseService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

public class MicroReportReleaseRestControllerTest {

    @Test
    public void releaseActionsNeedNoUnusedRequestBody() throws Exception {
        PreAuthorize authorization = MicroReportReleaseService.class
                .getMethod("releaseFinal", String.class, String.class).getAnnotation(PreAuthorize.class);

        assertEquals("hasAuthority('PRIV_MICRO_SUPERVISE')", authorization.value());
    }

    private MockHttpServletRequest requestFor(String userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(Integer.parseInt(userId));
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }
}
