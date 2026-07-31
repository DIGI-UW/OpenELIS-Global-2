package org.openelisglobal.common.domain;

/**
 * The single source of truth for the catalog domain — shared by tests
 * ({@code test.domain}) and sample types ({@code type_of_sample.domain}), and
 * served to the UI by {@code GET /rest/domains} so nothing hard-codes the list.
 *
 * <p>
 * Stored form is the enum name (CLINICAL / ENVIRONMENTAL / VECTOR). Sample
 * types may still present a legacy one-character {@code sample_domain} code
 * (D-030, OGC-1145) from un-migrated or plugin-inserted rows — H uman and N
 * ewborn fold into CLINICAL, E nvironmental into ENVIRONMENTAL, A nimal into
 * VECTOR — so {@link #fromRaw(String)} accepts either representation. All
 * domain interpretation, on the server and (mirrored) in the client, goes
 * through this enum.
 */
public enum Domain {

    CLINICAL("label.domain.CLINICAL"), ENVIRONMENTAL("label.domain.ENVIRONMENTAL"), VECTOR("label.domain.VECTOR");

    /** Default when a raw value is blank or unrecognized. */
    public static final Domain DEFAULT = CLINICAL;

    private final String labelKey;

    Domain(String labelKey) {
        this.labelKey = labelKey;
    }

    /** i18n message key for this domain's display label. */
    public String getLabelKey() {
        return labelKey;
    }

    /**
     * Legacy char or enum value (any case) → the domain, or {@code null} if the
     * value is blank or unrecognized. Callers that need a stored value should use
     * {@link #normalize(String)}; callers guarding compatibility treat {@code null}
     * as "offerable everywhere".
     */
    public static Domain fromRaw(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        switch (raw.trim().toUpperCase()) {
        case "E":
        case "ENVIRONMENTAL":
            return ENVIRONMENTAL;
        case "A":
        case "VECTOR":
            return VECTOR;
        case "H":
        case "N":
        case "CLINICAL":
            return CLINICAL;
        default:
            return null;
        }
    }

    /**
     * Canonical stored value for any raw input — the enum name, defaulting to
     * {@link #DEFAULT} for blank/unrecognized input (matching the OGC-936
     * test.domain backfill).
     */
    public static String normalize(String raw) {
        Domain domain = fromRaw(raw);
        return (domain == null ? DEFAULT : domain).name();
    }
}
