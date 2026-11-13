package org.openelisglobal.module.manifest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.module.api.manifest.ModuleManifest;

/**
 * The outcome of validating a manifest: the bound model when the schema
 * accepted it, and every problem found by the schema and by the manifest rules.
 * A manifest with any problem is {@code INVALID}.
 */
public final class ManifestValidation {

    /** One problem, located by a JSON pointer-like path. */
    public record Problem(String path, String message) {

        @Override
        public String toString() {
            return path.isEmpty() ? message : path + ": " + message;
        }
    }

    private final ModuleManifest manifest;
    private final List<Problem> problems;

    ManifestValidation(ModuleManifest manifest, List<Problem> problems) {
        this.manifest = manifest;
        this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
    }

    public boolean isValid() {
        return problems.isEmpty();
    }

    /**
     * The bound manifest; {@code null} when the document did not pass the schema.
     */
    public ModuleManifest manifest() {
        return manifest;
    }

    public List<Problem> problems() {
        return problems;
    }

    /** Every problem on one line, for the log and the registry's reason field. */
    public String describe() {
        return problems.stream().map(Problem::toString).collect(Collectors.joining("; "));
    }
}
