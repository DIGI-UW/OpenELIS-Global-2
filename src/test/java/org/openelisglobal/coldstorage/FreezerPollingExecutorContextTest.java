package org.openelisglobal.coldstorage;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The poll fan-out submits {@code @Async} alert work from its own pool threads,
 * and UserContextPropagatingTaskDecorator throws when the submitting thread has
 * no authenticated context - so the pool itself has to carry one.
 */
public class FreezerPollingExecutorContextTest extends BaseWebContextSensitiveTest {

    @Autowired
    @Qualifier("freezerPollingExecutor")
    private ExecutorService freezerPollingExecutor;

    @Test
    public void pollingExecutorThread_shouldCarryAnAuthenticatedSecurityContext() throws Exception {
        AtomicReference<Authentication> onPollThread = new AtomicReference<>();

        CompletableFuture.runAsync(() -> onPollThread.set(SecurityContextHolder.getContext().getAuthentication()),
                freezerPollingExecutor).join();

        assertNotNull("Freezer poll threads must run with a SecurityContext or every @Async alert submission throws",
                onPollThread.get());
        assertTrue("The poll thread's authentication must be authenticated", onPollThread.get().isAuthenticated());
    }
}
