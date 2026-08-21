package org.openelisglobal.analyzer.valueholder;

import java.util.Locale;

/** Bridge connection role initialized from the pinned analyzer profile. */
public enum AnalyzerConnectionRole {
    RECEIVER, INITIATOR;

    public static AnalyzerConnectionRole fromProfileValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
        case "SERVER", "RECEIVER" -> RECEIVER;
        case "CLIENT", "INITIATOR" -> INITIATOR;
        default -> null;
        };
    }
}
