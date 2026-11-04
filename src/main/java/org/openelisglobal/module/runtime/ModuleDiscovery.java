package org.openelisglobal.module.runtime;

import jakarta.persistence.Entity;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.module.api.ModuleKind;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.OpenElisModule;
import org.openelisglobal.module.api.manifest.BackendSection;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.api.manifest.Requires;
import org.openelisglobal.module.manifest.ManifestReader;
import org.openelisglobal.module.manifest.ManifestValidation;
import org.openelisglobal.module.manifest.ManifestValidator;
import org.openelisglobal.module.runtime.persistence.ModuleChangelogs;
import org.openelisglobal.module.version.VersionRanges;

/**
 * Finds the module JARs in the module directory and takes each one as far as
 * {@link ModuleState#VALIDATED} without touching the database or creating a
 * bean: manifest, digest allowlist, switches, compatibility with the running
 * contracts, dependencies and conflicts, then a class loader and the
 * loadability of every declared class.
 *
 * <p>
 * A module that fails a check is recorded as {@code INVALID},
 * {@code INCOMPATIBLE} or {@code DISABLED} with its reason and skipped; the
 * core starts without it. Two JARs declaring the same id abort start-up,
 * because the registry never guesses which one was meant.
 */
public final class ModuleDiscovery {

    private static final String LOG = "ModuleDiscovery";

    private final ModuleSystemProperties properties;
    private final ContractVersions versions;
    private final ClassLoader webappLoader;
    private final ManifestValidator validator;

    public ModuleDiscovery(ModuleSystemProperties properties, ContractVersions versions, ClassLoader webappLoader) {
        this(properties, versions, webappLoader, new ManifestValidator());
    }

    public ModuleDiscovery(ModuleSystemProperties properties, ContractVersions versions, ClassLoader webappLoader,
            ManifestValidator validator) {
        this.properties = properties;
        this.versions = versions;
        this.webappLoader = webappLoader;
        this.validator = validator;
    }

    /** Every JAR found, in dependency order, each in its discovered state. */
    public List<ModuleRecord> discover() {
        Path directory = properties.directory();
        if (!Files.isDirectory(directory)) {
            LogEvent.logInfo(LOG, "discover", "no module directory at " + directory + "; no modules installed");
            return List.of();
        }
        List<ModuleRecord> records = readJars(directory);
        refuseDuplicateIds(records);
        Map<String, String> lock = readLock();
        for (ModuleRecord record : records) {
            if (record.state() != ModuleState.DISCOVERED) {
                continue;
            }
            checkLock(record, lock);
        }
        for (ModuleRecord record : records) {
            if (record.state() == ModuleState.DISCOVERED && !properties.isEnabled(record.id())) {
                record.transition(ModuleState.DISABLED,
                        "switched off by modules.disabled or modules." + record.id() + ".enabled=false");
            }
        }
        for (ModuleRecord record : records) {
            if (record.state() == ModuleState.DISCOVERED) {
                checkCompatibility(record);
            }
        }
        refuseSharedBasePackages(records);
        List<ModuleRecord> ordered = resolveDependencies(records);
        Map<String, String> coreLibraries = ModuleLibraries.coreLibraries(webappLoader);
        Map<String, ModuleClassLoader> loaders = new HashMap<>();
        for (ModuleRecord record : ordered) {
            if (record.state() != ModuleState.DISCOVERED) {
                continue;
            }
            try {
                ModuleClassLoader loader = createLoader(record, loaders, coreLibraries);
                checkClasses(record, loader);
                if (record.state() == ModuleState.DISCOVERED) {
                    loaders.put(record.id(), loader);
                    record.setClassLoader(loader);
                    record.transition(ModuleState.VALIDATED);
                }
            } catch (IOException e) {
                record.transition(ModuleState.INVALID, "cannot read the JAR: " + e.getMessage());
            }
        }
        for (ModuleRecord record : ordered) {
            LogEvent.logInfo(LOG, "discover", "[module:" + record.id() + "] " + record.state()
                    + (record.reason().isPresent() ? " " + record.reason().get() : "") + " (" + record.jarPath() + ")");
        }
        return ordered;
    }

    private List<ModuleRecord> readJars(Path directory) {
        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.jar")) {
            for (Path jar : stream) {
                if (Files.isRegularFile(jar)) {
                    jars.add(jar);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list the module directory " + directory, e);
        }
        jars.sort(null);
        List<ModuleRecord> records = new ArrayList<>();
        for (Path jar : jars) {
            records.add(readJar(jar));
        }
        return records;
    }

    private ModuleRecord readJar(Path jar) {
        String fallbackId = jar.getFileName().toString().replaceAll("\\.jar$", "");
        String sha256;
        String manifestText;
        try {
            sha256 = sha256(jar);
            manifestText = readEntry(jar, ManifestReader.MANIFEST_ENTRY);
        } catch (IOException e) {
            ModuleRecord record = new ModuleRecord(jar, "", null, fallbackId);
            record.transition(ModuleState.INVALID, "cannot read the JAR: " + e.getMessage());
            return record;
        }
        if (manifestText == null) {
            ModuleRecord record = new ModuleRecord(jar, sha256, null, fallbackId);
            record.transition(ModuleState.INVALID, "no " + ManifestReader.MANIFEST_ENTRY + " in the JAR");
            return record;
        }
        ManifestValidation validation = validator.validate(manifestText);
        ModuleRecord record = new ModuleRecord(jar, sha256, validation.manifest(),
                declaredId(manifestText).orElse(fallbackId));
        if (!validation.isValid()) {
            record.transition(ModuleState.INVALID, "manifest: " + validation.describe());
        } else if (Files.exists(jar.resolveSibling(jar.getFileName() + ".disabled"))) {
            record.transition(ModuleState.DISABLED, "marker file " + jar.getFileName() + ".disabled present");
        }
        return record;
    }

    /**
     * The {@code id} of a manifest that failed validation, so the registry can
     * still name the module.
     */
    private static java.util.Optional<String> declaredId(String manifestText) {
        try {
            String id = new ManifestReader().readTree(manifestText).path("id").asText(null);
            return id == null || !id.matches(ModuleManifest.ID_PATTERN) ? java.util.Optional.empty()
                    : java.util.Optional.of(id);
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
    }

    private static void refuseDuplicateIds(List<ModuleRecord> records) {
        Map<String, ModuleRecord> byId = new HashMap<>();
        for (ModuleRecord record : records) {
            if (record.manifest() == null) {
                continue;
            }
            ModuleRecord other = byId.putIfAbsent(record.id(), record);
            if (other != null) {
                throw new ModuleStartupException("module id '" + record.id() + "' is declared by two JARs: "
                        + other.jarPath() + " and " + record.jarPath() + "; remove one and restart");
            }
        }
    }

    /**
     * {@code id@version sha256} lines; empty when strict mode is off (no lock
     * file).
     */
    private Map<String, String> readLock() {
        Path lockFile = properties.lockFile();
        Map<String, String> lock = new LinkedHashMap<>();
        if (!Files.isRegularFile(lockFile)) {
            return lock;
        }
        try {
            for (String line : Files.readAllLines(lockFile, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                if (parts.length < 2) {
                    throw new ModuleStartupException(
                            "modules.lock line is not '<id>@<version> <sha256>': '" + line + "'");
                }
                lock.put(parts[0], parts[1].toLowerCase());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + lockFile, e);
        }
        lock.put("", "strict");
        return lock;
    }

    private static void checkLock(ModuleRecord record, Map<String, String> lock) {
        if (lock.isEmpty()) {
            return;
        }
        String key = record.id() + "@" + record.version();
        String expected = lock.get(key);
        if (expected == null) {
            record.transition(ModuleState.INVALID, "strict mode: " + key + " is not listed in modules.lock");
        } else if (!expected.equals(record.sha256())) {
            record.transition(ModuleState.INVALID,
                    "strict mode: digest " + record.sha256() + " differs from modules.lock (" + expected + ")");
        }
    }

    private void checkCompatibility(ModuleRecord record) {
        Requires requires = record.manifest().getRequires();
        if (!VersionRanges.satisfies(versions.openelis(), requires.getOpenelis())) {
            record.transition(ModuleState.INCOMPATIBLE,
                    "requires OpenELIS " + requires.getOpenelis() + " but this is " + versions.openelis());
            return;
        }
        if (requires.getCoreApi() != null && !VersionRanges.satisfies(versions.coreApi(), requires.getCoreApi())) {
            record.transition(ModuleState.INCOMPATIBLE,
                    "requires openelis-module-api " + requires.getCoreApi() + " but this is " + versions.coreApi());
            return;
        }
        if (requires.getSpi() != null && !VersionRanges.satisfies(versions.spi(), requires.getSpi())) {
            record.transition(ModuleState.INCOMPATIBLE,
                    "requires openelis-spi " + requires.getSpi() + " but this is " + versions.spi());
        }
    }

    /**
     * Resolves {@code requires.modules} and {@code conflicts} and returns every
     * record in an order where each module comes after the modules it depends on. A
     * missing or unusable dependency, a conflict present or a cycle makes the
     * dependent {@code INCOMPATIBLE}.
     */
    static List<ModuleRecord> resolveDependencies(List<ModuleRecord> records) {
        Map<String, ModuleRecord> byId = new LinkedHashMap<>();
        for (ModuleRecord record : records) {
            if (record.manifest() != null) {
                byId.put(record.id(), record);
            }
        }
        for (ModuleRecord record : records) {
            if (record.state() != ModuleState.DISCOVERED) {
                continue;
            }
            for (String conflict : record.manifest().getConflicts()) {
                ModuleRecord other = byId.get(conflict);
                if (other != null && other.state() == ModuleState.DISCOVERED) {
                    record.transition(ModuleState.INCOMPATIBLE, "conflicts with installed module '" + conflict + "'");
                    break;
                }
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ModuleRecord record : records) {
                if (record.state() != ModuleState.DISCOVERED) {
                    continue;
                }
                for (Requires.ModuleDependency dependency : record.manifest().getRequires().getModules()) {
                    ModuleRecord other = byId.get(dependency.getId());
                    String problem = null;
                    if (other == null) {
                        problem = "requires module '" + dependency.getId() + "', which is not installed";
                    } else if (other.state() != ModuleState.DISCOVERED) {
                        problem = "requires module '" + dependency.getId() + "', which is " + other.state();
                    } else if (!VersionRanges.satisfies(other.version(), dependency.getVersion())) {
                        problem = "requires module '" + dependency.getId() + "' " + dependency.getVersion() + " but "
                                + other.version() + " is installed";
                    }
                    if (problem != null) {
                        record.transition(ModuleState.INCOMPATIBLE, problem);
                        changed = true;
                        break;
                    }
                }
            }
        }
        List<ModuleRecord> ordered = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        for (ModuleRecord record : records) {
            if (record.state() != ModuleState.DISCOVERED) {
                ordered.add(record);
                placed.add(record.id());
            }
        }
        List<ModuleRecord> pending = new ArrayList<>();
        for (ModuleRecord record : records) {
            if (record.state() == ModuleState.DISCOVERED) {
                pending.add(record);
            }
        }
        while (!pending.isEmpty()) {
            boolean progressed = false;
            for (ModuleRecord record : new ArrayList<>(pending)) {
                boolean ready = true;
                for (Requires.ModuleDependency dependency : record.manifest().getRequires().getModules()) {
                    ModuleRecord other = byId.get(dependency.getId());
                    if (other.state() == ModuleState.DISCOVERED && !placed.contains(other.id())) {
                        ready = false;
                        break;
                    }
                }
                if (ready) {
                    ordered.add(record);
                    placed.add(record.id());
                    pending.remove(record);
                    progressed = true;
                }
            }
            if (!progressed) {
                Set<String> cycle = new LinkedHashSet<>();
                for (ModuleRecord record : pending) {
                    cycle.add(record.id());
                }
                for (ModuleRecord record : pending) {
                    record.transition(ModuleState.INCOMPATIBLE, "dependency cycle between " + cycle);
                    ordered.add(record);
                }
                pending.clear();
            }
        }
        return ordered;
    }

    private ModuleClassLoader createLoader(ModuleRecord record, Map<String, ModuleClassLoader> loaders,
            Map<String, String> coreLibraries) throws IOException {
        List<Path> libraries = ModuleLibraries.extract(record.jarPath(), record.id(), record.sha256(),
                properties.cacheDirectory());
        List<URL> urls = new ArrayList<>();
        urls.add(toUrl(record.jarPath()));
        for (Path library : libraries) {
            urls.add(toUrl(library));
            String file = library.getFileName().toString();
            record.addBundledLibrary(file);
            String coreCopy = coreLibraries.get(ModuleLibraries.artifactName(file));
            if (coreCopy != null) {
                record.addShadowedLibrary(file, coreCopy);
                LogEvent.logWarn(LOG, "createLoader", "[module:" + record.id() + "] SHADOWED_LIBRARY " + file
                        + " is also shipped by the core as " + coreCopy + "; the core's copy is in use");
            }
        }
        List<ModuleClassLoader> dependencies = new ArrayList<>();
        for (Requires.ModuleDependency dependency : record.manifest().getRequires().getModules()) {
            ModuleClassLoader loader = loaders.get(dependency.getId());
            if (loader != null) {
                dependencies.add(loader);
            }
        }
        return new ModuleClassLoader(record.id(), urls.toArray(new URL[0]), webappLoader, dependencies);
    }

    /**
     * Two modules may not claim the same base package, nor one another's
     * sub-package: both would scan the other's beans. Both are refused, with the
     * other's name in the reason.
     */
    static void refuseSharedBasePackages(List<ModuleRecord> records) {
        Map<String, ModuleRecord> owners = new java.util.LinkedHashMap<>();
        for (ModuleRecord record : records) {
            if (record.state() != ModuleState.DISCOVERED || record.manifest().getBackend() == null) {
                continue;
            }
            for (String pkg : record.manifest().getBackend().getBasePackages()) {
                for (Map.Entry<String, ModuleRecord> owner : owners.entrySet()) {
                    ModuleRecord other = owner.getValue();
                    if (other != record && overlaps(owner.getKey(), pkg)) {
                        record.transition(ModuleState.INCOMPATIBLE, "base package " + pkg
                                + " is also claimed by module " + other.id() + " (" + owner.getKey() + ")");
                        other.transition(ModuleState.INCOMPATIBLE, "base package " + owner.getKey()
                                + " is also claimed by module " + record.id() + " (" + pkg + ")");
                    }
                }
                owners.putIfAbsent(pkg, record);
            }
        }
    }

    private static boolean overlaps(String first, String second) {
        return first.equals(second) || first.startsWith(second + ".") || second.startsWith(first + ".");
    }

    private void checkClasses(ModuleRecord record, ModuleClassLoader loader) {
        ModuleManifest manifest = record.manifest();
        BackendSection backend = manifest.getBackend();
        List<String> problems = new ArrayList<>();
        if (backend != null) {
            if (backend.getActivator() != null) {
                Class<?> activator = loadModuleClass(loader, backend.getActivator(), problems);
                if (activator != null && !OpenElisModule.class.isAssignableFrom(activator)) {
                    problems.add("activator " + backend.getActivator() + " does not implement OpenElisModule");
                }
            }
            for (String entity : backend.getEntities()) {
                Class<?> type = loadModuleClass(loader, entity, problems);
                if (type != null && !type.isAnnotationPresent(Entity.class)) {
                    problems.add("entity " + entity + " is not annotated with @Entity");
                }
            }
            for (BackendSection.ExtensionDeclaration extension : backend.getExtensions()) {
                Class<?> point = ExtensionPoints.interfaceFor(extension.getSpi());
                Class<?> type = loadModuleClass(loader, extension.getClassName(), problems);
                if (type != null && point != null && !point.isAssignableFrom(type)) {
                    problems.add("extension " + extension.getClassName() + " does not implement " + point.getName()
                            + " (" + extension.getSpi() + ")");
                }
            }
            if (backend.getLiquibase() != null) {
                try {
                    if (!ModuleChangelogs.exists(record.jarPath(), backend.getLiquibase())) {
                        problems.add("changelog " + backend.getLiquibase() + " is not in the JAR");
                    }
                } catch (IOException e) {
                    problems.add("changelog " + backend.getLiquibase() + " cannot be read: " + e.getMessage());
                }
            }
        }
        if (manifest.getFrontend() != null) {
            String assetRoot = "META-INF/resources/modules/" + manifest.getId() + "/";
            if (loader.findResource(assetRoot + manifest.getFrontend().getEntry()) == null) {
                problems.add("frontend entry " + assetRoot + manifest.getFrontend().getEntry()
                        + " is not in the JAR; build the frontend into the module before packaging");
            }
            String english = assetRoot + manifest.getFrontend().getMessages().replace("{locale}", "en");
            if (loader.findResource(english) == null) {
                record.addWarning("no English message file at " + english);
            }
        }
        if (!problems.isEmpty()) {
            record.transition(ModuleState.INVALID, String.join("; ", problems));
        } else if (record.kind() == ModuleKind.PROVIDER && backend == null) {
            record.transition(ModuleState.INVALID, "a provider declares a backend section with its extensions");
        }
    }

    private static Class<?> loadModuleClass(ModuleClassLoader loader, String className, List<String> problems) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            if (type.getClassLoader() == loader.getParent()) {
                problems.add(className + " is a core class, not a class of the module");
                return null;
            }
            return type;
        } catch (ClassNotFoundException | LinkageError e) {
            problems.add("class " + className + " cannot be loaded from the module: " + e);
            return null;
        }
    }

    static String readEntry(Path jar, String entryName) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                return null;
            }
            try (InputStream in = jarFile.getInputStream(entry)) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static URL toUrl(Path path) throws IOException {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
    }
}
