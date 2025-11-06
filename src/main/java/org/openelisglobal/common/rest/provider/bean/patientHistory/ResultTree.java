package org.openelisglobal.common.rest.provider.bean.patientHistory;

import java.util.List;

public class ResultTree {

    String display;

    List<PanelDisplay> subSets;

    public String getDisplay() {
        return display;
    }

    public List<PanelDisplay> getSubSets() {
        return subSets;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setSubSets(List<PanelDisplay> subSets) {
        this.subSets = subSets;
    }
}
