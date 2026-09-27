package org.openelisglobal.compliance.valueholder;

import lombok.Getter;

/**
 * Enumeration for compliance status values used in threshold value mapping.
 *
 * Used by ComplianceThresholdValueMap to map select list test result options to
 * compliance outcomes. Required by FR-3-013 and FR-3-014.
 */
@Getter
public enum ComplianceStatus {
    COMPLIANT("Compliant", "Result meets regulatory compliance requirements"),
    BORDERLINE("Borderline", "Result is in advisory zone requiring review"),
    NON_COMPLIANT("Non-Compliant", "Result does not meet regulatory compliance requirements");

    private final String displayName;
    private final String description;

    ComplianceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get status by display name (case insensitive)
     */
    public static ComplianceStatus fromDisplayName(String displayName) {
        for (ComplianceStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No ComplianceStatus with display name: " + displayName);
    }

    /**
     * Lenient parser for CSV / external input. Accepts the enum name
     * ({@code NON_COMPLIANT}), the display name ({@code Non-Compliant}), and the
     * common typed-by-hand variants in between (case-insensitive, whitespace
     * trimmed, '-' or whitespace treated like '_'). The strict
     * {@link #valueOf(String)} would silently reject all but the canonical enum
     * name, which caused valid seed/import rows to be skipped.
     */
    public static ComplianceStatus parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("ComplianceStatus value is null");
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("ComplianceStatus value is blank");
        }
        String normalized = trimmed.toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return ComplianceStatus.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // Fall through to display-name match (e.g. localized "Non-Compliant").
        }
        return fromDisplayName(trimmed);
    }

    /**
     * Check if this status indicates compliance failure
     */
    public boolean isNonCompliant() {
        return this == NON_COMPLIANT;
    }

    /**
     * Check if this status requires review/attention
     */
    public boolean requiresReview() {
        return this == BORDERLINE || this == NON_COMPLIANT;
    }

    /**
     * Check if this status passes compliance evaluation
     */
    public boolean isCompliant() {
        return this == COMPLIANT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}