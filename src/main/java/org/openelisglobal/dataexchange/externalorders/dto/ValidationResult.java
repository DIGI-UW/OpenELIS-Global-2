package org.openelisglobal.dataexchange.externalorders.dto;

/**
 * Represents the validation result for a single test or panel reference.
 */
public class ValidationResult {

    private String guid;
    private String loinc;
    private String resolvedId;
    private String resolvedName;
    private String rejectionReason;
    private boolean valid;

    public ValidationResult() {
    }

    // Factory methods for clean construction
    public static ValidationResult validForGuid(String guid, String resolvedId, String resolvedName) {
        ValidationResult vr = new ValidationResult();
        vr.setGuid(guid);
        vr.setResolvedId(resolvedId);
        vr.setResolvedName(resolvedName);
        vr.setValid(true);
        return vr;
    }

    public static ValidationResult validForLoinc(String loinc, String resolvedId, String resolvedName) {
        ValidationResult vr = new ValidationResult();
        vr.setLoinc(loinc);
        vr.setResolvedId(resolvedId);
        vr.setResolvedName(resolvedName);
        vr.setValid(true);
        return vr;
    }

    public static ValidationResult invalidForGuid(String guid) {
        ValidationResult vr = new ValidationResult();
        vr.setGuid(guid);
        vr.setRejectionReason(guid == null || guid.trim().isEmpty() ? "MISSING_GUID" : "GUID_NOT_FOUND");
        vr.setValid(false);
        return vr;
    }

    public static ValidationResult invalidForLoinc(String loinc) {
        ValidationResult vr = new ValidationResult();
        vr.setLoinc(loinc);
        vr.setRejectionReason(loinc == null || loinc.trim().isEmpty() ? "MISSING_LOINC" : "LOINC_NOT_FOUND");
        vr.setValid(false);
        return vr;
    }

    // Getters and Setters
    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    public String getResolvedId() {
        return resolvedId;
    }

    public void setResolvedId(String resolvedId) {
        this.resolvedId = resolvedId;
    }

    public String getResolvedName() {
        return resolvedName;
    }

    public void setResolvedName(String resolvedName) {
        this.resolvedName = resolvedName;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
