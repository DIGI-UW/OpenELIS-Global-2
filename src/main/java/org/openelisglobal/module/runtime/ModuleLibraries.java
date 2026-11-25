package org.openelisglobal.module.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The libraries a module bundles under {@code lib/}: extracted once per JAR
 * digest into the module cache so the module's class loader can open them, and
 * compared with the libraries the core ships so that a library the core also
 * provides is reported as shadowed (the core's copy wins by parent-first
 * delegation).
 */
public final class ModuleLibraries {

    private static final Pattern VERSIONED_JAR = Pattern.compile("^(.*?)-(\\d[^-]*(?:-[^-]*)?)\\.jar$");

    private ModuleLibraries() {
    }

    /** The {@code lib/*.jar} entries of a module JAR, by entry name. */
    public static List<String> bundledEntries(Path jarPath) throws IOException {
        List<String> entries = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> all = jar.entries();
            while (all.hasMoreElements()) {
                JarEntry entry = all.nextElement();
                String name = entry.getName();
                if (!entry.isDirectory() && name.startsWith("lib/") && name.endsWith(".jar")
                        && name.indexOf('/', 4) < 0) {
                    entries.add(name);
                }
            }
        }
        return entries;
    }

    /**
     * Extracts the bundled libraries into {@code <cache>/<id>/<sha256>/} (skipped
     * when that directory already holds them) and returns the extracted files in
     * entry order.
     */
    public static List<Path> extract(Path jarPath, String moduleId, String sha256, Path cacheDirectory)
            throws IOException {
        List<String> entries = bundledEntries(jarPath);
        List<Path> files = new ArrayList<>();
        if (entries.isEmpty()) {
            return files;
        }
        Path target = cacheDirectory.resolve(moduleId).resolve(sha256);
        Files.createDirectories(target);
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (String entry : entries) {
                Path file = target.resolve(entry.substring("lib/".length()));
                if (!Files.isRegularFile(file)) {
                    Path partial = file.resolveSibling(file.getFileName() + ".part");
                    try (InputStream in = jar.getInputStream(jar.getJarEntry(entry))) {
                        Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(partial, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                }
                files.add(file);
            }
        }
        return files;
    }

    /**
     * The artifact name of a JAR file name without its version:
     * {@code commons-csv-1.12.0.jar} is {@code commons-csv}.
     */
    public static String artifactName(String fileName) {
        String base = fileName.substring(fileName.lastIndexOf('/') + 1);
        Matcher matcher = VERSIONED_JAR.matcher(base);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return base.endsWith(".jar") ? base.substring(0, base.length() - 4) : base;
    }

    /**
     * The libraries the core ships, by artifact name, read from the web application
     * loader's URLs (Tomcat's loader is a {@code URLClassLoader}); empty when the
     * loader does not expose them.
     */
    public static Map<String, String> coreLibraries(ClassLoader webappLoader) {
        Map<String, String> libraries = new LinkedHashMap<>();
        if (!(webappLoader instanceof URLClassLoader)) {
            return libraries;
        }
        for (URL url : ((URLClassLoader) webappLoader).getURLs()) {
            String path = url.getPath();
            if (path.endsWith(".jar")) {
                String file = path.substring(path.lastIndexOf('/') + 1);
                libraries.putIfAbsent(artifactName(file), file);
            }
        }
        return libraries;
    }
}
