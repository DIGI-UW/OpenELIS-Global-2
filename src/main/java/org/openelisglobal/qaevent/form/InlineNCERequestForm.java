package org.openelisglobal.qaevent.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Form class for inline NCE creation requests.
 * Maps to the InlineNCERequest schema in the OpenAPI specification.
 */
public class InlineNCERequestForm {

    @NotBlank(message = "Category is required")
    private String category = "Analytical";

    private String subcategory;

    @NotNull(message = "Severity is required")
    private String severity;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String immediateAction;

    @NotNull(message = "Sample action is required")
    private String sampleAction;

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
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

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
        CONTINUE_WITH_FLAG("Continue with flag"),
        REJECT_SAMPLE("Reject sample");

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
        return "InlineNCERequestForm{" +
                "category='" + category + '\'' +
                ", subcategory='" + subcategory + '\'' +
                ", severity='" + severity + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", immediateAction='" + immediateAction + '\'' +
                ", sampleAction='" + sampleAction + '\'' +
                '}';
    }
}