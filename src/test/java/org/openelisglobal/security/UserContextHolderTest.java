package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.UserContextHolder;
import org.openelisglobal.login.service.LoginUserService;
import org.openelisglobal.login.valueholder.LoginUser;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContextHolderTest extends BaseWebContextSensitiveTest {

    @Autowired
    private UserContextHolder holder;

    @Autowired
    private LoginUserService loginUserService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- getCurrentSysUser tests ---

    @Test
    public void getCurrentSysUser_withDaemonToken_returnsDaemonUser() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new DaemonAuthenticationToken("42"));
        SecurityContextHolder.setContext(ctx);

        SystemUser user = holder.getCurrentSysUser();
        assertNotNull(user);
        assertEquals("daemon", user.getLoginName());
    }

    @Test
    public void getCurrentSysUser_withAuthenticatedUser_returnsSystemUser() {
        LoginUser adminLogin = loginUserService.getUserProfile("admin");
        if (adminLogin != null) {
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "pass"));
            SecurityContextHolder.setContext(ctx);

            SystemUser user = holder.getCurrentSysUser();
            assertNotNull(user);
            assertEquals(String.valueOf(adminLogin.getSystemUserId()), user.getId());
            assertNotNull(user.getLoginName());
        }
    }

    @Test
    public void getCurrentSysUser_withAnonymousToken_returnsNull() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        SecurityContextHolder.setContext(ctx);

        assertNull(holder.getCurrentSysUser());
    }

    @Test
    public void getCurrentSysUser_withNoAuthentication_returnsNull() {
        assertNull(holder.getCurrentSysUser());
    }

    @Test
    public void getCurrentSysUser_withUnknownUser_returnsNull() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken("nonexistent_user_xyz_12345", "pass"));
        SecurityContextHolder.setContext(ctx);

        assertNull(holder.getCurrentSysUser());
    }

    // --- getCurrentSysUserId tests (derives from getCurrentSysUser) ---

    @Test
    public void getCurrentSysUserId_withDaemonToken_returnsDaemonId() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new DaemonAuthenticationToken("42"));
        SecurityContextHolder.setContext(ctx);

        // The test daemon user has ID "1" (from AppTestConfig), but the
        // DaemonAuthenticationToken triggers the daemonSystemUser bean
        assertNotNull(holder.getCurrentSysUserId());
    }

    @Test
    public void getCurrentSysUserId_withNoAuthentication_returnsNull() {
        assertNull(holder.getCurrentSysUserId());
    }

    // --- requireSysUserId tests ---

    @Test(expected = LIMSRuntimeException.class)
    public void requireSysUserId_withNoContext_throws() {
        holder.requireSysUserId();
    }

    @Test(expected = LIMSRuntimeException.class)
    public void requireSysUserId_withAnonymousToken_throws() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        SecurityContextHolder.setContext(ctx);

        holder.requireSysUserId();
    }

    // --- isDaemonContext tests ---

    @Test
    public void isDaemonContext_withDaemonToken_returnsTrue() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new DaemonAuthenticationToken("42"));
        SecurityContextHolder.setContext(ctx);

        assertTrue(holder.isDaemonContext());
    }

    @Test
    public void isDaemonContext_withRegularUser_returnsFalse() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken("testUser", "pass"));
        SecurityContextHolder.setContext(ctx);

        assertFalse(holder.isDaemonContext());
    }

    @Test
    public void isDaemonContext_withNoAuthentication_returnsFalse() {
        assertFalse(holder.isDaemonContext());
    }

    // --- daemon accessor tests ---

    @Test
    public void getDaemonSysUser_returnsNonNull() {
        SystemUser daemonUser = holder.getDaemonSysUser();
        assertNotNull(daemonUser);
        assertNotNull(daemonUser.getId());
    }

    @Test
    public void getDaemonSysUserId_returnsNonNullId() {
        String daemonId = holder.getDaemonSysUserId();
        assertFalse("Daemon user ID should not be empty", daemonId == null || daemonId.isEmpty());
    }
}
