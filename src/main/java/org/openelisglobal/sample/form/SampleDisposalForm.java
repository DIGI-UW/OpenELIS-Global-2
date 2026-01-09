package org.openelisglobal.sample.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Form for bulk sample disposal operations per Stage 5 bioanalytical workflow.
 * Supports disposal scheduling with regulatory compliance tracking.
 */
public class SampleDisposalForm {

    @NotBlank(message = "At least one sample ID is required")
    private List<Integer> sampleIds;

    @NotBlank(message = "Disposal reason is required")
    @Size(max = 100, message = "Reason must not exceed 100 characters")
    private String disposalReason;

    @NotBlank(message = "Disposal method is required")
    @Size(max = 100, message = "Method must not exceed 100 characters")
    private String disposalMethod;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotBlank(message = "Supervisor approval is required")
    @Size(max = 255, message = "Supervisor name must not exceed 255 characters")
    private String supervisorApproval;

    public SampleDisposalForm() {
    }

    public SampleDisposalForm(List<Integer> sampleIds, String disposalReason,
                              String disposalMethod, String supervisorApproval) {
        this.sampleIds = sampleIds;
        this.disposalReason = disposalReason;
        this.disposalMethod = disposalMethod;
        this.supervisorApproval = supervisorApproval;
    }

    // Getters and Setters

    public List<Integer> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<Integer> sampleIds) {
        this.sampleIds = sampleIds;
    }

    public String getDisposalReason() {
        return disposalReason;
    }

    public void setDisposalReason(String disposalReason) {
        this.disposalReason = disposalReason;
    }

    public String getDisposalMethod() {
        return disposalMethod;
    }

    public void setDisposalMethod(String disposalMethod) {
        this.disposalMethod = disposalMethod;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSupervisorApproval() {
        return supervisorApproval;
    }

    public void setSupervisorApproval(String supervisorApproval) {
        this.supervisorApproval = supervisorApproval;
    }
}
