package org.openelisglobal.common.constants.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * AHRI research-lab allowlist for user-management lab unit assignment.
 * Matches {@code volume/configuration/backend/notebook-departments/research-lab-linkages.csv}.
 */
public final class AHRITestSectionCatalog {

    private static final Set<String> TEST_SECTION_NAMES;

    static {
        Set<String> names = new HashSet<>();
        // notebookTitle and departmentName columns from research-lab-linkages.csv
        names.add("Traditional & Modern Medicine Research Lab");
        names.add("Bioanalytical Laboratory");
        names.add("Bioequivalence Laboratory");
        names.add("Immunology Laboratory");
        names.add("Immunology");
        names.add("Pathology Laboratory");
        names.add("Bacteriology Laboratory");
        names.add("Bacteriology");
        names.add("Malaria and Neglected Tropical Disease (MNTD) Laboratory");
        names.add("Pharmaceuticals Laboratory");
        names.add("Viral Vaccine");
        names.add("Tuberculosis Laboratory");
        names.add("CTD");
        names.add("Biorepository Laboratory");
        TEST_SECTION_NAMES = Collections.unmodifiableSet(normalizeAll(names));
    }

    private AHRITestSectionCatalog() {
        // Utility class
    }

    /**
     * Returns true when the section name is an AHRI research lab (csv allowlist).
     */
    public static boolean contains(String sectionName) {
        String normalized = normalize(sectionName);
        if (normalized.isEmpty()) {
            return false;
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
