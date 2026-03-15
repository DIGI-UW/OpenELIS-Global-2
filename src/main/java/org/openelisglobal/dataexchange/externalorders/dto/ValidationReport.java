package org.openelisglobal.dataexchange.externalorders.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation report for an external order request. Contains patient validation
 * status and lists of valid/invalid tests and panels.
 */
public class ValidationReport {

    private boolean patientValid;
    private String patientGuid;
    private String patientRejectionReason;
    private List<ValidationResult> validTests = new ArrayList<>();
    private List<ValidationResult> invalidTests = new ArrayList<>();
    private List<ValidationResult> validPanels = new ArrayList<>();
    private List<ValidationResult> invalidPanels = new ArrayList<>();
    private int totalTestsReceived;
    private int totalPanelsReceived;
    private boolean overallValid;
    private boolean hasRemovedTests;
    private boolean hasRemovedPanels;

    public ValidationReport() {
    }

    /**
     * Returns true if the order can be stored (patient valid and at least one valid
     * test/panel, or has removals).
     */
    public boolean canStore() {
        return patientValid && (!validTests.isEmpty() || !validPanels.isEmpty() || hasRemovedTests || hasRemovedPanels);
    }

    /**
     * Returns true if everything is valid (patient, all tests, all panels).
     */
    public boolean isFullyValid() {
        return patientValid && invalidTests.isEmpty() && invalidPanels.isEmpty();
    }

    /**
     * Returns true if some tests/panels are invalid but order can still be stored.
     */
    public boolean isPartial() {
        return patientValid && (!invalidTests.isEmpty() || !invalidPanels.isEmpty()) && canStore();
    }

    /**
     * Returns true if nothing can be stored (invalid patient or no valid
     * tests/panels and no removals).
     */
    public boolean isCompletelyInvalid() {
        return !patientValid
                || (validTests.isEmpty() && validPanels.isEmpty() && !hasRemovedTests && !hasRemovedPanels);
    }

    // Getters and Setters
    public boolean isPatientValid() {
        return patientValid;
    }

    public void setPatientValid(boolean patientValid) {
        this.patientValid = patientValid;
    }

    public String getPatientGuid() {
        return patientGuid;
    }

    public void setPatientGuid(String patientGuid) {
        this.patientGuid = patientGuid;
    }

    public String getPatientRejectionReason() {
        return patientRejectionReason;
    }

    public void setPatientRejectionReason(String patientRejectionReason) {
        this.patientRejectionReason = patientRejectionReason;
    }

    public List<ValidationResult> getValidTests() {
        return validTests;
    }

    public void setValidTests(List<ValidationResult> validTests) {
        this.validTests = validTests != null ? validTests : new ArrayList<>();
    }

    public List<ValidationResult> getInvalidTests() {
        return invalidTests;
    }

    public void setInvalidTests(List<ValidationResult> invalidTests) {
        this.invalidTests = invalidTests != null ? invalidTests : new ArrayList<>();
    }

    public List<ValidationResult> getValidPanels() {
        return validPanels;
    }

    public void setValidPanels(List<ValidationResult> validPanels) {
        this.validPanels = validPanels != null ? validPanels : new ArrayList<>();
    }

    public List<ValidationResult> getInvalidPanels() {
        return invalidPanels;
    }

    public void setInvalidPanels(List<ValidationResult> invalidPanels) {
        this.invalidPanels = invalidPanels != null ? invalidPanels : new ArrayList<>();
    }

    public int getTotalTestsReceived() {
        return totalTestsReceived;
    }

    public void setTotalTestsReceived(int totalTestsReceived) {
        this.totalTestsReceived = totalTestsReceived;
    }

    public int getTotalPanelsReceived() {
        return totalPanelsReceived;
    }

    public void setTotalPanelsReceived(int totalPanelsReceived) {
        this.totalPanelsReceived = totalPanelsReceived;
    }

    public boolean isOverallValid() {
        return overallValid;
    }

    public void setOverallValid(boolean overallValid) {
        this.overallValid = overallValid;
    }

    public boolean isHasRemovedTests() {
        return hasRemovedTests;
    }

    public void setHasRemovedTests(boolean hasRemovedTests) {
        this.hasRemovedTests = hasRemovedTests;
    }

    public boolean isHasRemovedPanels() {
        return hasRemovedPanels;
    }

    public void setHasRemovedPanels(boolean hasRemovedPanels) {
        this.hasRemovedPanels = hasRemovedPanels;
    }
}
