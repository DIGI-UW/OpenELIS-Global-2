package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpServletRequest;

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
}
