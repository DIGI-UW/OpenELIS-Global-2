package org.openelisglobal.qaevent.form;

import jakarta.validation.constraints.NotBlank;

/**
 * Request form for recording prompt dismissals. Maps to spec Section 9.3.
 */
public class PromptDismissalRequestForm {

    @NotBlank(message = "Trigger action is required")
    private String triggerAction;

    @NotBlank(message = "Source type is required")
    private String sourceType;

    private String resultId;

    private String context;

    public PromptDismissalRequestForm() {
    }

    public String getTriggerAction() {
        return triggerAction;
    }

    public void setTriggerAction(String triggerAction) {
        this.triggerAction = triggerAction;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
