package org.openelisglobal.module.runtime;

/**
 * A failure that stops the application from starting: a module that passed
 * validation but failed its migration, its context refresh, a contribution or
 * {@code onStart}, two JARs with one id, or a bad {@code modules.lock}. The
 * message names the module so the operator can fix it or disable it with a
 * marker file and restart.
 */
public class ModuleStartupException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModuleStartupException(String message) {
        super(message);
    }

    public ModuleStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
