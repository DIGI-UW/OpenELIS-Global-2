package org.openelisglobal.module.api;

import java.nio.file.Path;
import java.util.Optional;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * A discovered module as the registry sees it: its manifest, where it came from
 * and its current state.
 */
public interface ModuleDescriptor {

    /** The manifest {@code id}; the key of every namespace the module owns. */
    String id();

    /** The manifest {@code name}, for people. */
    String name();

    /** The manifest {@code version}. */
    String version();

    ModuleKind kind();

    /** The full parsed manifest. */
    ModuleManifest manifest();

    ModuleState state();

    /** Why the module is not {@link ModuleState#STARTED}, when it is not. */
    Optional<String> reason();

    /** The JAR the module was loaded from. */
    Path jarPath();

    /** The SHA-256 of the JAR, lower-case hex. */
    String sha256();
}
