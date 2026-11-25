package org.openelisglobal.module.api;

/** The lifecycle states of a module as the registry reports them. */
public enum ModuleState {

    /** The JAR was found and its manifest read. */
    DISCOVERED,

    /**
     * The manifest, the namespace rules, a declared class or the bundle failed
     * validation; the module is skipped.
     */
    INVALID,

    /**
     * The running OpenELIS version, the core API, the SPI, a dependency or a
     * conflict rule is not satisfied; the module is skipped.
     */
    INCOMPATIBLE,

    /** Switched off by a marker file or a property; the module is skipped. */
    DISABLED,

    /** Every start-up check passed; the module will be migrated and started. */
    VALIDATED,

    /** The module's changelog has been applied (or it has none). */
    MIGRATED,

    /**
     * The module's context is refreshed, its contributions are registered and
     * {@code onStart} has returned.
     */
    STARTED,

    /**
     * Migration, context refresh, a contribution or {@code onStart} failed after
     * validation.
     */
    FAILED,

    /** The application is shutting down and {@code onStop} has been called. */
    STOPPED;

    /** Whether the module contributes anything to the running application. */
    public boolean isActive() {
        return this == STARTED;
    }
}
