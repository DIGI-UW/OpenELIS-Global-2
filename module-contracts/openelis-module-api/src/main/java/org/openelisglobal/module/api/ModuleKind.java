package org.openelisglobal.module.api;

/** The two shapes a module JAR declares through the manifest's {@code kind}. */
public enum ModuleKind {

    /**
     * Implements extension points and nothing else: no entities, no changelog, no
     * controllers, no frontend. The shape for a site-specific report, billing
     * connector, notification channel or similar swap at one point.
     */
    PROVIDER,

    /**
     * May declare everything: beans, controllers under {@code /rest/modules/<id>/},
     * extensions, entities, a Liquibase changelog, permissions, menu entries and a
     * prebuilt frontend. The shape for a whole workflow.
     */
    MODULE;

    /** The manifest spelling: {@code provider} or {@code module}. */
    public String manifestValue() {
        return name().toLowerCase();
    }

    public static ModuleKind fromManifestValue(String value) {
        for (ModuleKind kind : values()) {
            if (kind.manifestValue().equals(value)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("kind must be 'provider' or 'module', not '" + value + "'");
    }
}
