package org.openelisglobal.common.formfields;

public class FormField {

    private Boolean inUse;

    private boolean required;

    private String labelKey;

    private String label;

    public FormField() {
    }

    public FormField(String labelKey) {
        this.labelKey = labelKey;
    }

    public FormField(boolean inUse, String labelKey) {
        this.inUse = inUse;
        this.labelKey = labelKey;
    }

    public FormField(boolean inUse, String labelKey, boolean required) {
        this.inUse = inUse;
        this.labelKey = labelKey;
        this.required = required;
    }

    public FormField(boolean inUse) {
        this.inUse = inUse;
    }

    public Boolean getInUse() {
        return inUse;
    }

    public void setInUse(Boolean inUse) {
        this.inUse = inUse;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean getRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
