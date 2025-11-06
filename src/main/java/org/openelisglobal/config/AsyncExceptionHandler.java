package org.openelisglobal.config;

import java.lang.reflect.Method;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        LogEvent.logError(Thread.currentThread().getName(), method.getName(), ex.getMessage());
        LogEvent.logError(ex);
    }
}
