package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for activation validation
 * 
 * 
 * Contains validation results for analyzer mapping activation including: -
 * Whether activation can proceed - Missing required mappings - Pending message
 * count - Warnings and errors
 */
public class ActivationValidationResult {

    private boolean canActivate;
    private List<String> missingRequired;
    private Integer pendingMessagesCount;
    private List<String> warnings;

    public ActivationValidationResult() {
        this.canActivate = false;
        this.missingRequired = new ArrayList<>();
        this.pendingMessagesCount = 0;
        this.warnings = new ArrayList<>();
    }

    public boolean isCanActivate() {
        return canActivate;
    }

    public void setCanActivate(boolean canActivate) {
        this.canActivate = canActivate;
    }

    public List<String> getMissingRequired() {
        return missingRequired;
    }

    public void setMissingRequired(List<String> missingRequired) {
        this.missingRequired = missingRequired;
    }

    public Integer getPendingMessagesCount() {
        return pendingMessagesCount;
    }

    public void setPendingMessagesCount(Integer pendingMessagesCount) {
        this.pendingMessagesCount = pendingMessagesCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
