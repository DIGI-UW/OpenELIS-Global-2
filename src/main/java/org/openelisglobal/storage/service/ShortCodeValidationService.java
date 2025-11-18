package org.openelisglobal.storage.service;

/**
 * Service for validating short codes for storage locations Validates format,
 * uniqueness, and generates change warnings
 */
public interface ShortCodeValidationService {

    /**
     * Validate short code format Rules: - Max 10 characters - Alphanumeric, hyphen,
     * underscore only - Must start with letter or number (not hyphen/underscore) -
     * Auto-converts to uppercase
     * 
     * @param shortCode The short code to validate
     * @return Validation result with normalized code and error message
     */
    ShortCodeValidationResult validateFormat(String shortCode);

    /**
     * Validate short code uniqueness within context Checks if short code already
     * exists for a different location of the same type
     * 
     * @param shortCode  The short code to validate
     * @param context    The context type: "device", "shelf", or "rack"
     * @param locationId The ID of the location being validated (for updates, allows
     *                   same location)
     * @return Validation result
     */
    ShortCodeValidationResult validateUniqueness(String shortCode, String context, String locationId);

    /**
     * Check if short code change requires a warning Returns warning message if old
     * and new codes are different, null otherwise
     * 
     * @param oldCode    The previous short code (null for new locations)
     * @param newCode    The new short code
     * @param locationId The location ID
     * @return Warning message or null if no warning needed
     */
    String checkShortCodeChangeWarning(String oldCode, String newCode, String locationId);
}
