package org.openelisglobal.common.security;

import org.springframework.core.task.TaskDecorator;

/**
 * Task decorators for background work under privilege-based RBAC.
 *
 * <p>
 * Service-layer {@code @PreAuthorize} gates require either an Authentication or
 * the {@link SystemInitFlag} (see SystemAwareSecurityExpressionRoot). Threads
 * that run background work have neither by default, so:
 *
 * <ul>
 * <li>{@link #systemContext()} — unconditionally runs the task as the system
 * (flag set for the task's duration). For the {@code @Scheduled} scheduler:
 * scheduled jobs are system-initiated by definition and have no user to
 * authenticate.</li>
 * <li>{@link #propagateSystemContext()} — captures whether the submitting
 * thread was in system context and restores that state inside the task. For the
 * {@code @Async} executor, where a user-initiated task must NOT gain system
 * rights, but a system-initiated one (e.g. startup configuration reload) must
 * not lose them across the thread hop.</li>
 * </ul>
 */
public final class SystemContextTaskDecorator {

    private SystemContextTaskDecorator() {
    }

    public static TaskDecorator systemContext() {
        return runnable -> () -> {
            boolean wasSet = SystemInitFlag.enter();
            try {
                runnable.run();
            } finally {
                SystemInitFlag.exit(wasSet);
            }
        };
    }

    public static TaskDecorator propagateSystemContext() {
        return runnable -> {
            boolean submitterInSystemContext = SystemInitFlag.isSet();
            if (!submitterInSystemContext) {
                return runnable;
            }
            return () -> {
                boolean wasSet = SystemInitFlag.enter();
                try {
                    runnable.run();
                } finally {
                    SystemInitFlag.exit(wasSet);
                }
            };
        };
    }
}
