package org.openelisglobal.config;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Captures the SecurityContext from the calling thread and propagates it to the
 * async thread. This ensures that @Async methods inherit the user identity
 * (including daemon context) from their caller.
 */
public class UserContextPropagatingTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Guard: reject @Async submissions with no SecurityContext — this is a bug,
        // not something to silently propagate.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException(
                    "Cannot propagate SecurityContext to @Async task: no authenticated context. "
                            + "Use DaemonContextExecutor.executeAsDaemon() for system operations.");
        }
        // Defensive copy: capture the authentication at submission time so that later
        // changes to the caller's SecurityContext (e.g., logout) do not affect the
        // task.
        SecurityContext snapshot = SecurityContextHolder.createEmptyContext();
        snapshot.setAuthentication(auth);
        return () -> {
            SecurityContextHolder.setContext(snapshot);
            try {
                runnable.run();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }
}
