package org.openelisglobal.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/**
 * Factory that creates a SecurityContext containing a
 * {@link DaemonAuthenticationToken} with {@code ROLE_SYSTEM} for use in tests
 * annotated with {@link WithDaemonUser}.
 */
public class WithDaemonUserSecurityContextFactory implements WithSecurityContextFactory<WithDaemonUser> {

    @Override
    public SecurityContext createSecurityContext(WithDaemonUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new DaemonAuthenticationToken("1"));
        return context;
    }
}
