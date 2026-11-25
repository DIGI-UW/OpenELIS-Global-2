package org.openelisglobal.module.runtime;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * One class loader per module, parent-first. The parent is the web
 * application's loader, so every class the core ships (Spring, Hibernate,
 * Jackson, the core's own classes) resolves to the core's copy; only classes
 * the core does not have come from the module JAR and its extracted
 * {@code lib/*.jar}. A module that names other modules in
 * {@code requires.modules} sees their classes too: those loaders are searched
 * after the parent and before the module's own JARs.
 *
 * <p>
 * Loaders live for the lifetime of the web application; nothing is unloaded.
 */
public final class ModuleClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final String moduleId;
    private final List<ModuleClassLoader> dependencies;

    public ModuleClassLoader(String moduleId, URL[] urls, ClassLoader parent, List<ModuleClassLoader> dependencies) {
        super("module:" + moduleId, urls, parent);
        this.moduleId = moduleId;
        this.dependencies = List.copyOf(dependencies);
    }

    public String getModuleId() {
        return moduleId;
    }

    /**
     * The loaders of the modules this one depends on, searched before its own JARs.
     */
    public List<ModuleClassLoader> getDependencies() {
        return dependencies;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ModuleClassLoader dependency : dependencies) {
            try {
                return dependency.loadClass(name);
            } catch (ClassNotFoundException e) {
                // not in that module either
            }
        }
        return super.findClass(name);
    }

    @Override
    public URL findResource(String name) {
        for (ModuleClassLoader dependency : dependencies) {
            URL url = dependency.findResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> all = new ArrayList<>();
        for (ModuleClassLoader dependency : dependencies) {
            all.addAll(Collections.list(dependency.findResources(name)));
        }
        all.addAll(Collections.list(super.findResources(name)));
        return Collections.enumeration(all);
    }

    /**
     * Whether the class was defined by this loader (as opposed to the core or a
     * dependency).
     */
    public boolean defines(Class<?> type) {
        return type.getClassLoader() == this;
    }

    @Override
    public String toString() {
        return getName();
    }
}
