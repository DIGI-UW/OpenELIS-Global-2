package org.openelisglobal.analyzer.config;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.openelisglobal.security.DaemonAuthenticationToken;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class BridgeRegistrationAsyncConfigTest {

    @Test
    public void registrationExecutorRunsStartupWorkAsDaemon() throws Exception {
        DaemonContextExecutor daemonContextExecutor = new DaemonContextExecutor();
        ReflectionTestUtils.setField(daemonContextExecutor, "daemonSysUserId", "1");
        BridgeRegistrationAsyncConfig config = new BridgeRegistrationAsyncConfig(daemonContextExecutor);
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) config.bridgeRegistrationExecutor();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean ranAsDaemon = new AtomicBoolean(false);

        SecurityContextHolder.clearContext();
        try {
            executor.execute(() -> {
                ranAsDaemon.set(
                        SecurityContextHolder.getContext().getAuthentication() instanceof DaemonAuthenticationToken);
                completed.countDown();
            });

            assertTrue("Bridge startup work should complete", completed.await(2, TimeUnit.SECONDS));
            assertTrue("Bridge startup work should use the daemon identity", ranAsDaemon.get());
        } finally {
            executor.shutdown();
            SecurityContextHolder.clearContext();
        }
    }
}
