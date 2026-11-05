package org.openelisglobal.module.testkit;

import java.nio.file.Path;
import org.openelisglobal.module.api.manifest.ModuleManifest;
import org.openelisglobal.module.runtime.ModuleRecord;
import org.openelisglobal.module.runtime.ModuleRecordFactory;

/** Builds {@link ModuleRecord}s for tests without going through discovery. */
public final class ModuleRecords {

    private ModuleRecords() {
    }

    public static ModuleRecord record(Path jar, ModuleManifest manifest) {
        return ModuleRecordFactory.create(jar, "0".repeat(64), manifest);
    }
}
