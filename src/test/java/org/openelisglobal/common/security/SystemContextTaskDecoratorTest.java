package org.openelisglobal.common.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.core.task.TaskDecorator;

/**
 * Locks the two task-decorator contracts that background RBAC relies on (see
 * {@link SystemContextTaskDecorator}).
 *
 * <p>
 * {@code systemContext()} always runs its task as the system.
 * {@code propagateSystemContext()} carries the submitter's system state across
 * the thread hop but must NOT grant system rights to a user-initiated task —
 * the invariant that keeps the {@code @Async} executor from silently escalating
 * a plain request into a privileged one.
 */
public class SystemContextTaskDecoratorTest {

    @After
    public void tearDown() {
        SystemInitFlag.clear();
    }

    @Test
    public void systemContext_setsFlagInsideTask_thenRestores() {
        boolean[] flagInsideTask = { false };
        TaskDecorator decorator = SystemContextTaskDecorator.systemContext();

        Runnable decorated = decorator.decorate(() -> flagInsideTask[0] = SystemInitFlag.isSet());
        decorated.run();

        assertTrue("systemContext() must set the flag for the task's duration", flagInsideTask[0]);
        assertFalse("flag must not leak past the task on this thread", SystemInitFlag.isSet());
    }

    @Test
    public void propagateSystemContext_carriesFlag_whenSubmitterInSystemContext() {
        boolean[] flagInsideTask = { false };
        TaskDecorator decorator = SystemContextTaskDecorator.propagateSystemContext();

        boolean wasSet = SystemInitFlag.enter();
        try {
            Runnable decorated = decorator.decorate(() -> flagInsideTask[0] = SystemInitFlag.isSet());
            decorated.run();
        } finally {
            SystemInitFlag.exit(wasSet);
        }

        assertTrue("a system-initiated task must keep system context across the hop", flagInsideTask[0]);
    }

    @Test
    public void propagateSystemContext_doesNotGrantSystem_whenSubmitterIsUser() {
        boolean[] flagInsideTask = { true };
        TaskDecorator decorator = SystemContextTaskDecorator.propagateSystemContext();

        // Submitter is NOT in system context (a real user request). The decorated
        // task must not gain system rights.
        Runnable decorated = decorator.decorate(() -> flagInsideTask[0] = SystemInitFlag.isSet());
        decorated.run();

        assertFalse("a user-initiated task must NOT be escalated to system context", flagInsideTask[0]);
    }
}
