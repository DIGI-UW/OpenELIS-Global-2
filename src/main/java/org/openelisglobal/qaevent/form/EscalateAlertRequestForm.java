package org.openelisglobal.qaevent.form;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Form class for delta check alert escalation requests. Supports both the
 * legacy modal flow (nceTitle/nceDescription) and the inline NCE form flow
 * (title/description + subcategory/immediateAction/sampleAction).
 */
public class EscalateAlertRequestForm {

    @Size(max = 200, message = "NCE title cannot exceed 200 characters")
    private String nceTitle;

    private String nceDescription;

    private String category = "Analytical";

    private String subcategory;

    @NotNull(message = "Severity is required")
    private String severity = "Major";

    private String immediateAction;

    private String sampleAction;

    public EscalateAlertRequestForm() {
    }

    public String getNceTitle() {
        return nceTitle;
    }

    public void setNceTitle(String nceTitle) {
        this.nceTitle = nceTitle;
    }

    public String getNceDescription() {
        return nceDescription;
    }

    public void setNceDescription(String nceDescription) {
        this.nceDescription = nceDescription;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getImmediateAction() {
        return immediateAction;
    }

    public void setImmediateAction(String immediateAction) {
        this.immediateAction = immediateAction;
    }

    public String getSampleAction() {
        return sampleAction;
    }

    public void setSampleAction(String sampleAction) {
        this.sampleAction = sampleAction;
    }
}
