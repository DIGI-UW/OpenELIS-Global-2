package org.openelisglobal.storage.service;

/**
 * Validation result for code validation Contains validation status, normalized
 * code, and error message
 */
public class CodeValidationResult {

    private boolean valid;
    private String normalizedCode;
    private String errorMessage;

    public CodeValidationResult() {
        this.valid = false;
    }

    public CodeValidationResult(boolean valid, String normalizedCode, String errorMessage) {
        this.valid = valid;
        this.normalizedCode = normalizedCode;
        this.errorMessage = errorMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getNormalizedCode() {
        return normalizedCode;
    }

    public void setNormalizedCode(String normalizedCode) {
        this.normalizedCode = normalizedCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Create a valid result
     */
    public static CodeValidationResult valid(String normalizedCode) {
        return new CodeValidationResult(true, normalizedCode, null);
    }

    /**
     * Create an invalid result with error message
     */
    public static CodeValidationResult invalid(String errorMessage) {
        return new CodeValidationResult(false, null, errorMessage);
    }
}
