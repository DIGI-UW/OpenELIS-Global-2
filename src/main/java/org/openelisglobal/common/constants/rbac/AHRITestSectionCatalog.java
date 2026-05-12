package org.openelisglobal.common.constants.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * AHRI test-section catalog used for optional filtering of active sections.
 */
public final class AHRITestSectionCatalog {

    private static final Set<String> TEST_SECTION_NAMES;

    static {
        Set<String> names = new HashSet<>();
        TEST_SECTION_NAMES = Collections.unmodifiableSet(normalizeAll(names));
    }

    private AHRITestSectionCatalog() {
        // Utility class
    }

    /**
     * Returns true when the section should be included.
     * If the catalog is empty, filtering is effectively disabled.
     */
    public static boolean contains(String sectionName) {
        String normalized = normalize(sectionName);
        if (normalized.isEmpty()) {
            return false;
        }
        if (TEST_SECTION_NAMES.isEmpty()) {
            return true;
        }
        return TEST_SECTION_NAMES.contains(normalized);
    }

    private static Set<String> normalizeAll(Set<String> source) {
        Set<String> normalized = new HashSet<>();
        for (String value : source) {
            normalized.add(normalize(value));
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
