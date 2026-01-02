package org.openelisglobal.panel.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;

/**
 * Request form for panel export.
 */
public class PanelExportRequest extends BaseForm {
    private List<String> panelIds;
    private String format; // "json" or "csv"
    private boolean includeTests = true;

    public PanelExportRequest() {
        setFormName("panelExportRequest");
    }

    public List<String> getPanelIds() {
        return panelIds;
    }

    public void setPanelIds(List<String> panelIds) {
        this.panelIds = panelIds;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isIncludeTests() {
        return includeTests;
    }

    public void setIncludeTests(boolean includeTests) {
        this.includeTests = includeTests;
    }
}
