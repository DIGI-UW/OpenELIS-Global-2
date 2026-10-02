package org.openelisglobal.organization.valueholder;

import java.util.Arrays;
import java.util.Locale;

/**
 * OGC-1363 (FR-D1): the kind of care a ward or department gives, with the
 * WHONET / GLASS location-type code that antimicrobial resistance exports need,
 * so AMR reports can split inpatient from outpatient isolates without a
 * separate mapping step.
 */
public enum WardServiceType {
    INPATIENT("Inpatient", "inpatient"), OUTPATIENT("Outpatient", "outpatient"),
    INTENSIVE_CARE("Intensive care", "icu"), EMERGENCY("Emergency", "emergency"), MATERNITY("Maternity", "maternity"),
    LABORATORY("Laboratory", "laboratory"), OTHER("Other", "other");

    private final String label;
    private final String whonetCode;

    WardServiceType(String label, String whonetCode) {
        this.label = label;
        this.whonetCode = whonetCode;
    }

    public String getLabel() {
        return label;
    }

    public String getWhonetCode() {
        return whonetCode;
    }

    /** The type named by its code or its label, in any case; null when unknown. */
    public static WardServiceType parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String wanted = text.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Arrays.stream(values())
                .filter(type -> type.name().toLowerCase(Locale.ROOT).replace('_', ' ').equals(wanted)
                        || type.label.toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst().orElse(null);
    }
}
