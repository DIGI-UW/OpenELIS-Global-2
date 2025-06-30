package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.testconfiguration.action.PanelTests;

public class PanelTestAssignForm extends BaseForm {
    // for display
    private List panelList;

    // for display
    private PanelTests selectedPanel = new PanelTests();

    // for display
    private List panelTestList;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String panelId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String deactivatePanelId = "";

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> currentTests;

    private List<@Pattern(regexp = ValidationHelper.ID_REGEX) String> availableTests;

    public PanelTestAssignForm() {
        setFormName("panelTestAssignForm");
    }

    public List getPanelList() {
        return panelList;
    }

    public void setPanelList(List panelList) {
        this.panelList = panelList;
    }

    public PanelTests getSelectedPanel() {
        return selectedPanel;
    }

    public void setSelectedPanel(PanelTests selectedPanel) {
        this.selectedPanel = selectedPanel;
    }

    public List getPanelTestList() {
        return panelTestList;
    }

    public void setPanelTestList(List panelTestList) {
        this.panelTestList = panelTestList;
    }

    public String getPanelId() {
        return panelId;
    }

    public void setPanelId(String panelId) {
        this.panelId = panelId;
    }

    public String getDeactivatePanelId() {
        return deactivatePanelId;
    }

    public void setDeactivatePanelId(String deactivatePanelId) {
        this.deactivatePanelId = deactivatePanelId;
    }

    public List<String> getCurrentTests() {
        return currentTests;
    }

    public void setCurrentTests(List<String> currentTests) {
        this.currentTests = currentTests;
    }

    public List<String> getAvailableTests() {
        return availableTests;
    }

    public void setAvailableTests(List<String> availableTests) {
        this.availableTests = availableTests;
    }
}
