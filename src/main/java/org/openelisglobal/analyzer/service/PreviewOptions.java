package org.openelisglobal.analyzer.service;

/**
 * Options for mapping preview operation
 * 
 */
public class PreviewOptions {
    private boolean includeDetailedParsing = false;
    private boolean validateAllMappings = false;

    public boolean isIncludeDetailedParsing() {
        return includeDetailedParsing;
    }

    public void setIncludeDetailedParsing(boolean includeDetailedParsing) {
        this.includeDetailedParsing = includeDetailedParsing;
    }

    public boolean isValidateAllMappings() {
        return validateAllMappings;
    }

    public void setValidateAllMappings(boolean validateAllMappings) {
        this.validateAllMappings = validateAllMappings;
    }
}
