package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;

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
    public void getAuthorities_returnsEmpty() {
        DaemonAuthenticationToken token = new DaemonAuthenticationToken("42");
        assertTrue(token.getAuthorities().isEmpty());
    }
}
