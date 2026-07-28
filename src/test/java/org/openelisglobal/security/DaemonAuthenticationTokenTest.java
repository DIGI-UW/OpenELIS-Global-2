package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class DaemonAuthenticationTokenTest extends BaseWebContextSensitiveTest {

    @Test
    public void isAuthenticated_returnsTrue() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("42");
        assertTrue(token.isAuthenticated());
    }

    @Test
    public void getPrincipal_returnsDaemon() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("42");
        assertEquals("daemon", token.getPrincipal());
    }

    @Test
    public void getCredentials_returnsNull() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("42");
        assertNull(token.getCredentials());
    }

    @Test
    public void getDaemonSysUserId_returnsConfiguredId() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("99");
        assertEquals("99", token.getDaemonSysUserId());
    }

    @Test
    public void getAuthorities_containsRoleSystem() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("42");
        assertEquals(1, token.getAuthorities().size());
        assertTrue(token.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SYSTEM")));
    }

    @Autowired
    private DaemonContextExecutor daemonContextExecutor;

    @Test
    public void executeAsDaemon_securityContextCarriesRoleSystem() {
        daemonContextExecutor.executeAsDaemon(() -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue("Should be DaemonAuthenticationToken", auth instanceof DaemonAuthenticationToken);
            assertTrue("Should have ROLE_SYSTEM",
                    auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SYSTEM")));
        });
    }
}
