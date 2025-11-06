package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class TestSectionRenameEntryForm extends BaseForm {
    // for display
    private List testSectionList;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameEnglish = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameFrench = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testSectionId = "";

    public TestSectionRenameEntryForm() {
        setFormName("testSectionRenameEntryForm");
    }

    public List getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List testSectionList) {
        this.testSectionList = testSectionList;
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

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }
}
