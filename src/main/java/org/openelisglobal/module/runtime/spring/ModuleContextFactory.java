package org.openelisglobal.module.runtime.spring;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.sql.DataSource;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.ModuleValidationException;
import org.openelisglobal.module.api.OpenElisModule;
import org.openelisglobal.module.api.manifest.BackendSection;
import org.openelisglobal.module.runtime.ExtensionPoints;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRegistry;
import org.openelisglobal.module.runtime.ModuleStartupException;
import org.openelisglobal.module.runtime.contributions.ModuleMenuContributor;
import org.openelisglobal.module.runtime.contributions.ModulePermissionRegistrar;
import org.openelisglobal.spi.ExtensionPoint;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;

/**
 * Starts every migrated module once the root context is refreshed: one
 * {@link ModuleApplicationContext} per module in dependency order (module
 * beans, the activator, the declared extensions), then the module's
 * contributions to the core (extensions into the extension registry,
 * controllers into the root handler mapping, permission rows, menu entries),
 * then {@code onStart} within the start timeout. Any failure after validation
 * stops the application with the module named (fail-fast), and the root context
 * itself stays intact.
 *
 * <p>
 * Runs first among the root's refresh listeners so that module permissions and
 * menu entries exist before the core's own start-up listeners and the first
 * login. Ignores refresh events of other contexts.
 */
public class ModuleContextFactory implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private static final String LOG = "ModuleContextFactory";

    private final ModuleRegistry registry;
    private final ModuleAwareRequestMappingHandlerMapping handlerMapping;
    private final ModulePermissionRegistrar permissionRegistrar;
    private final ModuleMenuContributor menuContributor;
    private final DataSource dataSource;
    private final Map<String, ModuleApplicationContext> contexts = new LinkedHashMap<>();
    private final Map<String, ModuleContextImpl> moduleContexts = new LinkedHashMap<>();
    private final Map<String, OpenElisModule> activators = new LinkedHashMap<>();
    private volatile boolean started;

    public ModuleContextFactory(ModuleRegistry registry, ModuleAwareRequestMappingHandlerMapping handlerMapping,
            ModulePermissionRegistrar permissionRegistrar, ModuleMenuContributor menuContributor,
            DataSource dataSource) {
        this.registry = registry;
        this.handlerMapping = handlerMapping;
        this.permissionRegistrar = permissionRegistrar;
        this.menuContributor = menuContributor;
        this.dataSource = dataSource;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        if (context.getParent() != null || started) {
            return;
        }
        started = true;
        startAll(context);
    }

    /** Starts every migrated module under the given root; public for the tests. */
    public void startAll(ApplicationContext root) {
        for (ModuleRecord module : registry.inState(ModuleState.MIGRATED)) {
            start(module, root);
        }
    }

    private void start(ModuleRecord module, ApplicationContext root) {
        long startedAt = System.currentTimeMillis();
        ModuleApplicationContext child = new ModuleApplicationContext(module.id());
        try {
            ModuleContextImpl moduleContext = new ModuleContextImpl(module, root, registry.properties());
            refresh(module, child, root, moduleContext);
            OpenElisModule activator = resolveActivator(module, child);
            validate(module, moduleContext, activator);
            registerExtensions(module, child);
            List<String> paths = registerControllers(module, child);
            permissionRegistrar.register(module);
            menuContributor.contribute(module);
            onStart(module, moduleContext, activator);
            contexts.put(module.id(), child);
            moduleContexts.put(module.id(), moduleContext);
            if (activator != null) {
                activators.put(module.id(), activator);
            }
            module.transition(ModuleState.STARTED);
            recordStarted(module);
            LogEvent.logInfo(LOG, "start", "[module:" + module.id() + "] STARTED " + module.version() + " in "
                    + (System.currentTimeMillis() - startedAt) + " ms; endpoints " + paths);
        } catch (ModuleStartupException e) {
            fail(module, child, e.getMessage(), e);
        } catch (RuntimeException e) {
            fail(module, child, e.toString(), e);
        }
    }

    private void fail(ModuleRecord module, ModuleApplicationContext child, String message, Throwable cause) {
        module.transition(ModuleState.FAILED, message);
        recordFailure(module, message);
        try {
            if (child.isActive()) {
                child.close();
            }
        } catch (RuntimeException e) {
            LogEvent.logWarn(LOG, "fail", "[module:" + module.id() + "] closing the failed context: " + e);
        }
        LogEvent.logError(LOG, "fail", "[module:" + module.id() + "] FAILED " + message);
        throw new ModuleStartupException("[module:" + module.id() + "] " + message + " (disable the module with a "
                + module.jarPath().getFileName() + ".disabled marker file" + " or modules." + module.id()
                + ".enabled=false and restart)", cause);
    }

    private void refresh(ModuleRecord module, ModuleApplicationContext child, ApplicationContext root,
            ModuleContextImpl moduleContext) {
        BackendSection backend = module.manifest().getBackend();
        child.setParent(root);
        child.setClassLoader(module.classLoader());
        if (root instanceof ConfigurableApplicationContext configurable) {
            child.setEnvironment(configurable.getEnvironment());
        }
        child.getBeanFactory().setBeanClassLoader(module.classLoader());
        child.getBeanFactory().registerSingleton("moduleContext", moduleContext);
        child.getBeanFactory().registerSingleton("moduleLog", moduleContext.log());
        child.getBeanFactory().registerSingleton("moduleConfiguration", moduleContext.configuration());
        child.register(ModuleChildConfig.class);
        if (backend != null) {
            if (!backend.getBasePackages().isEmpty()) {
                child.scan(backend.getBasePackages().toArray(new String[0]));
            }
            if (backend.getActivator() != null) {
                child.register(loadClass(module, backend.getActivator()));
            }
            for (BackendSection.ExtensionDeclaration extension : backend.getExtensions()) {
                child.register(loadClass(module, extension.getClassName()));
            }
        }
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(module.classLoader());
        try {
            child.refresh();
        } catch (RuntimeException e) {
            throw new ModuleStartupException("context refresh failed: " + rootMessage(e), e);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Class<?> loadClass(ModuleRecord module, String className) {
        try {
            return Class.forName(className, true, module.classLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            throw new ModuleStartupException("class " + className + " cannot be loaded: " + e);
        }
    }

    private static OpenElisModule resolveActivator(ModuleRecord module, ModuleApplicationContext child) {
        BackendSection backend = module.manifest().getBackend();
        if (backend == null || backend.getActivator() == null) {
            return null;
        }
        return (OpenElisModule) child.getBean(loadClass(module, backend.getActivator()));
    }

    private void validate(ModuleRecord module, ModuleContextImpl moduleContext, OpenElisModule activator) {
        if (activator == null) {
            return;
        }
        try {
            moduleContext.withModuleClassLoader(() -> {
                activator.validate(moduleContext);
                return null;
            });
        } catch (ModuleValidationException e) {
            throw new ModuleStartupException("validate refused to start: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ModuleStartupException("validate failed: " + e, e);
        }
    }

    private void registerExtensions(ModuleRecord module, ModuleApplicationContext child) {
        BackendSection backend = module.manifest().getBackend();
        if (backend == null) {
            return;
        }
        for (BackendSection.ExtensionDeclaration declaration : backend.getExtensions()) {
            Class<? extends ExtensionPoint> point = ExtensionPoints.interfaceFor(declaration.getSpi());
            Class<?> type = loadClass(module, declaration.getClassName());
            Object bean = child.getBean(type);
            registerExtension(module, point, bean, declaration.getOrder());
        }
    }

    private <T extends ExtensionPoint> void registerExtension(ModuleRecord module, Class<T> point, Object bean,
            int order) {
        registry.extensions().register(point, module.id(), point.cast(bean), order);
    }

    private List<String> registerControllers(ModuleRecord module, ModuleApplicationContext child) {
        List<String> paths = new ArrayList<>();
        Map<String, Object> controllers = child.getBeansWithAnnotation(Controller.class);
        if (controllers.isEmpty()) {
            return paths;
        }
        if (module.kind() == org.openelisglobal.module.api.ModuleKind.PROVIDER) {
            throw new ModuleStartupException("a provider may not declare controllers: " + controllers.keySet());
        }
        for (Object controller : controllers.values()) {
            paths.addAll(handlerMapping.registerModuleController(controller, module.manifest().getRestPrefix()));
        }
        return paths;
    }

    private void onStart(ModuleRecord module, ModuleContextImpl moduleContext, OpenElisModule activator) {
        if (activator == null) {
            return;
        }
        int timeoutSeconds = registry.properties().startTimeoutSeconds();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "module-start-" + module.id());
            thread.setDaemon(true);
            return thread;
        });
        try {
            Future<?> future = executor.submit(() -> moduleContext.withModuleClassLoader(() -> {
                if (module.isFirstInstall() || module.recordedVersion() == null) {
                    // first install: nothing to upgrade from
                } else if (!module.recordedVersion().equals(module.version())) {
                    activator.onUpgrade(moduleContext, module.recordedVersion(), module.version());
                }
                activator.onStart(moduleContext);
                return null;
            }));
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ModuleStartupException("onStart did not return within " + timeoutSeconds + " s");
        } catch (ExecutionException e) {
            throw new ModuleStartupException("onStart failed: " + rootMessage(e.getCause()), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModuleStartupException("onStart interrupted");
        } finally {
            executor.shutdownNow();
        }
    }

    /** Stops every started module when the root context closes. */
    public void stopAll() {
        List<String> ids = new ArrayList<>(contexts.keySet());
        java.util.Collections.reverse(ids);
        for (String id : ids) {
            ModuleApplicationContext child = contexts.remove(id);
            OpenElisModule activator = activators.remove(id);
            ModuleContextImpl moduleContext = moduleContexts.remove(id);
            try {
                if (activator != null) {
                    activator.onStop(moduleContext);
                }
            } catch (RuntimeException e) {
                LogEvent.logWarn(LOG, "stopAll", "[module:" + id + "] onStop failed: " + e);
            }
            try {
                child.close();
            } catch (RuntimeException e) {
                LogEvent.logWarn(LOG, "stopAll", "[module:" + id + "] closing the context failed: " + e);
            }
            registry.find(id).ifPresent(record -> record.transition(ModuleState.STOPPED));
        }
    }

    /** The started module contexts, in start order. */
    public Map<String, ModuleApplicationContext> contexts() {
        return java.util.Collections.unmodifiableMap(contexts);
    }

    public ModuleContextImpl moduleContext(String moduleId) {
        return moduleContexts.get(moduleId);
    }

    private void recordStarted(ModuleRecord module) {
        try {
            new JdbcTemplate(dataSource).update(
                    "update clinlims.module_state set status = ?, last_started_at = ?, last_error = null where id = ?",
                    ModuleState.STARTED.name(), Timestamp.from(Instant.now()), module.id());
        } catch (RuntimeException e) {
            LogEvent.logWarn(LOG, "recordStarted", "[module:" + module.id() + "] module_state not updated: " + e);
        }
    }

    private void recordFailure(ModuleRecord module, String message) {
        try {
            String truncated = message.length() > 1000 ? message.substring(0, 1000) : message;
            new JdbcTemplate(dataSource).update(
                    "update clinlims.module_state set status = ?, last_error = ? where id = ?",
                    ModuleState.FAILED.name(), truncated, module.id());
        } catch (RuntimeException e) {
            LogEvent.logWarn(LOG, "recordFailure", "[module:" + module.id() + "] module_state not updated: " + e);
        }
    }

    static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? cause.toString()
                : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    /** Closes module contexts when the root context closes. */
    public static final class Shutdown implements ApplicationListener<ContextClosedEvent> {

        private final ModuleContextFactory factory;

        public Shutdown(ModuleContextFactory factory) {
            this.factory = factory;
        }

        @Override
        public void onApplicationEvent(ContextClosedEvent event) {
            if (event.getApplicationContext().getParent() == null) {
                factory.stopAll();
            }
        }
    }
}
