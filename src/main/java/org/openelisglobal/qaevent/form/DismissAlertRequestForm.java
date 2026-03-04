package org.openelisglobal.qaevent.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form class for alert dismissal requests. Provides validation for the dismiss
 * delta check alert endpoint.
 */
public class DismissAlertRequestForm {

    @NotBlank(message = "Reason is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String reason;

    public DismissAlertRequestForm() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
