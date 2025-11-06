package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;

public class SampleTypeTestAssignForm extends BaseForm {
    // for display
    private List sampleTypeList;

    // for display
    private LinkedHashMap sampleTypeTestList;

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String sampleTypeId = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String deactivateSampleTypeId = "";

    public SampleTypeTestAssignForm() {
        setFormName("sampleTypeTestAssignForm");
    }

    public List getSampleTypeList() {
        return sampleTypeList;
    }

    public void setSampleTypeList(List sampleTypeList) {
        this.sampleTypeList = sampleTypeList;
    }

    public LinkedHashMap getSampleTypeTestList() {
        return sampleTypeTestList;
    }

    public void setSampleTypeTestList(LinkedHashMap sampleTypeTestList) {
        this.sampleTypeTestList = sampleTypeTestList;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getDeactivateSampleTypeId() {
        return deactivateSampleTypeId;
    }

    public void setDeactivateSampleTypeId(String deactivateSampleTypeId) {
        this.deactivateSampleTypeId = deactivateSampleTypeId;
    }
}
