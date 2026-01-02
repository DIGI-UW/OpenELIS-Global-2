package org.openelisglobal.panel.form;

import org.openelisglobal.common.form.BaseForm;

/**
 * Form representing a test within a panel, including order and panel-specific
 * LOINC.
 */
public class PanelTestForm extends BaseForm {
    private String testId;
    private String testName;
    private String testLoincCode; // Test's own LOINC (read-only reference)
    private String panelLoincCode; // Panel-specific LOINC (editable)
    private Integer displayOrder; // Sort order within panel

    public PanelTestForm() {
        setFormName("panelTestForm");
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestLoincCode() {
        return testLoincCode;
    }

    public void setTestLoincCode(String testLoincCode) {
        this.testLoincCode = testLoincCode;
    }

    public String getPanelLoincCode() {
        return panelLoincCode;
    }

    public void setPanelLoincCode(String panelLoincCode) {
        this.panelLoincCode = panelLoincCode;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
