package org.openelisglobal.common.provider.query;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Exact-and-prefix name matching for the order entry patient search (OGC-1443,
 * FR-B6a). The shared patient search also matches names that contain the term
 * or are spelled close to it, which suits finding a patient to manage but makes
 * it easy to pick the wrong patient for an order; order entry keeps only the
 * names that start with what was typed and leaves sound-alike matching to the
 * possible-matches check on a new patient. The search already returns every
 * name that contains the term, so narrowing its results loses nothing.
 */
public final class PatientNamePrefixMatch {

    /** Request value that asks the patient search for prefix matching. */
    public static final String PREFIX = "prefix";

    private PatientNamePrefixMatch() {
    }

    /**
     * The results whose last and first names start with the given terms, ignoring
     * case and surrounding spaces. A blank term places no condition on its name.
     */
    public static List<PatientSearchResults> keepPrefixMatches(List<PatientSearchResults> results, String lastName,
            String firstName) {
        String last = normalize(lastName);
        String first = normalize(firstName);
        if (last.isEmpty() && first.isEmpty()) {
            return results;
        }
        return results.stream()
                .filter(result -> startsWith(result.getLastName(), last) && startsWith(result.getFirstName(), first))
                .collect(Collectors.toList());
    }

    private static boolean startsWith(String name, String term) {
        return term.isEmpty() || normalize(name).startsWith(term);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
