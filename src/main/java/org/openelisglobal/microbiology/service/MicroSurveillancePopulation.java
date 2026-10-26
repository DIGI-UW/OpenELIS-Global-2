package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.valueholder.MicroCulturePurpose;

/**
 * Shared population policy for WHONET and future clinical surveillance
 * consumers.
 */
public final class MicroSurveillancePopulation {
    private MicroSurveillancePopulation() {
    }

    public static boolean whonetPurpose(String purpose, boolean environmental, boolean includeEnvironmental,
            boolean includeScreening, boolean surveySelected) {
        if (environmental)
            return includeEnvironmental && MicroCulturePurpose.options(true).contains(purpose);
        return switch (purpose == null ? "" : purpose) {
        case "DIAGNOSTIC", "TREATMENT_FOLLOW_UP" -> true;
        case "SCREENING" -> includeScreening;
        case "SURVEY_OR_STUDY" -> surveySelected;
        default -> false;
        };
    }

    public static boolean clinicalSurveillance(String purpose, boolean environmental, boolean external) {
        return !environmental && !external && "DIAGNOSTIC".equals(purpose);
    }

    public static boolean excludedCulture(String track, List<String> outcomes, boolean receivedIsolate) {
        return (!receivedIsolate && outcomes.isEmpty())
                || ("TB".equalsIgnoreCase(track) && outcomes.contains("CONTAMINATED"));
    }
}
