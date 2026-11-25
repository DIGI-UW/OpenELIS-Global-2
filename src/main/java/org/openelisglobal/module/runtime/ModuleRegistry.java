package org.openelisglobal.module.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.module.api.ModuleState;

/**
 * The modules of this installation. Discovery runs once when the registry is
 * created, early in the root context (the entity manager factory depends on the
 * module migrations, which depend on this bean), so every later step of
 * start-up reads the same records: the migrations, the entity registration, the
 * child contexts and the registry endpoint.
 */
public final class ModuleRegistry {

    private final ModuleSystemProperties properties;
    private final ContractVersions versions;
    private final List<ModuleRecord> records;
    private final ExtensionRegistry extensions = new ExtensionRegistry();

    public ModuleRegistry(ModuleSystemProperties properties, ContractVersions versions, List<ModuleRecord> records) {
        this.properties = properties;
        this.versions = versions;
        this.records = Collections.unmodifiableList(new ArrayList<>(records));
    }

    /** Discovers the modules in the configured directory. */
    public static ModuleRegistry discover(ModuleSystemProperties properties, ContractVersions versions,
            ClassLoader webappLoader) {
        List<ModuleRecord> records = new ModuleDiscovery(properties, versions, webappLoader).discover();
        return new ModuleRegistry(properties, versions, records);
    }

    public ModuleSystemProperties properties() {
        return properties;
    }

    public ContractVersions versions() {
        return versions;
    }

    public ExtensionRegistry extensions() {
        return extensions;
    }

    /** Every discovered JAR, in dependency order. */
    public List<ModuleRecord> all() {
        return records;
    }

    public Optional<ModuleRecord> find(String id) {
        return records.stream().filter(record -> record.id().equals(id)).findFirst();
    }

    public List<ModuleRecord> inState(ModuleState state) {
        List<ModuleRecord> matching = new ArrayList<>();
        for (ModuleRecord record : records) {
            if (record.state() == state) {
                matching.add(record);
            }
        }
        return matching;
    }

    /**
     * Modules that passed discovery and are not yet started, in the order they must
     * be started.
     */
    public List<ModuleRecord> startable() {
        List<ModuleRecord> matching = new ArrayList<>();
        for (ModuleRecord record : records) {
            if (record.state() == ModuleState.VALIDATED || record.state() == ModuleState.MIGRATED) {
                matching.add(record);
            }
        }
        return matching;
    }

    public List<ModuleRecord> started() {
        return inState(ModuleState.STARTED);
    }

    /** The class loaders of every module that will be started, for Hibernate. */
    public List<ClassLoader> classLoaders() {
        List<ClassLoader> loaders = new ArrayList<>();
        for (ModuleRecord record : startable()) {
            if (record.classLoader() != null) {
                loaders.add(record.classLoader());
            }
        }
        return loaders;
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }
}
