package org.openelisglobal.storage.service;

/**
 * Result object for location deletion validation.
 *
 * Represents the outcome of validating whether a storage location can be
 * deleted. Includes error information if deletion is blocked by referential
 * integrity constraints.
 */
public class DeletionValidationResult {

    private final boolean success;
    private final String errorCode;
    private final String message;
    private final int dependentCount;

    /**
     * Create a validation result
     * 
     * @param success        true if location can be deleted, false otherwise
     * @param errorCode      Error code if deletion blocked (e.g.,
     *                       "REFERENTIAL_INTEGRITY_VIOLATION",
     *                       "ACTIVE_ASSIGNMENTS")
     * @param message        User-friendly error message explaining why deletion is
     *                       blocked
     * @param dependentCount Number of dependent entities preventing deletion (e.g.,
     *                       child locations, assigned samples)
     */
    public DeletionValidationResult(boolean success, String errorCode, String message, int dependentCount) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.dependentCount = dependentCount;
    }

    /**
     * Create a successful validation result (deletion allowed)
     */
    public static DeletionValidationResult success() {
        return new DeletionValidationResult(true, null, null, 0);
    }

    /**
     * Create a failed validation result due to referential integrity violation
     * 
     * @param entityType     Type of entity being deleted (e.g., "Room", "Device")
     * @param entityName     Name of entity being deleted
     * @param dependentType  Type of dependent entities (e.g., "devices", "shelves",
     *                       "samples")
     * @param dependentCount Number of dependent entities
     */
    public static DeletionValidationResult referentialIntegrityViolation(String entityType, String entityName,
            String dependentType, int dependentCount) {
        String message = String.format("Cannot delete %s '%s': contains %d %s", entityType, entityName, dependentCount,
                dependentType);
        return new DeletionValidationResult(false, "REFERENTIAL_INTEGRITY_VIOLATION", message, dependentCount);
    }

    /**
     * Create a failed validation result due to active sample assignments
     * 
     * @param entityType  Type of entity being deleted (e.g., "Rack")
     * @param entityName  Name of entity being deleted
     * @param sampleCount Number of samples currently assigned
     */
    public static DeletionValidationResult activeAssignments(String entityType, String entityName, int sampleCount) {
        String message = String.format("Cannot delete %s '%s': has %d assigned samples", entityType, entityName,
                sampleCount);
        return new DeletionValidationResult(false, "ACTIVE_ASSIGNMENTS", message, sampleCount);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public int getDependentCount() {
        return dependentCount;
    }
}
