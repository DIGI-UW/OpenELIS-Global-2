package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class TestRenameEntryForm extends BaseForm {
    // for display
    private List<IdValuePair> testList;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameEnglish = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameFrench = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reportNameEnglish = "";

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reportNameFrench = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId = "";

    public TestRenameEntryForm() {
        setFormName("testRenameEntryForm");
    }

    public List<IdValuePair> getTestList() {
        return testList;
    }

    public void setTestList(List<IdValuePair> testList) {
        this.testList = testList;
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

    public String getReportNameEnglish() {
        return reportNameEnglish;
    }

    public void setReportNameEnglish(String reportNameEnglish) {
        this.reportNameEnglish = reportNameEnglish;
    }

    public String getReportNameFrench() {
        return reportNameFrench;
    }

    public void setReportNameFrench(String reportNameFrench) {
        this.reportNameFrench = reportNameFrench;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }
}
