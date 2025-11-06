package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for sample movement
 */
public class SampleMovementForm {

    @NotBlank(message = "Sample ID is required")
    private String sampleId;

    @NotBlank(message = "Target position ID is required")
    private String targetPositionId;

    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    // Getters and Setters

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getTargetPositionId() {
        return targetPositionId;
    }

    public void setTargetPositionId(String targetPositionId) {
        this.targetPositionId = targetPositionId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

