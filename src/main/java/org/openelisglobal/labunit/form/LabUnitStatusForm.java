package org.openelisglobal.labunit.form;

import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.form.BaseForm;

public class LabUnitStatusForm extends BaseForm {

    @NotNull(message = "labunit.status.cascade.required")
    private Boolean cascade = false;

    private String reason;

    public LabUnitStatusForm() {
        setFormName("labUnitStatusForm");
    }

    public Boolean getCascade() {
        return cascade;
    }

    public void setCascade(Boolean cascade) {
        this.cascade = cascade;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}