package org.openelisglobal.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentication token representing the daemon/system user. This token is
 * always considered authenticated. Its principal is the constant
 * {@code "daemon"}; the daemon's system user ID is carried separately and
 * exposed via {@link #getDaemonSysUserId()}. It is placed into the
 * SecurityContext for scheduled tasks, async operations, and system-initiated
 * actions where no human user is present.
 */
public class DaemonAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final String daemonSysUserId;

    public DaemonAuthenticationToken(String daemonSysUserId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
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
