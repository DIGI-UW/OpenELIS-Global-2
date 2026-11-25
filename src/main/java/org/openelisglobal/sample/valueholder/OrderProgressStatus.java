package org.openelisglobal.sample.valueholder;

import org.apache.commons.validator.GenericValidator;

/**
 * Where an order stands in order entry (OGC-1266, FR-F5). Entered when Enter
 * Order is first saved, Samples prepared when Prepare Samples is completed,
 * Ready for testing when the optional Sample check releases it, and Cancelled
 * with a reason. An order is complete at Samples prepared when the Sample check
 * step is off, and at Ready for testing when it is on.
 */
public enum OrderProgressStatus {
    ENTERED, SAMPLES_PREPARED, READY_FOR_TESTING, CANCELLED;

    public static OrderProgressStatus fromStored(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The step that completed at this status, in the stepper's key space. */
    public boolean isAtLeast(OrderProgressStatus other) {
        return other != null && this != CANCELLED && other != CANCELLED && ordinal() >= other.ordinal();
    }
}
