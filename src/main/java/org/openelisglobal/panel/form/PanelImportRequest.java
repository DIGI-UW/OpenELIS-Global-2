package org.openelisglobal.panel.form;

import org.openelisglobal.common.form.BaseForm;

/**
 * Request form for panel import.
 */
public class PanelImportRequest extends BaseForm {
    private String mode; // "create", "update", or "both"
    private Object data; // JSON structure containing panel data

    public PanelImportRequest() {
        setFormName("panelImportRequest");
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
