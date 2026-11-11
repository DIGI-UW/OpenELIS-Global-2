package org.openelisglobal.module.api;

/**
 * Thrown by {@link OpenElisModule#validate} to refuse to start the module. The
 * message names what is wrong (a missing property, an unreachable dependency)
 * and is recorded as the module's failure reason.
 */
public class ModuleValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModuleValidationException(String message) {
        super(message);
    }

    public ModuleValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
