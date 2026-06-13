package org.openelisglobal.panel.form;

import jakarta.validation.constraints.NotBlank;
import org.openelisglobal.common.form.BaseForm;

/**
 * Request body for adding a single test to a panel.
 */
public class PanelAddTestRequest extends BaseForm {

    @NotBlank
    private String testId;

    private Integer displayOrder;

    private String panelLoincCode;

    public PanelAddTestRequest() {
        setFormName("panelAddTestRequest");
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getPanelLoincCode() {
        return panelLoincCode;
    }

    public void setPanelLoincCode(String panelLoincCode) {
        this.panelLoincCode = panelLoincCode;
    }
}
