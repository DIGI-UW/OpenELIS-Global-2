package org.openelisglobal.module.api;

/**
 * A logger whose every line carries the module id, so operators can tell which
 * module wrote it.
 */
public interface ModuleLog {

    void info(String message);

    void warn(String message);

    void warn(String message, Throwable cause);

    void error(String message);

    void error(String message, Throwable cause);
}
