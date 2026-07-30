package org.openelisglobal.security;

import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to execute code blocks within a daemon SecurityContext. Use for
 * scheduled tasks, system operations, and any code that runs outside an HTTP
 * request where a user identity is needed for audit trail.
 *
 * <p>
 * The previous SecurityContext is saved and restored after execution, making
 * this safe for nested calls and mixed-context scenarios.
 */
@Component
public class DaemonContextExecutor {

    @Qualifier("daemonSysUserId")
    @Autowired
    private String daemonSysUserId;

    /**
     * Execute a Runnable as the daemon user. Restores the previous SecurityContext
     * afterward.
     */
    public void executeAsDaemon(Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext daemonContext = SecurityContextHolder.createEmptyContext();
            daemonContext.setAuthentication(new DaemonAuthenticationToken(daemonSysUserId));
            SecurityContextHolder.setContext(daemonContext);
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    /**
     * Execute a Callable as the daemon user and return the result. Restores the
     * previous SecurityContext afterward.
     */
    public <T> T executeAsDaemon(Callable<T> action) throws Exception {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContext daemonContext = SecurityContextHolder.createEmptyContext();
            daemonContext.setAuthentication(new DaemonAuthenticationToken(daemonSysUserId));
            SecurityContextHolder.setContext(daemonContext);
            return action.call();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    /**
     * Creates a SecurityContext pre-configured with the daemon authentication
     * token. Useful for configuring executors that need a default SecurityContext
     * (e.g., DelegatingSecurityContextScheduledExecutorService).
     */
    public SecurityContext createDaemonSecurityContext() {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new DaemonAuthenticationToken(daemonSysUserId));
        return ctx;
    }
}
