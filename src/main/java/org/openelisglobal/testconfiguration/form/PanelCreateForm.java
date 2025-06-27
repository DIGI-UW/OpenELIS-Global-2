package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class PanelCreateForm extends BaseForm {
    // for display
    private List existingPanelList;

    // for display
    private List inactivePanelList;

    // for display
    private Map existingPanelMap;

    // for display
    private Map inactivePanelMap;

    // for display
    private List existingSampleTypeList;

    // for display
    private String existingEnglishNames;

    // for display
    private String existingFrenchNames;

    private String existingLoinc;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String panelEnglishName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String panelFrenchName;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sampleTypeId;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String panelLoinc;

    public PanelCreateForm() {
        setFormName("panelCreateForm");
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

    public Map getExistingPanelMap() {
        return existingPanelMap;
    }

    public void setExistingPanelMap(Map existingPanelMap) {
        this.existingPanelMap = existingPanelMap;
    }

    public Map getInactivePanelMap() {
        return inactivePanelMap;
    }

    public void setInactivePanelMap(Map inactivePanelMap) {
        this.inactivePanelMap = inactivePanelMap;
    }

    public List getExistingSampleTypeList() {
        return existingSampleTypeList;
    }

    public void setExistingSampleTypeList(List existingSampleTypeList) {
        this.existingSampleTypeList = existingSampleTypeList;
    }

    public String getExistingEnglishNames() {
        return existingEnglishNames;
    }

    public void setExistingEnglishNames(String existingEnglishNames) {
        this.existingEnglishNames = existingEnglishNames;
    }

    public String getExistingFrenchNames() {
        return existingFrenchNames;
    }

    public void setExistingFrenchNames(String existingFrenchNames) {
        this.existingFrenchNames = existingFrenchNames;
    }

    public String getPanelEnglishName() {
        return panelEnglishName;
    }

    public void setPanelEnglishName(String panelEnglishName) {
        this.panelEnglishName = panelEnglishName;
    }

    public String getPanelFrenchName() {
        return panelFrenchName;
    }

    public void setPanelFrenchName(String panelFrenchName) {
        this.panelFrenchName = panelFrenchName;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getExistingLoinc() {
        return existingLoinc;
    }

    public void setExistingLoinc(String existingLoinc) {
        this.existingLoinc = existingLoinc;
    }

    public String getPanelLoinc() {
        return panelLoinc;
    }

    public void setPanelLoinc(String panelLoinc) {
        this.panelLoinc = panelLoinc;
    }
}
