package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.validation.annotations.SafeHtml;

public class MethodCreateForm extends BaseForm {
    // for display
    private List existingMethodList;

    // for display
    private List inactiveMethodList;

    // for display
    private String existingEnglishNames;

    // for display
    private String existingFrenchNames;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String methodEnglishName;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String methodFrenchName;

    public MethodCreateForm() {
        setFormName("methodCreateForm");
    }

    public List getExistingMethodList() {
        return existingMethodList;
    }

    public void setExistingMethodList(List<IdValuePair> existingMethodList) {
        this.existingMethodList = existingMethodList;
    }

    public List getInactiveMethodList() {
        return inactiveMethodList;
    }

    public void setInactiveMethodList(List inactiveMethodList) {
        this.inactiveMethodList = inactiveMethodList;
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

    public String getMethodEnglishName() {
        return methodEnglishName;
    }

    public void setMethodEnglishName(String methodEnglishName) {
        this.methodEnglishName = methodEnglishName;
    }

    public String getMethodFrenchName() {
        return methodFrenchName;
    }

    public void setMethodFrenchName(String methodFrenchName) {
        this.methodFrenchName = methodFrenchName;
    }
}
