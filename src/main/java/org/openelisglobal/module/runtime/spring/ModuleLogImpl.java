package org.openelisglobal.module.runtime.spring;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.ModuleLog;

/**
 * Writes through the core's {@code LogEvent} with the module id in front of
 * every line.
 */
public final class ModuleLogImpl implements ModuleLog {

    private final String prefix;

    public ModuleLogImpl(String moduleId) {
        this.prefix = "[module:" + moduleId + "] ";
    }

    @Override
    public void info(String message) {
        LogEvent.logInfo("Module", "log", prefix + message);
    }

    @Override
    public void warn(String message) {
        LogEvent.logWarn("Module", "log", prefix + message);
    }

    @Override
    public void warn(String message, Throwable cause) {
        LogEvent.logWarn("Module", "log", prefix + message + ": " + cause);
    }

    @Override
    public void error(String message) {
        LogEvent.logError("Module", "log", prefix + message);
    }

    @Override
    public void error(String message, Throwable cause) {
        LogEvent.logError(prefix + message, cause);
    }
}
