package org.openelisglobal.orderentry.valueholder;

import java.util.Locale;

/**
 * The condition a sample arrived in at the laboratory (FRS clinical order entry
 * v4, FR-C9a): a short coded list the order entry screen labels through
 * {@code sample.condition.*} text tags. Stored by name on
 * {@code sample_item.arrival_condition}.
 */
public enum ArrivalCondition {
    ROOM_TEMPERATURE, REFRIGERATED, FROZEN, ON_ICE, DRY_ICE;

    /**
     * The condition named by {@code value} (case and surrounding spaces ignored),
     * or null for a blank or unknown value, so a stale or tampered value is never
     * stored.
     */
    public static ArrivalCondition fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
