package org.openelisglobal.terminology;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The code system a terminology mapping source names. LOINC and SNOMED use the
 * HL7-registered URIs; CIEL and OCL use their OpenConceptLab canonical URLs.
 * Any other source is an identifier the catalog owner chose.
 */
public final class TerminologySystems {

    public static final String LOINC = "http://loinc.org";

    /** The length of every terminology table's {@code source} column. */
    public static final int MAX_SOURCE_LENGTH = 20;

    private static final Pattern URI = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:\\S+$");

    private TerminologySystems() {
    }

    /**
     * The FHIR system URI: the canonical one for a known source, the source itself
     * when it is a URI, or null, so a source with no URI is skipped rather than
     * given a bogus system.
     */
    public static String urlOf(String source) {
        if (source == null) {
            return null;
        }
        String trimmed = source.trim();
        switch (trimmed.toUpperCase(Locale.ROOT)) {
        case "LOINC":
            return LOINC;
        case "SNOMED":
            return "http://snomed.info/sct";
        case "CIEL":
            return "https://openconceptlab.org/orgs/CIEL/sources/CIEL";
        case "OCL":
            return "https://openconceptlab.org";
        default:
            return URI.matcher(trimmed).matches() ? trimmed : null;
        }
    }

    /**
     * The system a source names, for matching codings: its URI when it has one,
     * otherwise the source itself; null for a blank source.
     */
    public static String systemOf(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        String url = urlOf(source);
        return url != null ? url : source.trim();
    }

    /**
     * A source as stored: a known one in its own spelling, any other as written,
     * trimmed; null when it is blank or does not fit the column.
     */
    public static String sourceOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        // Canonical URIs use the same persisted aliases as their short names.
        // Normalize before the column limit because CIEL's URI is longer than it.
        for (String known : new String[] { "LOINC", "SNOMED", "CIEL", "OCL" }) {
            if (urlOf(known).equals(trimmed)) {
                return known;
            }
        }
        if (trimmed.length() > MAX_SOURCE_LENGTH) {
            return null;
        }
        return switch (trimmed.toUpperCase(Locale.ROOT)) {
        case "LOINC", "SNOMED", "CIEL", "OCL" -> trimmed.toUpperCase(Locale.ROOT);
        default -> trimmed;
        };
    }
}
