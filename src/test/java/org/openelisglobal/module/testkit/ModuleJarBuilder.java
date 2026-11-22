package org.openelisglobal.module.testkit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * Builds a module JAR for a test: Java sources compiled at test time with the
 * test class path (so the classes exist only inside the JAR and are provably
 * loaded by the module class loader, never by the test's loader), the manifest,
 * resources such as a changelog or frontend assets, and optional bundled
 * libraries under {@code lib/}.
 */
public final class ModuleJarBuilder {

    private final Map<String, String> sources = new LinkedHashMap<>();
    private final Map<String, byte[]> resources = new LinkedHashMap<>();
    private String manifest;

    public static ModuleJarBuilder module() {
        return new ModuleJarBuilder();
    }

    public ModuleJarBuilder manifest(String json) {
        this.manifest = json;
        return this;
    }

    /**
     * A Java source; the file name is derived from the package and class name in
     * the source.
     */
    public ModuleJarBuilder source(String className, String code) {
        sources.put(className, code);
        return this;
    }

    public ModuleJarBuilder resource(String path, String text) {
        resources.put(path, text.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public ModuleJarBuilder resource(String path, byte[] bytes) {
        resources.put(path, bytes);
        return this;
    }

    /** Bundles a library JAR under {@code lib/}. */
    public ModuleJarBuilder library(String fileName, Path jar) {
        try {
            resources.put("lib/" + fileName, Files.readAllBytes(jar));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    /** Writes the JAR to {@code target}. */
    public Path build(Path target) {
        try {
            Files.createDirectories(target.getParent());
            Map<String, byte[]> classes = sources.isEmpty() ? Map.of() : compile();
            Map<String, byte[]> entries = new LinkedHashMap<>();
            if (manifest != null) {
                entries.put("META-INF/openelis-module.json", manifest.getBytes(StandardCharsets.UTF_8));
            }
            entries.putAll(classes);
            entries.putAll(resources);
            try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(target))) {
                java.util.Set<String> directories = new java.util.LinkedHashSet<>();
                for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                    writeDirectories(jar, entry.getKey(), directories);
                    write(jar, entry.getKey(), entry.getValue());
                }
            }
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Builds a plain library JAR (no manifest) from sources, for {@code lib/}
     * fixtures.
     */
    public static Path library(Path target, Map<String, String> sources) {
        ModuleJarBuilder builder = new ModuleJarBuilder();
        sources.forEach(builder::source);
        return builder.build(target);
    }

    private Map<String, byte[]> compile() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no Java compiler available; run the tests on a JDK");
        }
        Path output = Files.createTempDirectory("module-fixture-classes");
        List<JavaFileObject> units = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            units.add(new SourceObject(source.getKey(), source.getValue()));
        }
        List<String> options = List.of("-d", output.toString(), "-classpath", System.getProperty("java.class.path"),
                "-proc:none", "-parameters");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            StringBuilderWriter diagnostics = new StringBuilderWriter();
            boolean ok = compiler.getTask(diagnostics, fileManager, null, options, null, units).call();
            if (!ok) {
                throw new IllegalStateException("fixture module does not compile:\n" + diagnostics);
            }
        }
        Map<String, byte[]> classes = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(output)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                classes.put(output.relativize(file).toString().replace('\\', '/'), Files.readAllBytes(file));
            }
        }
        return classes;
    }

    private static void write(JarOutputStream jar, String name, byte[] bytes) throws IOException {
        jar.putNextEntry(new JarEntry(name));
        jar.write(bytes);
        jar.closeEntry();
    }

    /**
     * Directory entries, as Maven and the JDK's {@code jar} tool write them:
     * Spring's class path scanning finds a package inside a JAR through its
     * directory entry.
     */
    private static void writeDirectories(JarOutputStream jar, String name, java.util.Set<String> written)
            throws IOException {
        int slash = name.indexOf('/');
        while (slash > 0) {
            String directory = name.substring(0, slash + 1);
            if (written.add(directory)) {
                jar.putNextEntry(new JarEntry(directory));
                jar.closeEntry();
            }
            slash = name.indexOf('/', slash + 1);
        }
    }

    static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo((OutputStream) out);
        return out.toByteArray();
    }

    private static final class SourceObject extends SimpleJavaFileObject {
        private final String code;

        SourceObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static final class StringBuilderWriter extends java.io.Writer {
        private final StringBuilder builder = new StringBuilder();

        @Override
        public void write(char[] cbuf, int off, int len) {
            builder.append(cbuf, off, len);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
