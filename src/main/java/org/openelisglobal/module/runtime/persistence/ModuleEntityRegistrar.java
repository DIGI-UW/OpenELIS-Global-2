package org.openelisglobal.module.runtime.persistence;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;

/**
 * Adds every migrated module's declared entity classes to the core persistence
 * unit before the entity manager factory is built, so module entities share the
 * core's session factory, transactions and {@code clinlims} schema. Hibernate
 * loads the classes through the module class loaders that
 * {@code HibernateConfig} passes as {@code hibernate.classLoaders}; core user
 * types, converters and proxies keep resolving to the core's classes through
 * parent-first delegation.
 *
 * <p>
 * Entities are added at bootstrap only; the session factory's model is fixed
 * once built, which is why modules are installed by restart.
 */
public class ModuleEntityRegistrar implements PersistenceUnitPostProcessor {

    private static final String LOG = "ModuleEntityRegistrar";

    private final ModuleRegistry registry;

    public ModuleEntityRegistrar(ModuleRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo persistenceUnit) {
        for (String entity : entityClassNames()) {
            persistenceUnit.addManagedClassName(entity);
        }
    }

    /**
     * The entity class names of every module that reached {@code MIGRATED}, in
     * module order.
     */
    public List<String> entityClassNames() {
        List<String> names = new ArrayList<>();
        for (ModuleRecord module : registry.inState(ModuleState.MIGRATED)) {
            if (module.manifest().getBackend() == null) {
                continue;
            }
            for (String entity : module.manifest().getBackend().getEntities()) {
                names.add(entity);
                LogEvent.logInfo(LOG, "entityClassNames", "[module:" + module.id() + "] entity " + entity);
            }
        }
        return names;
    }

    /** The class loaders Hibernate needs to load those entities. */
    public List<ClassLoader> classLoaders() {
        List<ClassLoader> loaders = new ArrayList<>();
        for (ModuleRecord module : registry.inState(ModuleState.MIGRATED)) {
            if (module.classLoader() != null) {
                loaders.add(module.classLoader());
            }
        }
        return loaders;
    }
}
