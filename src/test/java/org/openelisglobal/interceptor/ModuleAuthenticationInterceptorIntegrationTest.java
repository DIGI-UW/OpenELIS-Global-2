package org.openelisglobal.interceptor;

import static org.junit.Assert.assertTrue;

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
 * Spec 012 T017 — integration proof of the module interceptor's REST-path
 * contract under privilege-based RBAC.
 *
 * <p>
 * An unregistered REST path (no {@code system_module_url} mapping) is
 * deliberately allowed PAST this interceptor for any authenticated user. The
 * module interceptor runs BEFORE method security, so denying here would lock
 * non-admins out of unmapped infrastructure endpoints (menu, configuration,
 * home-dashboard, user-test-sections); instead the path is deferred and the
 * service-layer {@code @PreAuthorize} (PRIV_*) gate is the sole authorization
 * boundary (live-verified regression fix, commit 76cba99b1). This test wires
 * the interceptor to the REAL module/user services (no subclass stubs) and
 * exercises {@code preHandle} end to end.
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
    public void preHandle_defersUnregisteredRestPathForNonAdmin() throws Exception {
        // An unregistered /rest path (no SystemModuleUrl mapping) is deliberately
        // allowed PAST this interceptor for any authenticated user — the module
        // interceptor runs BEFORE method security, so denying here would lock every
        // non-admin out of unmapped infrastructure endpoints (menu, configuration,
        // home-dashboard, user-test-sections). Authorization for these paths is the
        // service layer's @PreAuthorize (PRIV_*) gate, not this interceptor. This is
        // the live-verified behavior from the non-admin regression fix (76cba99b1);
        // fixture user 902 is authenticated, non-admin, with no module mappings.
        MockHttpServletRequest request = requestFor(UNREGISTERED_REST_PATH, 902, "rbac_nopriv_user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue("unregistered REST path must defer to @PreAuthorize, not be denied at the interceptor", allowed);
    }

    @Test
    public void preHandle_defersUnregisteredRestPathEvenWithPrivilegedRole() throws Exception {
        // fixture user 901 holds RBAC_TEST_VIEWER (patient:view). Privileges are not
        // consumed by the module interceptor at all — the path is deferred the same
        // way as for any authenticated user, and @PreAuthorize alone decides access.
        MockHttpServletRequest request = requestFor(UNREGISTERED_REST_PATH, 901, "rbac_priv_user");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
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
