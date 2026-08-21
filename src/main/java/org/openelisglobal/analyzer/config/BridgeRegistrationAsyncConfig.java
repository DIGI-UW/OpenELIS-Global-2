package org.openelisglobal.analyzer.config;

import java.util.concurrent.Executor;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated pool for fire-and-forget bridge registration calls (avoid
 * {@link java.util.concurrent.ForkJoinPool#commonPool()} contention).
 */
@Configuration
public class BridgeRegistrationAsyncConfig {

    private final DaemonContextExecutor daemonContextExecutor;

    public BridgeRegistrationAsyncConfig(DaemonContextExecutor daemonContextExecutor) {
        this.daemonContextExecutor = daemonContextExecutor;
    }

    @Bean(name = "bridgeRegistrationExecutor")
    public Executor bridgeRegistrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(0);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("bridge-reg-");
        executor.setTaskDecorator(task -> () -> daemonContextExecutor.executeAsDaemon(task));
        executor.initialize();
        return executor;
    }
}
