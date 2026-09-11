package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public class ResultUtilUserRoleTest extends BaseWebContextSensitiveTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/result-edit-role.xml");
        resetCachedResultEditRoleId();
    }

    @Test
    public void userNotInRole_returnsFalseWhenUserHasResultsRole() {
        MockHttpServletRequest request = requestForSystemUser(101);
        authenticateNonAdmin(request, "resultsuser");
        assertFalse(ResultUtil.userNotInRole(request));
    }

    @Test
    public void userNotInRole_returnsTrueWhenUserLacksResultsRole() {
        MockHttpServletRequest request = requestForSystemUser(102);
        authenticateNonAdmin(request, "noresultsuser");
        assertTrue(ResultUtil.userNotInRole(request));
    }

    private void authenticateNonAdmin(MockHttpServletRequest request, String loginName) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(loginName, "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private MockHttpServletRequest requestForSystemUser(int systemUserId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData sessionData = new UserSessionData();
        sessionData.setSytemUserId(systemUserId);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, sessionData);
        return request;
    }

    private void resetCachedResultEditRoleId() throws Exception {
        Field field = ResultUtil.class.getDeclaredField("RESULT_EDIT_ROLE_ID");
        field.setAccessible(true);
        field.set(null, null);
    }
}
