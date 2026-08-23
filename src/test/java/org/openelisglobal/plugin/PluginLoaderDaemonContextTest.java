package org.openelisglobal.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.security.DaemonAuthenticationToken;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression test: {@code PluginLoader.load()} runs in @PostConstruct on the
 * unauthenticated bootstrap thread, and legacy plugin {@code connect()}
 * persists Analyzer rows / test mappings / menu-permission bindings through
 * auditable services. Without a daemon SecurityContext those writes throw
 * (swallowed by broad catches), silently breaking legacy analyzer plugin
 * registration. This pins the contract that the plugin-loading body executes
 * under the daemon context and that the previous context is restored after.
 */
public class PluginLoaderDaemonContextTest {

    private static final String DAEMON_ID = "7";

    @Before
    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void load_runsPluginLoadingUnderDaemonContext() {
        AtomicReference<Authentication> authDuringLoad = new AtomicReference<>();
        PluginLoader loader = new PluginLoader() {
            @Override
            void doLoad() {
                authDuringLoad.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };
        DaemonContextExecutor executor = new DaemonContextExecutor();
        ReflectionTestUtils.setField(executor, "daemonSysUserId", DAEMON_ID);
        ReflectionTestUtils.setField(loader, "daemonContextExecutor", executor);

        loader.load();

        Authentication captured = authDuringLoad.get();
        assertTrue("plugin loading must run under a daemon SecurityContext, got: " + captured,
                captured instanceof DaemonAuthenticationToken);
        assertEquals(DAEMON_ID, ((DaemonAuthenticationToken) captured).getDaemonSysUserId());
        assertNull("bootstrap thread's empty context must be restored after loading",
                SecurityContextHolder.getContext().getAuthentication());
    }
}
