package org.openelisglobal.module.runtime.spring;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import javax.sql.DataSource;
import org.openelisglobal.module.api.ModuleConfiguration;
import org.openelisglobal.module.api.ModuleContext;
import org.openelisglobal.module.api.ModuleDescriptor;
import org.openelisglobal.module.api.ModuleLog;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleSystemProperties;
import org.openelisglobal.security.DaemonContextExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** The {@link ModuleContext} the runtime gives one module. */
public final class ModuleContextImpl implements ModuleContext {

    private final ModuleRecord record;
    private final ApplicationContext root;
    private final ModuleSystemProperties properties;
    private final ModuleConfiguration configuration;
    private final ModuleLog log;
    private volatile NamedParameterJdbcTemplate jdbc;

    public ModuleContextImpl(ModuleRecord record, ApplicationContext root, ModuleSystemProperties properties) {
        this.record = record;
        this.root = root;
        this.properties = properties;
        this.configuration = new ModuleConfigurationImpl(record.manifest(), properties);
        this.log = new ModuleLogImpl(record.id());
    }

    @Override
    public ModuleDescriptor descriptor() {
        return record;
    }

    @Override
    public ModuleConfiguration configuration() {
        return configuration;
    }

    @Override
    public <T> T coreService(Class<T> type) {
        try {
            return root.getBean(type);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("no core service of type " + type.getName(), e);
        }
    }

    @Override
    public NamedParameterJdbcTemplate jdbc() {
        NamedParameterJdbcTemplate template = jdbc;
        if (template == null) {
            template = new NamedParameterJdbcTemplate(root.getBean(DataSource.class));
            jdbc = template;
        }
        return template;
    }

    @Override
    public Path dataDirectory() {
        Path directory = properties.dataDirectory().resolve(record.id());
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create the module data directory " + directory, e);
        }
        return directory;
    }

    @Override
    public void runAsSystem(Runnable startupWrite) {
        root.getBean(DaemonContextExecutor.class).executeAsDaemon(startupWrite);
    }

    @Override
    public <T> T withModuleClassLoader(Callable<T> call) throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(record.classLoader());
        try {
            return call.call();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public ModuleLog log() {
        return log;
    }
}
