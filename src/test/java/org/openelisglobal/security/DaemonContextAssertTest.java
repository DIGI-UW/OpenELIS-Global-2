package org.openelisglobal.security;

import org.junit.After;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class DaemonContextAssertTest extends BaseWebContextSensitiveTest {

    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // --- assertDaemonContext ---

    @Test(expected = IllegalStateException.class)
    public void assertDaemonContext_noContext_throws() {
        SecurityContextHolder.clearContext();
        DaemonContextAssert.assertDaemonContext();
    }

    @Test
    @WithDaemonUser
    public void assertDaemonContext_withDaemonUser_passes() {
        DaemonContextAssert.assertDaemonContext(); // should not throw
    }

    @Test(expected = IllegalStateException.class)
    public void assertDaemonContext_withHumanUser_throws() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("labtech", "pass",
                AuthorityUtils.createAuthorityList("ROLE_RESULTS")));
        DaemonContextAssert.assertDaemonContext();
    }

    // --- assertAuthenticatedContext ---

    @Test(expected = IllegalStateException.class)
    public void assertAuthenticatedContext_noContext_throws() {
        SecurityContextHolder.clearContext();
        DaemonContextAssert.assertAuthenticatedContext();
    }

    @Test
    @WithDaemonUser
    public void assertAuthenticatedContext_withDaemonUser_passes() {
        DaemonContextAssert.assertAuthenticatedContext(); // should not throw
    }

    @Test
    public void assertAuthenticatedContext_withHumanUser_passes() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("labtech", "pass",
                AuthorityUtils.createAuthorityList("ROLE_RESULTS")));
        DaemonContextAssert.assertAuthenticatedContext(); // should not throw
    }
}
