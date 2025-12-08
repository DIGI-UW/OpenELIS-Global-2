package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.test.beanItems.TestActivationBean;

public class TestActivationForm extends BaseForm {
    // for display
    private List<TestActivationBean> activeTestList;

    // for display
    private List<TestActivationBean> inactiveTestList;

    // additional in validator
    @NotBlank
    private String jsonChangeList = "";

    public TestActivationForm() {
        setFormName("testActivationForm");
    }

    public List<TestActivationBean> getActiveTestList() {
        return activeTestList;
    }

    public void setActiveTestList(List<TestActivationBean> activeTestList) {
        this.activeTestList = activeTestList;
    }

    public List<TestActivationBean> getInactiveTestList() {
        return inactiveTestList;
    }

    public void setInactiveTestList(List<TestActivationBean> inactiveTestList) {
        this.inactiveTestList = inactiveTestList;
    }

    public String getJsonChangeList() {
        return jsonChangeList;
    }

    public void setJsonChangeList(String jsonChangeList) {
        this.jsonChangeList = jsonChangeList;
    }
}
