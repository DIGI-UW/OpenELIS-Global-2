package org.openelisglobal.panel.form;

import java.util.List;

public class PanelImportResponse {
    private int panelsCreated;
    private int panelsUpdated;
    private int panelsSkipped;
    private List<String> warnings;

    public int getPanelsCreated() {
        return panelsCreated;
    }

    public void setPanelsCreated(int panelsCreated) {
        this.panelsCreated = panelsCreated;
    }

    public int getPanelsUpdated() {
        return panelsUpdated;
    }

    public void setPanelsUpdated(int panelsUpdated) {
        this.panelsUpdated = panelsUpdated;
    }

    public int getPanelsSkipped() {
        return panelsSkipped;
    }

    public void setPanelsSkipped(int panelsSkipped) {
        this.panelsSkipped = panelsSkipped;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
