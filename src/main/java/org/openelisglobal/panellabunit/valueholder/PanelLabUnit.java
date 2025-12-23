package org.openelisglobal.panellabunit.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class PanelLabUnit extends BaseObject<String> {

    private String id;
    private String panelId;
    private String labUnitId;

    public PanelLabUnit() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public String getLabUnitId() {
        return labUnitId;
    }

    public void setLabUnitId(String labUnitId) {
        this.labUnitId = labUnitId;
    }
}
