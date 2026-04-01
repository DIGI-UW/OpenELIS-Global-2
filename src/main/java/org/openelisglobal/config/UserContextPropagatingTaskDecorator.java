package org.openelisglobal.config;

import org.springframework.core.task.TaskDecorator;
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
        // Defensive copy: capture the authentication at submission time so that later
        // changes to the caller's SecurityContext (e.g., logout) do not affect the
        // task.
        SecurityContext snapshot = SecurityContextHolder.createEmptyContext();
        snapshot.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
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
