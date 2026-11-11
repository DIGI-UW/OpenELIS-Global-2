package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

public class ControllerUtillsTest {

    @Test
    public void requestScopedUserDataDoesNotCreateAnHttpSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(42);
        request.setAttribute(IActionConstants.USER_SESSION_DATA, user);

        assertNull(request.getSession(false));
        assertEquals("42", ControllerUtills.getSysUserId(request));
        assertNull("Reading the audit actor must not create a session", request.getSession(false));
    }

    @Test
    public void existingBrowserSessionUserDataIsResolvedWithoutReplacingTheSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        UserSessionData user = new UserSessionData();
        user.setSytemUserId(43);
        session.setAttribute(IActionConstants.USER_SESSION_DATA, user);
        request.setSession(session);

        assertEquals("43", ControllerUtills.getSysUserId(request));
        assertSame(session, request.getSession(false));
    }
}
