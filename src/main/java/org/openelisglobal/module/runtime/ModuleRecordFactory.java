package org.openelisglobal.module.runtime;

import java.nio.file.Path;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * Creates records outside discovery, for the module test kit and the core's own
 * tests.
 */
public final class ModuleRecordFactory {

    private ModuleRecordFactory() {
    }

    public static ModuleRecord create(Path jarPath, String sha256, ModuleManifest manifest) {
        return new ModuleRecord(jarPath, sha256, manifest, manifest.getId());
    }
}
