package org.openelisglobal.microbiology.valueholder;

import java.util.Arrays;
import java.util.List;

/** Domain-scoped purposes of a microbiology case. */
public enum MicroCulturePurpose {
    // Existing subculture classifications; case selectors use the domain-scoped
    // values below.
    CLINICAL_DIAGNOSTIC(false, true), ACTIVE_SCREENING(false, true), DIAGNOSTIC(false), SCREENING(false),
    TREATMENT_FOLLOW_UP(false), SURVEY_OR_STUDY(false), EQA_PROFICIENCY(false), ROUTINE_MONITORING(true),
    POST_CLEANING_CHECK(true), OUTBREAK_INVESTIGATION(true), COMPLAINT(true);

    private final boolean environmental;
    private final boolean subcultureOnly;

    MicroCulturePurpose(boolean environmental) {
        this(environmental, false);
    }

    MicroCulturePurpose(boolean environmental, boolean subcultureOnly) {
        this.environmental = environmental;
        this.subcultureOnly = subcultureOnly;
    }

    public static List<String> options(boolean environmental) {
        return Arrays.stream(values()).filter(p -> !p.subcultureOnly && p.environmental == environmental)
                .map(Enum::name).toList();
    }

    public static String validate(String value, boolean environmental) {
        if (value == null || !options(environmental).contains(value))
            throw new IllegalArgumentException("MICROBIOLOGY_CASE_PURPOSE_INVALID");
        return value;
    }

    public static String defaultFor(boolean environmental) {
        return (environmental ? ROUTINE_MONITORING : DIAGNOSTIC).name();
    }
}
