package org.openelisglobal.module.api;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * What the core gives a module at runtime. One instance exists per started
 * module as a bean of the module's own Spring context, so module beans may
 * inject it; the lifecycle hooks of {@link OpenElisModule} receive the same
 * instance.
 */
public interface ModuleContext {

    /**
     * The parsed manifest, the module's current state and where it was loaded from.
     */
    ModuleDescriptor descriptor();

    /**
     * The module's configuration: the {@code modules.<id>.<key>} properties of the
     * application's property sources, typed and defaulted by the manifest's
     * {@code configuration.properties}.
     */
    ModuleConfiguration configuration();

    /**
     * A core service by type: any bean of the core application context (a provider
     * should prefer the facades published in this artifact, a full module may ask
     * for any core service it compiles against).
     *
     * @throws IllegalArgumentException when no core bean of that type exists
     */
    <T> T coreService(Class<T> type);

    /**
     * A JDBC template on the core datasource that takes part in the surrounding
     * core transaction. The supported way for a provider, which owns no entities,
     * to read data the core services do not expose.
     */
    NamedParameterJdbcTemplate jdbc();

    /**
     * The module's private data directory
     * ({@code /var/lib/openelis-global/modules-data/<id>}), created on first use.
     */
    Path dataDirectory();

    /**
     * Runs a start-up write as the system (daemon) user, the way the core's own
     * seeders write, so that audit columns are filled when no user is signed in.
     */
    void runAsSystem(Runnable startupWrite);

    /**
     * Runs a call with the module's class loader as the thread context class
     * loader, for libraries that discover implementations through
     * {@code ServiceLoader} or {@code Class.forName}. Restores the previous loader
     * afterwards.
     */
    <T> T withModuleClassLoader(Callable<T> call) throws Exception;

    /** A logger that prefixes every line with the module id. */
    ModuleLog log();
}
