package org.openelisglobal.security;

import java.util.Collections;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication token representing the daemon/system user. This token is
 * always considered authenticated and carries the daemon system user ID as its
 * principal. It is placed into the SecurityContext for scheduled tasks, async
 * operations, and system-initiated actions where no human user is present.
 */
public class DaemonAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final String daemonSysUserId;

    public DaemonAuthenticationToken(String daemonSysUserId) {
        super(Collections.emptyList());
        this.daemonSysUserId = daemonSysUserId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return "daemon";
    }

    public String getDaemonSysUserId() {
        return daemonSysUserId;
    }
}
