package org.openelisglobal.panel.form;

import java.util.List;
import java.util.Map;

public class PanelImportPreviewResponse {
    private List<PanelImportPreviewItem> preview;
    private Map<String, Long> counts;

    public List<PanelImportPreviewItem> getPreview() {
        return preview;
    }

    public void setPreview(List<PanelImportPreviewItem> preview) {
        this.preview = preview;
    }

    public Map<String, Long> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }
}
