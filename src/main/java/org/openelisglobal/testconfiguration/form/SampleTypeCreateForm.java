package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.validation.annotations.SafeHtml;

public class SampleTypeCreateForm extends BaseForm {
    // for display
    private List existingSampleTypeList;

    // for display
    private List inactiveSampleTypeList;

    // for display
    private String existingEnglishNames;

    // for display
    private String existingFrenchNames;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeEnglishName;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleTypeFrenchName;

    public SampleTypeCreateForm() {
        setFormName("sampleTypeCreateForm");
    }

    public List getExistingSampleTypeList() {
        return existingSampleTypeList;
    }

    public void setExistingSampleTypeList(List existingSampleTypeList) {
        this.existingSampleTypeList = existingSampleTypeList;
    }

    public List getInactiveSampleTypeList() {
        return inactiveSampleTypeList;
    }

    public void setInactiveSampleTypeList(List inactiveSampleTypeList) {
        this.inactiveSampleTypeList = inactiveSampleTypeList;
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

    public String getSampleTypeEnglishName() {
        return sampleTypeEnglishName;
    }

    public void setSampleTypeEnglishName(String sampleTypeEnglishName) {
        this.sampleTypeEnglishName = sampleTypeEnglishName;
    }

    public String getSampleTypeFrenchName() {
        return sampleTypeFrenchName;
    }

    public void setSampleTypeFrenchName(String sampleTypeFrenchName) {
        this.sampleTypeFrenchName = sampleTypeFrenchName;
    }
}
