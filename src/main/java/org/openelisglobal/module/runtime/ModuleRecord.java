package org.openelisglobal.module.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.module.api.ModuleDescriptor;
import org.openelisglobal.module.api.ModuleKind;
import org.openelisglobal.module.api.ModuleState;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * The registry's record of one discovered JAR: the manifest, where it came
 * from, its state and why, the loader once it has one, and the reports gathered
 * at discovery. Mutable only from the runtime package.
 */
public final class ModuleRecord implements ModuleDescriptor {

    /**
     * One library bundled under {@code lib/} that the core also ships; the core's
     * copy is the one in use.
     */
    public record ShadowedLibrary(String bundled, String coreCopy) {
    }

    private final Path jarPath;
    private final String sha256;
    private final ModuleManifest manifest;
    private final String fallbackId;
    private volatile ModuleState state;
    private volatile String reason;
    private volatile ModuleClassLoader classLoader;
    private final List<ShadowedLibrary> shadowedLibraries = new ArrayList<>();
    private final List<String> bundledLibraries = new ArrayList<>();
    private volatile String recordedVersion;
    private volatile boolean firstInstall;
    private final List<String> warnings = new ArrayList<>();

    ModuleRecord(Path jarPath, String sha256, ModuleManifest manifest, String fallbackId) {
        this.jarPath = jarPath;
        this.sha256 = sha256;
        this.manifest = manifest;
        this.fallbackId = fallbackId;
        this.state = ModuleState.DISCOVERED;
    }

    @Override
    public String id() {
        return manifest != null && manifest.getId() != null ? manifest.getId() : fallbackId;
    }

    @Override
    public String name() {
        return manifest != null && manifest.getName() != null ? manifest.getName() : id();
    }

    @Override
    public String version() {
        return manifest != null ? manifest.getVersion() : null;
    }

    @Override
    public ModuleKind kind() {
        if (manifest == null || manifest.getKind() == null) {
            return ModuleKind.MODULE;
        }
        try {
            return ModuleKind.fromManifestValue(manifest.getKind());
        } catch (IllegalArgumentException e) {
            return ModuleKind.MODULE;
        }
    }

    /**
     * The manifest; {@code null} only for a JAR whose manifest could not be read at
     * all.
     */
    @Override
    public ModuleManifest manifest() {
        return manifest;
    }

    @Override
    public ModuleState state() {
        return state;
    }

    @Override
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public Path jarPath() {
        return jarPath;
    }

    @Override
    public String sha256() {
        return sha256;
    }

    public ModuleClassLoader classLoader() {
        return classLoader;
    }

    public List<ShadowedLibrary> shadowedLibraries() {
        return Collections.unmodifiableList(shadowedLibraries);
    }

    public List<String> bundledLibraries() {
        return Collections.unmodifiableList(bundledLibraries);
    }

    /**
     * The version recorded in {@code module_state} before this start; {@code null}
     * on first install.
     */
    public String recordedVersion() {
        return recordedVersion;
    }

    public boolean isFirstInstall() {
        return firstInstall;
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public boolean isActive() {
        return state.isActive();
    }

    /**
     * Runtime only: moves the module to a new state with the reason the registry
     * shows.
     */
    public void transition(ModuleState newState, String newReason) {
        this.state = newState;
        this.reason = newReason;
    }

    /** Runtime only. */
    public void transition(ModuleState newState) {
        transition(newState, null);
    }

    /** Runtime only. */
    public void setClassLoader(ModuleClassLoader loader) {
        this.classLoader = loader;
    }

    /** Runtime only. */
    public void addShadowedLibrary(String bundled, String coreCopy) {
        shadowedLibraries.add(new ShadowedLibrary(bundled, coreCopy));
    }

    /** Runtime only. */
    public void addBundledLibrary(String name) {
        bundledLibraries.add(name);
    }

    /**
     * Runtime only: the version found in {@code module_state} before this start,
     * {@code null} on first install.
     */
    public void setRecordedVersion(String version) {
        this.recordedVersion = version;
        this.firstInstall = version == null;
    }

    /** Runtime only. */
    public void addWarning(String warning) {
        warnings.add(warning);
    }

    @Override
    public String toString() {
        return "module " + id() + " " + version() + " [" + state + (reason == null ? "" : ": " + reason) + "]";
    }
}
