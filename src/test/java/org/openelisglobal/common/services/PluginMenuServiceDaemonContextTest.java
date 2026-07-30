package org.openelisglobal.common.services;

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
 * Regression test: {@code PluginMenuService.onApplicationReady()} fires on the
 * ContextRefreshedEvent thread (no SecurityContext) and registers analyzer
 * menus/permissions through auditable services (role, system_module,
 * role_module inserts). Without a daemon context those inserts throw and are
 * swallowed, so analyzer menus were silently never created on fresh databases.
 * This pins the contract that menu initialization executes under the daemon
 * context and restores the previous (empty) context afterward.
 */
public class PluginMenuServiceDaemonContextTest {

    private static final String DAEMON_ID = "7";

    @Before
    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void onApplicationReady_initializesMenusUnderDaemonContext() {
        AtomicReference<Authentication> authDuringInit = new AtomicReference<>();
        PluginMenuService service = new PluginMenuService() {
            @Override
            public void initializeAnalyzerMenus() {
                authDuringInit.set(SecurityContextHolder.getContext().getAuthentication());
            }
        };
        DaemonContextExecutor executor = new DaemonContextExecutor();
        ReflectionTestUtils.setField(executor, "daemonSysUserId", DAEMON_ID);
        ReflectionTestUtils.setField(service, "daemonContextExecutor", executor);

        service.onApplicationReady();

        Authentication captured = authDuringInit.get();
        assertTrue("menu initialization must run under a daemon SecurityContext, got: " + captured,
                captured instanceof DaemonAuthenticationToken);
        assertEquals(DAEMON_ID, ((DaemonAuthenticationToken) captured).getDaemonSysUserId());
        assertNull("event thread's empty context must be restored after initialization",
                SecurityContextHolder.getContext().getAuthentication());
    }
}
