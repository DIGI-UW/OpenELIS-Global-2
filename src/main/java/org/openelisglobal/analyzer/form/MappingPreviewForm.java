package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Form object for mapping preview request
 * 
 */
public class MappingPreviewForm {

    @NotBlank(message = "ASTM message is required")
    // Max size enforced in AnalyzerFieldMappingRestController.previewMapping so
    // oversized payloads return 413 PAYLOAD_TOO_LARGE instead of 400 from Bean
    // Validation.
    private String astmMessage;

    private boolean includeDetailedParsing = false;
    private boolean validateAllMappings = false;

    public String getAstmMessage() {
        return astmMessage;
    }

    public void setAstmMessage(String astmMessage) {
        this.astmMessage = astmMessage;
    }

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
