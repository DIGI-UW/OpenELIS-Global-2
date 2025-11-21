package org.openelisglobal.storage.form;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form for updating short code
 */
public class ShortCodeUpdateForm {

    @Size(max = 10, message = "Short code cannot exceed 10 characters")
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]*$", message = "Short code must start with letter or number and contain only alphanumeric characters, hyphens, and underscores")
    private String shortCode;

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
}
