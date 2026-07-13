package org.openelisglobal.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.dao.UserModuleService;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.systemmodule.service.SystemModuleUrlService;
import org.openelisglobal.systemusermodule.service.PermissionModuleService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Spec 012 T017 — integration proof that the legacy auto-allow gap is closed.
 *
 * <p>
 * Before the RBAC migration, a REST path with no {@code system_module_url}
 * registration was allowed for ANY authenticated user (the interceptor's
 * auto-allow branch). The migration flips that: an unregistered REST path is
 * DENIED so that service-layer {@code @PreAuthorize} is the sole gate. This
 * test wires the interceptor to the REAL module/user services (no subclass
 * stubs) and exercises {@code preHandle} end to end.
 */
public class ModuleAuthenticationInterceptorIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String UNREGISTERED_REST_PATH = "/rest/definitely-unregistered-endpoint-xyz";

    @Autowired
    private UserModuleService userModuleService;
    @Autowired
    private SystemModuleUrlService systemModuleUrlService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private PermissionModuleService<org.openelisglobal.systemusermodule.valueholder.PermissionModule> permissionModuleService;
    private ModuleAuthenticationInterceptor interceptor;

    @Before
    public void setUpInterceptor() throws Exception {
        executeDataSetWithStateManagement("testdata/rbac-privilege-enforcement.xml");
        // seeds the admin login (is_admin = Y) used by the positive control
        executeDataSetWithStateManagement("testdata/system-user.xml");
        // The interceptor bean is not part of the test component scan; construct
        // it against the real service beans so hasPermission() runs genuine
        // module-url resolution, not a stubbed branch.
        interceptor = new ModuleAuthenticationInterceptor();
        ReflectionTestUtils.setField(interceptor, "userModuleService", userModuleService);
        ReflectionTestUtils.setField(interceptor, "systemModuleUrlService", systemModuleUrlService);
        ReflectionTestUtils.setField(interceptor, "userRoleService", userRoleService);
        ReflectionTestUtils.setField(interceptor, "permissionModuleService", permissionModuleService);
    }

    private MockHttpServletRequest requestFor(String path, int systemUserId, String loginName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setContextPath("");
        request.setRequestURI(path);
        request.setServletPath(path);
        UserSessionData usd = new UserSessionData();
        usd.setSytemUserId(systemUserId);
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);
        // isUserAdmin() resolves the login through the session's persisted Spring
        // SecurityContext, exactly as a real authenticated request carries it
        SecurityContext securityContext = new SecurityContextImpl();
        // principal must be a UserDetails — getUserLogin() resolves the login
        // profile from UserDetails.getUsername()
        User principal = new User(loginName, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "N/A", principal.getAuthorities()));
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        return request;
    }

    @Test
    public void preHandle_deniesUnregisteredRestPathForNonAdmin() throws Exception {
        // fixture user 902 is authenticated, non-admin, and holds a role with NO
        // module mappings — exactly the population the old auto-allow leaked to
        MockHttpServletRequest request = requestFor(UNREGISTERED_REST_PATH, 902, "rbac_nopriv_user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse("unregistered REST path must be denied, not auto-allowed", allowed);
        assertEquals("REST denial must be a 401 JSON response, not a redirect", HttpServletResponse.SC_UNAUTHORIZED,
                response.getStatus());
    }

    @Test
    public void preHandle_deniesUnregisteredRestPathEvenWithPrivilegedRole() throws Exception {
        // fixture user 901 holds RBAC_TEST_VIEWER (patient:view) — privileges do
        // not bypass the module interceptor; only @PreAuthorize consumes them
        MockHttpServletRequest request = requestFor(UNREGISTERED_REST_PATH, 901, "rbac_priv_user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    public void preHandle_allowsAdminOnUnregisteredPath_provingDenialIsNotTriviallyTrue() throws Exception {
        // the seeded admin (system user 1, login_user.is_admin = Y) passes via the
        // isUserAdmin short-circuit — demonstrating hasPermission() genuinely
        // evaluates the user rather than denying everything unconditionally
        MockHttpServletRequest request = requestFor(UNREGISTERED_REST_PATH, 1, "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue("admin short-circuit must still allow", allowed);
    }
}
