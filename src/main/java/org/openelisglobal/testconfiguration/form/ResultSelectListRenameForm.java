package org.openelisglobal.testconfiguration.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;

public class ResultSelectListRenameForm extends BaseForm {

    private List resultSelectOptionList;

    private String nameEnglish = "";

    private String nameFrench = "";

    private String resultSelectOptionId = "";

    public List getResultSelectOptionList() {
        return resultSelectOptionList;
    }

    public void setResultSelectOptionList(List resultSelectOptionList) {
        this.resultSelectOptionList = resultSelectOptionList;
    }

    public String getNameEnglish() {
        return nameEnglish;
    }

    public void setNameEnglish(String nameEnglish) {
        this.nameEnglish = nameEnglish;
    }

    public String getNameFrench() {
        return nameFrench;
    }

    public void setNameFrench(String nameFrench) {
        this.nameFrench = nameFrench;
    }

    public String getResultSelectOptionId() {
        return resultSelectOptionId;
    }

    public void setResultSelectOptionId(String resultSelectOptionId) {
        this.resultSelectOptionId = resultSelectOptionId;
    }
}
