package org.openelisglobal.panel.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;

public class PanelForm extends BaseForm {
    private String id;
    private String name;
    private String code;
    private String loincCode;
    private String description;
    private boolean active;
    private List<String> labUnitIds;
    private List<String> sampleTypeIds;
    private List<PanelTestForm> tests; // Tests in panel (when includeTests=true)

    public PanelForm() {
        setFormName("panelForm");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public List<PanelTestForm> getTests() {
        return tests;
    }

    public void setTests(List<PanelTestForm> tests) {
        this.tests = tests;
    }
}
