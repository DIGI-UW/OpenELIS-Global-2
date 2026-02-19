package org.openelisglobal.analyzer.service;

import java.util.List;
import org.openelisglobal.analyzer.valueholder.ValidationRuleConfiguration;

/**
 * ValidationRuleEngine interface - Stateless validation engine for custom field
 * types
 * 
 * Per FR-018: Custom field types MUST include validation rules (e.g., format
 * patterns, value ranges, allowed characters) and MUST be available for use in
 * field mapping configuration.
 * 
 */
public interface ValidationRuleEngine {
    /**
     * Evaluate a validation rule against a value
     * 
     * @param value The value to validate
     * @param rule  The validation rule configuration
     * @return true if value passes validation, false otherwise
     * @throws IllegalArgumentException if rule expression is invalid
     */
    boolean evaluateRule(String value, ValidationRuleConfiguration rule);

    /**
     * Validate value against regex pattern
     * 
     * @param value   The value to validate
     * @param pattern The regex pattern
     * @return true if value matches pattern, false otherwise
     */
    boolean validateRegex(String value, String pattern);

    /**
     * Validate numeric value against range
     * 
     * @param value The numeric value
     * @param min   Minimum value (inclusive)
     * @param max   Maximum value (inclusive)
     * @return true if value is within range, false otherwise
     */
    boolean validateRange(Number value, Number min, Number max);

    /**
     * Validate value against enumeration of allowed values
     * 
     * @param value         The value to validate
     * @param allowedValues List of allowed values
     * @return true if value is in allowed values list, false otherwise
     */
    boolean validateEnum(String value, List<String> allowedValues);

    /**
     * Validate string length
     * 
     * @param value     The string value
     * @param minLength Minimum length (inclusive)
     * @param maxLength Maximum length (inclusive)
     * @return true if length is within range, false otherwise
     */
    boolean validateLength(String value, Integer minLength, Integer maxLength);
}
