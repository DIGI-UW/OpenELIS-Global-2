package org.openelisglobal.common.security;

import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for background threads (schedulers, startup config loaders) that need
 * to call @PreAuthorize-guarded services without a user auth context.
 *
 * Usage: SystemAuthentication.runAs(() -> myService.guardedMethod());
 */
public final class SystemAuthentication {

    public static final String ROLE_SYSTEM = "ROLE_SYSTEM";

    public static final Authentication SYSTEM_AUTH = new UsernamePasswordAuthenticationToken("system", null,
            List.of(new SimpleGrantedAuthority(ROLE_SYSTEM)));

    private SystemAuthentication() {
    }

    public static void runAs(Runnable task) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(SYSTEM_AUTH);
        SecurityContextHolder.setContext(ctx);
        try {
            task.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
