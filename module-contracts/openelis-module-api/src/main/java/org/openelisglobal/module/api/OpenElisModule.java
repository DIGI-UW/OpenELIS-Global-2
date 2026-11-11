package org.openelisglobal.module.api;

/**
 * The lifecycle hooks of a module, implemented once per module by the class
 * named as {@code backend.activator} in its manifest. The runtime instantiates
 * it as a bean of the module's own Spring context, so it may inject the
 * module's beans and any core service.
 *
 * <p>
 * Every method has an empty default: a module that needs no lifecycle code
 * names a class that implements this interface and nothing else. The runtime
 * runs {@link #validate} and {@link #onStart} with the module's class loader as
 * the thread context class loader.
 */
public interface OpenElisModule {

    /**
     * Called after the module's context has been refreshed and before anything of
     * the module is published to users. Throw a {@link ModuleValidationException}
     * to refuse to start, for example when a required configuration property is
     * missing; the message is logged and shown in the registry.
     */
    default void validate(ModuleContext context) throws ModuleValidationException {
    }

    /**
     * Called once per application start after the module's migrations have run, its
     * beans exist and its extensions, controllers, permissions and menu entries are
     * registered. Must return within the configured start timeout.
     */
    default void onStart(ModuleContext context) {
    }

    /**
     * Called after the module's migrations have run when the installed version
     * recorded by the core differs from the JAR's version. {@code fromVersion} is
     * the previously recorded version.
     */
    default void onUpgrade(ModuleContext context, String fromVersion, String toVersion) {
    }

    /**
     * Called when the application shuts down, before the module's context is
     * closed.
     */
    default void onStop(ModuleContext context) {
    }
}
