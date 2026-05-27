package org.openelisglobal.config;

import jakarta.annotation.Nullable;
import java.util.concurrent.Executor;
import org.openelisglobal.common.security.SystemAuthentication;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableAsync
public class AsyncConfig extends AsyncConfigurerSupport {
    @Override
    public Executor getAsyncExecutor() {
        SecurityContext systemContext = SecurityContextHolder.createEmptyContext();
        systemContext.setAuthentication(SystemAuthentication.SYSTEM_AUTH);
        return new DelegatingSecurityContextExecutor(new SimpleAsyncTaskExecutor(), systemContext);
    }

    @Override
    @Nullable
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }
}
