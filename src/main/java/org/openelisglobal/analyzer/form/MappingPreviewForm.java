package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for mapping preview request
 * 
 */
public class MappingPreviewForm {

    @NotBlank(message = "ASTM message is required")
    @Size(max = 10240, message = "ASTM message must not exceed 10KB")
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
