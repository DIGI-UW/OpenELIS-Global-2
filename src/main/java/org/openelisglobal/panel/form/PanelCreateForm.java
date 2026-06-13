package org.openelisglobal.panel.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;

public class PanelCreateForm extends BaseForm {

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String code;

    @Size(max = 20)
    private String loincCode;

    @Size(max = 60)
    private String description;

    @NotEmpty(message = "At least one lab unit is required")
    private List<String> labUnitIds;

    private List<String> sampleTypeIds;

    private boolean active = true;

    public PanelCreateForm() {
        setFormName("panelCreateForm");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getLabUnitIds() {
        return labUnitIds;
    }

    public void setLabUnitIds(List<String> labUnitIds) {
        this.labUnitIds = labUnitIds;
    }

    public List<String> getSampleTypeIds() {
        return sampleTypeIds;
    }

    public void setSampleTypeIds(List<String> sampleTypeIds) {
        this.sampleTypeIds = sampleTypeIds;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
