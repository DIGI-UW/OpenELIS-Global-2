package org.openelisglobal.terminology;

/**
 * The canonical FHIR system URI of each terminology mapping source. LOINC and
 * SNOMED use the HL7-registered URIs; CIEL and OCL use their OpenConceptLab
 * canonical URLs.
 */
public final class TerminologySystems {

    private TerminologySystems() {
    }

    /**
     * Null for an unrecognized source, so it is skipped rather than given a bogus
     * system.
     */
    public static String urlOf(String source) {
        if (source == null) {
            return null;
        }
        switch (source.toUpperCase()) {
        case "LOINC":
            return "http://loinc.org";
        case "SNOMED":
            return "http://snomed.info/sct";
        case "CIEL":
            return "https://openconceptlab.org/orgs/CIEL/sources/CIEL";
        case "OCL":
            return "https://openconceptlab.org";
        default:
            return null;
        }
    }
}
