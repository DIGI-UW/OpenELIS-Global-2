package org.openelisglobal.typeofsample.util;

/**
 * Canonical mapping for the sample-type domain (OGC-296 v2.1). Storage is the
 * enum value (CLINICAL / ENVIRONMENTAL / VECTOR) since the Dependency-4
 * migration, but readers must stay bilingual: legacy one-character codes can
 * still arrive from test fixtures, CSV imports, and plugin-inserted rows. The
 * char mapping follows D-030 (OGC-1145): H uman and N ewborn fold into
 * CLINICAL, E nvironmental is ENVIRONMENTAL, A nimal is VECTOR; anything else
 * (including blank) defaults to CLINICAL, matching the OGC-936 test.domain
 * backfill.
 */
public final class SampleTypeDomainMapper {

    public static final String CLINICAL = "CLINICAL";
    public static final String ENVIRONMENTAL = "ENVIRONMENTAL";
    public static final String VECTOR = "VECTOR";

    private SampleTypeDomainMapper() {
    }

    /** Legacy char or enum value (any case) → canonical enum value. */
    public static String normalize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return CLINICAL;
        }
        switch (raw.trim().toUpperCase()) {
        case "E":
        case ENVIRONMENTAL:
            return ENVIRONMENTAL;
        case "A":
        case VECTOR:
            return VECTOR;
        default:
            return CLINICAL;
        }
    }
}
