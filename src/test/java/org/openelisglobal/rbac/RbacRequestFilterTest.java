package org.openelisglobal.rbac;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.rbac.context.RbacContext;
import org.openelisglobal.rbac.filter.RbacRequestFilter;
import org.openelisglobal.rbac.service.RbacPermissionService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Proves RbacRequestFilter correctly populates RbacContext per request.
 */
@RunWith(MockitoJUnitRunner.class)
public class RbacRequestFilterTest {

    private static final String USER_ID = "5";
    private static final String DEPT_A = "10";

    @Mock
    private RbacPermissionService rbacPermissionService;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private RbacRequestFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @After
    public void tearDown() {
        RbacContext.clear();
    }

    /** Filter runs and populates active department from loginLabUnit. */
    @Test
    public void restrictedUser_activeDeptSetFromLoginLabUnit() throws Exception {
        UserSessionData usd = sessionData(5, "alice", 10);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(rbacPermissionService.getAllowedDepartments(USER_ID)).thenReturn(List.of(DEPT_A));
        when(rbacPermissionService.getAllowedProjects(USER_ID)).thenReturn(List.of());

        filter.doFilter(request, response, chain);

        // chain must still be called
        verify(chain).doFilter(request, response);
        // context is cleared after the request
        assertNull("RbacContext must be cleared after request", RbacContext.get());
    }

    /** Filter sets activeDepartmentId = loginLabUnit for restricted user. */
    @Test
    public void restrictedUser_contextHasCorrectActiveDept() throws Exception {
        UserSessionData usd = sessionData(5, "alice", 10);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(rbacPermissionService.getAllowedDepartments(USER_ID)).thenReturn(List.of(DEPT_A));
        when(rbacPermissionService.getAllowedProjects(USER_ID)).thenReturn(List.of());

        // Capture context during chain execution
        final String[] capturedActiveDept = { null };
        final boolean[] capturedUnrestricted = { true };
        doAnswer(inv -> {
            RbacContext ctx = RbacContext.get();
            if (ctx != null) {
                capturedActiveDept[0] = ctx.getActiveDepartmentId();
                capturedUnrestricted[0] = ctx.isUnrestricted();
            }
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertEquals("Active dept must match loginLabUnit", DEPT_A, capturedActiveDept[0]);
        assertFalse("Restricted user must not be unrestricted", capturedUnrestricted[0]);
    }

    /**
     * Admin (getAllowedDepartments returns null) is unrestricted with no active
     * dept.
     */
    @Test
    public void adminUser_isUnrestrictedWithNoActiveDept() throws Exception {
        UserSessionData usd = sessionData(5, "admin", 10);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        when(rbacPermissionService.getAllowedDepartments(USER_ID)).thenReturn(null); // unrestricted
        when(rbacPermissionService.getAllowedProjects(USER_ID)).thenReturn(null);

        final boolean[] capturedUnrestricted = { false };
        final String[] capturedActiveDept = { "SOMETHING" };
        doAnswer(inv -> {
            RbacContext ctx = RbacContext.get();
            if (ctx != null) {
                capturedUnrestricted[0] = ctx.isUnrestricted();
                capturedActiveDept[0] = ctx.getActiveDepartmentId();
            }
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        assertTrue("Admin must be unrestricted", capturedUnrestricted[0]);
        assertNull("Admin must have no active dept constraint", capturedActiveDept[0]);
    }

    /** No session → context is not set, chain still runs. */
    @Test
    public void noSession_chainRunsWithoutContext() throws Exception {
        // no session attribute set
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(RbacContext.get());
    }

    /** Context is always cleared after the request, even if chain throws. */
    @Test
    public void contextAlwaysClearedAfterRequest() throws Exception {
        UserSessionData usd = sessionData(5, "alice", 10);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        when(rbacPermissionService.getAllowedDepartments(USER_ID)).thenReturn(List.of(DEPT_A));
        when(rbacPermissionService.getAllowedProjects(USER_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("chain error")).when(chain).doFilter(request, response);

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException ignored) {
        }

        assertNull("Context must be cleared even after chain exception", RbacContext.get());
    }

    private UserSessionData sessionData(int userId, String loginName, int loginLabUnit) {
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(userId);
        usd.setLoginName(loginName);
        usd.setLoginLabUnit(loginLabUnit);
        return usd;
    }
}
