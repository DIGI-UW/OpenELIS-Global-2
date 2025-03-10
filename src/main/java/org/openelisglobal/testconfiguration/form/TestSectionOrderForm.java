package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;

public class TestSectionOrderForm extends BaseForm {
    // for display
    private List testSectionList;

    // additional in validator
    @NotBlank
    private String jsonChangeList = "";

    public TestSectionOrderForm() {
        setFormName("testSectionOrderForm");
    }

    public List getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List testSectionList) {
        this.testSectionList = testSectionList;
    }

    public String getJsonChangeList() {
        return jsonChangeList;
    }

    public void setJsonChangeList(String jsonChangeList) {
        this.jsonChangeList = jsonChangeList;
    }
}
