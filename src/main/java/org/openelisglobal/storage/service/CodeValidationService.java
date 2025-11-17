package org.openelisglobal.storage.service;

/**
 * Service for validating codes for storage locations Validates format, length,
 * and uniqueness
 */
public interface CodeValidationService {

    /**
     * Validate code format Rules: - Max 10 characters - Alphanumeric, hyphen,
     * underscore only - Must start with letter or number (not hyphen/underscore) -
     * Auto-converts to uppercase
     * 
     * @param code The code to validate
     * @return Validation result with normalized code and error message
     */
    CodeValidationResult validateFormat(String code);

    /**
     * Validate code length
     * 
     * @param code The code to validate
     * @return Validation result
     */
    CodeValidationResult validateLength(String code);

    /**
     * Validate code uniqueness within context Checks if code already exists for a
     * different location Room: globally unique; Device/Shelf/Rack: unique within
     * parent
     * 
     * @param code       The code to validate
     * @param context    The context type: "room", "device", "shelf", or "rack"
     * @param locationId The ID of the location being validated (for updates, allows
     *                   same location)
     * @param parentId   The parent ID (for Device/Shelf/Rack, required for
     *                   uniqueness check)
     * @return Validation result
     */
    CodeValidationResult validateUniqueness(String code, String context, String locationId, String parentId);

    /**
     * Auto-uppercase code
     * 
     * @param code The code to uppercase
     * @return Uppercased code
     */
    String autoUppercase(String code);
}
