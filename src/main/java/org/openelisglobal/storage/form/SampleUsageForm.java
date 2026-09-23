package org.openelisglobal.storage.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form object for recording sample usage against a SampleItem's remaining
 * quantity. OGC-1026 (Results Entry v3 R7): partial use decrements the
 * remaining quantity; {@code markUsedUp} zeroes it (exhausted), which makes the
 * sample eligible for the disposal workflow.
 */
public class SampleUsageForm {

    @NotBlank(message = "SampleItem ID is required")
    private String sampleItemId;

    @Size(max = 20, message = "Amount must not exceed 20 characters")
    private String amountUsed;

    private boolean markUsedUp;

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getAmountUsed() {
        return amountUsed;
    }

    public void setAmountUsed(String amountUsed) {
        this.amountUsed = amountUsed;
    }

    public boolean isMarkUsedUp() {
        return markUsedUp;
    }

    public void setMarkUsedUp(boolean markUsedUp) {
        this.markUsedUp = markUsedUp;
    }
}
