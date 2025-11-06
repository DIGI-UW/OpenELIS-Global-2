package org.openelisglobal.common.rest.provider.bean.patientHistory;

import java.util.List;

public class PanelDisplay {

    String display;

    List<TestDisplay> subSets;

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public List<TestDisplay> getSubSets() {
        return subSets;
    }

    public void setSubSets(List<TestDisplay> subSets) {
        this.subSets = subSets;
    }
}
