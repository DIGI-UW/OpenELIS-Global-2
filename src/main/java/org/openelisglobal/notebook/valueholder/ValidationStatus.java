package org.openelisglobal.notebook.valueholder;

/**
 * Validation status for result compilation per US7 requirements. Used to flag
 * samples as valid, invalid, or inconclusive during result review.
 */
public enum ValidationStatus {

    /** Result is valid and confirmed */
    VALID("Valid", "green"),

    /** Result is invalid and should be excluded */
    INVALID("Invalid", "red"),

    /** Result is inconclusive and needs review */
    INCONCLUSIVE("Inconclusive", "yellow"),

    /** Not yet validated */
    PENDING("Pending Validation", "gray");

    private final String displayName;
    private final String tagColor;

    ValidationStatus(String displayName, String tagColor) {
        this.displayName = displayName;
        this.tagColor = tagColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTagColor() {
        return tagColor;
    }

    /**
     * Parse from string, case-insensitive.
     *
     * @param value String representation
     * @return ValidationStatus or PENDING if not found
     */
    public static ValidationStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
