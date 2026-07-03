package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;

public class TestSectionEditForm extends BaseForm {

    // for display — list of all active test sections
    private List<IdValuePair> testSectionList;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testSectionId = "";

    // read-only display fields (not posted back)
    private String nameEnglish = "";
    private String nameFrench = "";

    @NotBlank
    @Pattern(regexp = "CLINICAL|ENVIRONMENTAL|VECTOR")
    private String domain = "";

    public TestSectionEditForm() {
        setFormName("testSectionEditForm");
    }

    public List<IdValuePair> getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List<IdValuePair> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
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

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
