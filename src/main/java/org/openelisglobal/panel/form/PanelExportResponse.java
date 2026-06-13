package org.openelisglobal.panel.form;

import java.util.List;

public class PanelExportResponse {
    private List<PanelForm> panels;
    private String exportedAt;
    private Integer count;
    private String csvData;

    public List<PanelForm> getPanels() {
        return panels;
    }

    public void setPanels(List<PanelForm> panels) {
        this.panels = panels;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(String exportedAt) {
        this.exportedAt = exportedAt;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getCsvData() {
        return csvData;
    }

    public void setCsvData(String csvData) {
        this.csvData = csvData;
    }
}
