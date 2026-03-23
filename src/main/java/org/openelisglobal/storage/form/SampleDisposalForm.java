package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for SampleItem disposal OGC-73: Supports sample disposal with
 * reason, method, and notes
 */
public class SampleDisposalForm {

    @NotBlank(message = "SampleItem ID is required")
    private String sampleItemId;

    @NotBlank(message = "Disposal reason is required")
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String reason;

    @NotBlank(message = "Disposal method is required")
    @Size(max = 100, message = "Method must not exceed 100 characters")
    private String method;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Getters and Setters

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
