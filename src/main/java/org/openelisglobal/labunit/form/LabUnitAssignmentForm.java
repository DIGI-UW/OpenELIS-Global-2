package org.openelisglobal.labunit.form;

import jakarta.validation.constraints.NotEmpty;
import org.openelisglobal.common.form.BaseForm;

public class LabUnitAssignmentForm extends BaseForm {

    @NotEmpty(message = "labunit.assignment.items.required")
    private String[] itemIds;

    private String targetLabUnitId;

    public LabUnitAssignmentForm() {
        setFormName("labUnitAssignmentForm");
    }

    public String[] getItemIds() {
        return itemIds;
    }

    public void setItemIds(String[] itemIds) {
        this.itemIds = itemIds;
    }

    public String getTargetLabUnitId() {
        return targetLabUnitId;
    }

    public void setTargetLabUnitId(String targetLabUnitId) {
        this.targetLabUnitId = targetLabUnitId;
    }
}