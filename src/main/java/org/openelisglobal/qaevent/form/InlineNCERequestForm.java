package org.openelisglobal.qaevent.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Form class for inline NCE creation requests. Maps to the InlineNCERequest
 * schema in the OpenAPI specification.
 */
public class InlineNCERequestForm {

    @NotBlank(message = "Category is required")
    private String category = "Analytical";

    private String subcategory;

    @NotNull(message = "Severity is required")
    private String severity;

    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    private String description;

    private String immediateAction;

    @NotNull(message = "Sample action is required")
    private String sampleAction;

    // Trigger context fields for audit trail
    private String sourceType;

    private String triggerType;

    private String triggerAction;

    // Auto-populated context information (read-only)
    private Map<String, Object> autoPopulatedContext;

    public InlineNCERequestForm() {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public Map<String, Object> getAutoPopulatedContext() {
        return autoPopulatedContext;
    }

    public void setAutoPopulatedContext(Map<String, Object> autoPopulatedContext) {
        this.autoPopulatedContext = autoPopulatedContext;
    }

    /**
     * Enumeration for severity levels
     */
    public enum Severity {
        MINOR("Minor"), MAJOR("Major"), CRITICAL("Critical");

        private final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Enumeration for sample actions
     */
    public enum SampleAction {
        CONTINUE_WITH_FLAG("Continue with flag"), REJECT_SAMPLE("Reject sample");

        private final String displayName;

        SampleAction(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "InlineNCERequestForm{" + "category='" + category + '\'' + ", subcategory='" + subcategory + '\''
                + ", severity='" + severity + '\'' + ", title='" + title + '\'' + ", description='" + description + '\''
                + ", immediateAction='" + immediateAction + '\'' + ", sampleAction='" + sampleAction + '\'' + '}';
    }
}