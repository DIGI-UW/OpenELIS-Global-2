package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class UomRenameEntryForm extends BaseForm {
    // for display
    private List uomList;

    @NotBlank
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameEnglish = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameFrench = "";

    @NotBlank
    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String uomId = "";

    public UomRenameEntryForm() {
        setFormName("uomRenameEntryForm");
    }

    public List getUomList() {
        return uomList;
    }

    public void setUomList(List uomList) {
        this.uomList = uomList;
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

    public String getUomId() {
        return uomId;
    }

    public void setUomId(String uomId) {
        this.uomId = uomId;
    }
}
