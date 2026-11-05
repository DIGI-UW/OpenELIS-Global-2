package org.openelisglobal.module.runtime.persistence;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.jar.JarFile;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

/**
 * Reads a module's changelog files from the module JAR alone. A module names
 * one changelog file, {@code liquibase/base.xml} as a rule, which includes the
 * module's changelog files in order the way the core's own base changelogs do;
 * the includes resolve inside the same JAR, never against a core changelog that
 * happens to share the path.
 */
public final class ModuleChangelogs {

    private ModuleChangelogs() {
    }

    /** A resource accessor over the module JAR only (no parent class loader). */
    public static ResourceAccessor accessor(Path jar) throws MalformedURLException {
        return new ClassLoaderResourceAccessor(new URLClassLoader(new URL[] { jar.toUri().toURL() }, null));
    }

    /** Whether the JAR holds a file at {@code path}. */
    public static boolean exists(Path jar, String path) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getJarEntry(path) != null && !jarFile.getJarEntry(path).isDirectory();
        }
    }
}
