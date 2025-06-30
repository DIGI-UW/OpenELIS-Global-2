package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;

public class PanelOrderForm extends BaseForm {
    // for display
    private List panelList;

    // additional in validator
    @NotBlank
    private String jsonChangeList = "";

    // for display
    private List existingPanelList;

    // for display
    private List inactivePanelList;

    // for display
    private List existingSampleTypeList;

    public PanelOrderForm() {
        setFormName("panelOrderForm");
    }

    public List getPanelList() {
        return panelList;
    }

    public void setPanelList(List panelList) {
        this.panelList = panelList;
    }

    public String getJsonChangeList() {
        return jsonChangeList;
    }

    public void setJsonChangeList(String jsonChangeList) {
        this.jsonChangeList = jsonChangeList;
    }

    public List getExistingPanelList() {
        return existingPanelList;
    }

    public void setExistingPanelList(List existingPanelList) {
        this.existingPanelList = existingPanelList;
    }

    public List getInactivePanelList() {
        return inactivePanelList;
    }

    public void setInactivePanelList(List inactivePanelList) {
        this.inactivePanelList = inactivePanelList;
    }

    public List getExistingSampleTypeList() {
        return existingSampleTypeList;
    }

    public void setExistingSampleTypeList(List existingSampleTypeList) {
        this.existingSampleTypeList = existingSampleTypeList;
    }
}
