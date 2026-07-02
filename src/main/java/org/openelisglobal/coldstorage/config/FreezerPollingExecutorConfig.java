package org.openelisglobal.coldstorage.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.openelisglobal.config.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated thread pool for per-device Modbus polling fan-out.
 *
 * <p>
 * The application-wide {@code @Scheduled} task pool (see
 * {@code org.openelisglobal.scheduler.SchedulerConfig}) is a shared, fixed-size
 * pool used by every scheduled job in the application. Reusing it for
 * per-device polling would let a large freezer fleet starve unrelated scheduled
 * jobs during a poll fan-out. This pool is sized independently (via
 * {@link FreezerMonitoringProperties#getPollPoolSize()}) and is only used to
 * isolate individual device polls from one another - the poll cycle itself is
 * still triggered by the existing {@code @Scheduled} mechanism.
 */
@Configuration
@ConditionalOnProperty(property = "org.openelisglobal.freezermonitoring.enabled", havingValue = "true")
public class FreezerPollingExecutorConfig {

    @Bean(name = "freezerPollingExecutor", destroyMethod = "shutdown")
    public ExecutorService freezerPollingExecutor(FreezerMonitoringProperties config) {
        int poolSize = Math.max(1, config.getPollPoolSize());
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "freezer-poll-" + counter.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }
}
