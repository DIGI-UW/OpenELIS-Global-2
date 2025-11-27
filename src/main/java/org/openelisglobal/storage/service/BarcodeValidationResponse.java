package org.openelisglobal.storage.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Response object for barcode validation Contains validation result and
 * component details for form pre-filling
 */
public class BarcodeValidationResponse {

    private boolean valid;
    private String barcode;
    private String barcodeType; // 'location' | 'sample' | 'unknown'
    private String failedStep;
    private String errorMessage;
    private Map<String, Object> validComponents;
    private String firstMissingLevel; // 'device' | 'shelf' | 'rack' | 'position' | null if all valid or completely
                                      // invalid
    private boolean hasAdditionalInvalidLevels; // true if there are invalid levels beyond valid portion

    public BarcodeValidationResponse() {
        this.validComponents = new HashMap<>();
        this.hasAdditionalInvalidLevels = false;
    }

    // Getters and setters
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    public String getFailedStep() {
        return failedStep;
    }

    public void setFailedStep(String failedStep) {
        this.failedStep = failedStep;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getValidComponents() {
        return validComponents;
    }

    public void setValidComponents(Map<String, Object> validComponents) {
        this.validComponents = validComponents;
    }

    /**
     * Add a valid component to the response
     */
    public void addValidComponent(String key, Object value) {
        this.validComponents.put(key, value);
    }

    public String getFirstMissingLevel() {
        return firstMissingLevel;
    }

    public void setFirstMissingLevel(String firstMissingLevel) {
        this.firstMissingLevel = firstMissingLevel;
    }

    public boolean isHasAdditionalInvalidLevels() {
        return hasAdditionalInvalidLevels;
    }

    public void setHasAdditionalInvalidLevels(boolean hasAdditionalInvalidLevels) {
        this.hasAdditionalInvalidLevels = hasAdditionalInvalidLevels;
    }
}
