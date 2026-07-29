package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class DaemonContextExecutorTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DaemonContextExecutor executor;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void executeAsDaemon_runnable_setsDaemonContext() {
        final boolean[] wasDaemon = { false };

        executor.executeAsDaemon(() -> {
            SecurityContext ctx = SecurityContextHolder.getContext();
            wasDaemon[0] = ctx.getAuthentication() instanceof DaemonAuthenticationToken;
        });

        assertTrue("Code inside executeAsDaemon should see DaemonAuthenticationToken", wasDaemon[0]);
    }

    @Test
    public void executeAsDaemon_runnable_restoresPreviousContext() {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken("testUser", "pass");
        SecurityContext userCtx = SecurityContextHolder.createEmptyContext();
        userCtx.setAuthentication(userAuth);
        SecurityContextHolder.setContext(userCtx);

        executor.executeAsDaemon(() -> {
            // Inside daemon context
        });

        assertEquals("testUser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    public void executeAsDaemon_runnable_restoresContextAndPropagatesException() {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken("testUser", "pass");
        SecurityContext userCtx = SecurityContextHolder.createEmptyContext();
        userCtx.setAuthentication(userAuth);
        SecurityContextHolder.setContext(userCtx);

        try {
            executor.executeAsDaemon((Runnable) () -> {
                throw new RuntimeException("test error");
            });
            fail("Expected RuntimeException to propagate");
        } catch (RuntimeException e) {
            assertEquals("test error", e.getMessage());
        }

        assertEquals("testUser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    public void executeAsDaemon_callable_returnsDaemonUserId() throws Exception {
        String result = executor.executeAsDaemon(() -> {
            DaemonAuthenticationToken token = (DaemonAuthenticationToken) SecurityContextHolder.getContext()
                    .getAuthentication();
            return token.getDaemonSysUserId();
        });

        assertNotNull(result);
    }

    @Test
    public void executeAsDaemon_callable_restoresContextAndPropagatesException() {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken("testUser", "pass");
        SecurityContext userCtx = SecurityContextHolder.createEmptyContext();
        userCtx.setAuthentication(userAuth);
        SecurityContextHolder.setContext(userCtx);

        try {
            executor.executeAsDaemon((Callable<String>) () -> {
                throw new Exception("test error");
            });
            fail("Expected Exception to propagate");
        } catch (Exception e) {
            assertEquals("test error", e.getMessage());
        }

        assertEquals("testUser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    public void executeAsDaemon_nested_restoresOuterDaemonContext() {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken("testUser", "pass");
        SecurityContext userCtx = SecurityContextHolder.createEmptyContext();
        userCtx.setAuthentication(userAuth);
        SecurityContextHolder.setContext(userCtx);

        executor.executeAsDaemon(() -> {
            assertTrue(SecurityContextHolder.getContext().getAuthentication() instanceof DaemonAuthenticationToken);

            // Nested daemon call
            executor.executeAsDaemon(() -> {
                assertTrue(SecurityContextHolder.getContext().getAuthentication() instanceof DaemonAuthenticationToken);
            });

            // After inner returns, outer daemon context should be restored
            assertTrue(SecurityContextHolder.getContext().getAuthentication() instanceof DaemonAuthenticationToken);
        });

        // After all daemon calls, original user context should be restored
        assertEquals("testUser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    public void createDaemonSecurityContext_returnsPreconfiguredContext() {
        SecurityContext ctx = executor.createDaemonSecurityContext();

        assertNotNull(ctx);
        assertNotNull(ctx.getAuthentication());
        assertTrue(ctx.getAuthentication() instanceof DaemonAuthenticationToken);
    }
}
