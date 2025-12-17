package org.openelisglobal.labunit.form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.openelisglobal.common.form.BaseForm;

public class LabUnitOrderForm extends BaseForm {

    @NotEmpty(message = "labunit.order.items.required")
    private LabUnitOrderItem[] items;

    public LabUnitOrderForm() {
        setFormName("labUnitOrderForm");
    }

    public LabUnitOrderItem[] getItems() {
        return items;
    }

    public void setItems(LabUnitOrderItem[] items) {
        this.items = items;
    }

    public static class LabUnitOrderItem {
        @NotNull(message = "labunit.order.id.required")
        private String id;

        @NotNull(message = "labunit.order.sortorder.required")
        private Integer sortOrder;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}