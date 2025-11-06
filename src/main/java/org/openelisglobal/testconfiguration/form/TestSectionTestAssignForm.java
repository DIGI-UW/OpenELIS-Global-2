package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;

public class TestSectionTestAssignForm extends BaseForm {
    // for display
    private List testSectionList;

    // for display
    private LinkedHashMap sectionTestList;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testSectionId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String deactivateTestSectionId = "";

    public TestSectionTestAssignForm() {
        setFormName("testSectionTestAssignForm");
    }

    public List getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List testSectionList) {
        this.testSectionList = testSectionList;
    }

    public LinkedHashMap getSectionTestList() {
        return sectionTestList;
    }

    public void setSectionTestList(LinkedHashMap sectionTestList) {
        this.sectionTestList = sectionTestList;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getDeactivateTestSectionId() {
        return deactivateTestSectionId;
    }

    public void setDeactivateTestSectionId(String deactivateTestSectionId) {
        this.deactivateTestSectionId = deactivateTestSectionId;
    }
}
