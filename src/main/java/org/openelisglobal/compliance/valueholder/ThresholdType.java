package org.openelisglobal.compliance.valueholder;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Enumeration for threshold types in compliance evaluations.
 *
 * Defines different types of threshold conditions that can be applied to
 * parameter values during compliance evaluation.
 */
@Getter
public enum ThresholdType {

    /**
     * Value must be within a specified range (min <= value <= max)
     */
    RANGE("Range", "Acceptable range with minimum and maximum values"),

    /**
     * Value must not be less than the minimum (value >= min)
     */
    MINIMUM("Minimum", "Minimum acceptable value"),

    /**
     * Value must not exceed the maximum (value <= max)
     */
    MAXIMUM("Maximum", "Maximum acceptable value"),

    /**
     * Value should match the target value exactly
     */
    EXACT("Exact", "Exact target value required"),

    /**
     * Borderline/advisory zone - triggers review flag but not non-compliant
     */
    BORDERLINE("Borderline", "Advisory warning zone requiring review but not marking as non-compliant"),

    /**
     * Qualitative/text condition (e.g. "No odor", "Clear", "Absent"). Stored in
     * value_descriptive. Cannot be auto-evaluated - the engine flags results
     * against DESCRIPTIVE thresholds as Manual Review Required.
     */
    DESCRIPTIVE("Qualitative", "Free-text qualitative condition requiring manual analyst review"),

    /**
     * Container for select-list (predefined option) tests. The numeric value fields
     * are unused; instead, ComplianceThresholdValueMap rows attached to the
     * threshold map each result option to a ComplianceStatus (COMPLIANT /
     * BORDERLINE / NON_COMPLIANT).
     */
    SELECT_MAP("Select Mapping", "Select-list parent record - mappings live in ComplianceThresholdValueMap");

    private final String displayName;
    private final String description;

    ThresholdType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Get threshold type by display name (case insensitive)
     */
    public static ThresholdType fromDisplayName(String displayName) {
        for (ThresholdType type : values()) {
            if (type.displayName.equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No ThresholdType with display name: " + displayName);
    }

    /**
     * Resolve a threshold-type token coming from a CSV import or a permissive
     * external caller. Accepts: - the canonical enum name (RANGE, MINIMUM, MAXIMUM,
     * EXACT, BORDERLINE, DESCRIPTIVE, SELECT_MAP) - the display name ("Range",
     * "Minimum", "Maximum", ...) - the FRS aliases: HIGH -> MAXIMUM LOW -> MINIMUM
     * MAX -> MAXIMUM MIN -> MINIMUM QUALITATIVE -> DESCRIPTIVE SELECT -> SELECT_MAP
     * SELECT-MAP -> SELECT_MAP Tokens are trimmed and case-insensitive. Whitespace
     * and hyphen are treated like underscore so "Select Mapping", "select-map", and
     * "SELECT_MAP" all resolve to the same value.
     */
    public static ThresholdType fromImportToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("Threshold type token cannot be null");
        }
        String normalized = token.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Threshold type token cannot be blank");
        }
        switch (normalized) {
        case "HIGH":
        case "MAX":
        case "MAXIMUM":
            return MAXIMUM;
        case "LOW":
        case "MIN":
        case "MINIMUM":
            return MINIMUM;
        case "RANGE":
            return RANGE;
        case "EXACT":
            return EXACT;
        case "BORDERLINE":
            return BORDERLINE;
        case "DESCRIPTIVE":
        case "QUALITATIVE":
            return DESCRIPTIVE;
        case "SELECT":
        case "SELECT_MAP":
        case "SELECT_MAPPING":
            return SELECT_MAP;
        default:
            // Last resort: try the canonical enum name.
            try {
                return ThresholdType.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unrecognized threshold type token: " + token);
            }
        }
    }

    /**
     * Check if this threshold type requires a minimum value
     */
    public boolean requiresMinValue() {
        return this == RANGE || this == MINIMUM || this == BORDERLINE;
    }

    /**
     * Check if this threshold type requires a maximum value
     */
    public boolean requiresMaxValue() {
        return this == RANGE || this == MAXIMUM || this == BORDERLINE;
    }

    /**
     * Check if this threshold type requires a target value
     */
    public boolean requiresTargetValue() {
        return this == EXACT;
    }

    /**
     * Check if this threshold type requires a free-text qualitative value
     */
    public boolean requiresDescriptiveValue() {
        return this == DESCRIPTIVE;
    }

    /**
     * Check if this threshold type supports numeric values
     */
    public boolean supportsNumericValues() {
        return this != DESCRIPTIVE && this != SELECT_MAP;
    }

    /**
     * Check if this threshold type supports text values
     */
    public boolean supportsTextValues() {
        return this == EXACT || this == DESCRIPTIVE || this == SELECT_MAP;
    }

    /**
     * SELECT_MAP thresholds carry no numeric/text fields directly - their verdicts
     * come from attached ComplianceThresholdValueMap rows.
     */
    public boolean usesValueMapping() {
        return this == SELECT_MAP;
    }

    /**
     * Whether the engine should flag results against this threshold as needing
     * manual analyst review instead of auto-evaluating compliance.
     */
    public boolean requiresManualReview() {
        return this == DESCRIPTIVE;
    }

    /**
     * Evaluate if a numeric value is compliant with this threshold
     */
    public boolean evaluate(BigDecimal value, BigDecimal minValue, BigDecimal maxValue, BigDecimal targetValue) {
        if (value == null) {
            return false;
        }

        switch (this) {
        case RANGE:
            return minValue != null && maxValue != null && value.compareTo(minValue) >= 0
                    && value.compareTo(maxValue) <= 0;
        case MINIMUM:
            return minValue != null && value.compareTo(minValue) >= 0;
        case MAXIMUM:
            return maxValue != null && value.compareTo(maxValue) <= 0;
        case BORDERLINE:
            // BORDERLINE works like RANGE but indicates advisory zone
            return minValue != null && maxValue != null && value.compareTo(minValue) >= 0
                    && value.compareTo(maxValue) <= 0;
        case EXACT:
            return targetValue != null && value.compareTo(targetValue) == 0;
        case DESCRIPTIVE:
            // DESCRIPTIVE thresholds cannot be auto-evaluated against a numeric value;
            // the engine surfaces these as Manual Review Required rather than
            // returning a compliant/non-compliant boolean.
            return false;
        case SELECT_MAP:
            // SELECT_MAP thresholds carry no numeric semantics; the engine must
            // look up the option's mapped ComplianceStatus instead of calling
            // this overload.
            return false;
        default:
            return false;
        }
    }

    /**
     * Evaluate if a text value is compliant with this threshold
     */
    public boolean evaluate(String value, String expectedValue) {
        if (this == EXACT) {
            return value != null && expectedValue != null && value.equalsIgnoreCase(expectedValue);
        }
        // DESCRIPTIVE / SELECT_MAP cannot be auto-evaluated against a single
        // expectedValue - callers must consult requiresManualReview() or look
        // up ComplianceThresholdValueMap rows respectively.
        return false;
    }

    @Override
    public String toString() {
        return displayName;
    }
}