package org.openelisglobal.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Programmatic assertions for daemon eligibility and context enforcement.
 *
 * <p>
 * Use {@link #assertDaemonContext()} as the first line of {@code @Scheduled}
 * methods to declare daemon eligibility (documentation-as-code). Use
 * {@link #assertAuthenticatedContext()} in AOP advice to enforce that every
 * data mutation has a SecurityContext.
 *
 * <p>
 * These exist because {@code @PreAuthorize} does not work on {@code @Scheduled}
 * methods (Spring's scheduler bypasses the method security proxy) or on
 * {@code private} methods.
 */
public final class DaemonContextAssert {

    private static final SimpleGrantedAuthority ROLE_SYSTEM = new SimpleGrantedAuthority("ROLE_SYSTEM");

    private DaemonContextAssert() {
    }

    /**
     * Asserts that the current thread is running in daemon context (has
     * {@code ROLE_SYSTEM}).
     *
     * @throws IllegalStateException if no daemon context is present
     */
    public static void assertDaemonContext() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !auth.getAuthorities().contains(ROLE_SYSTEM)) {
            throw new IllegalStateException("Expected daemon context (ROLE_SYSTEM) but none found. "
                    + "Ensure this code runs within a @Scheduled executor "
                    + "or DaemonContextExecutor.executeAsDaemon().");
        }
    }

    /**
     * Asserts that the current thread has any authenticated context — either a
     * human user or the daemon user. Use this to enforce that every data mutation
     * has a SecurityContext.
     *
     * @throws IllegalStateException if no authenticated context is present
     */
    public static void assertAuthenticatedContext() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("No authenticated SecurityContext available. "
                    + "For user operations, ensure the request is authenticated. "
                    + "For system operations, use DaemonContextExecutor.executeAsDaemon().");
        }
    }

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
