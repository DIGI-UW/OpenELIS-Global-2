package org.openelisglobal.admin.form;

import org.hibernate.validator.constraints.Length;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.validation.annotations.SafeHtml;

public class LabNumberManagementForm {

    private AccessionFormat labNumberType;

    private Boolean usePrefix;

    @Length(max = 5, min = 0)
    @SafeHtml
    private String alphanumPrefix;

    @SafeHtml
    private String customAccessionRegex;

    @SafeHtml
    private String customAccessionTemplate;

    public AccessionFormat getLabNumberType() {
        return labNumberType;
    }

    public void setLabNumberType(AccessionFormat labNumberType) {
        this.labNumberType = labNumberType;
    }

    public Boolean getUsePrefix() {
        return usePrefix;
    }

    public void setUsePrefix(Boolean usePrefix) {
        this.usePrefix = usePrefix;
    }

    public String getAlphanumPrefix() {
        return alphanumPrefix;
    }

    public void setAlphanumPrefix(String alphanumPrefix) {
        this.alphanumPrefix = alphanumPrefix;
    }

    public String getCustomAccessionRegex() {
        return customAccessionRegex;
    }

    public void setCustomAccessionRegex(String customAccessionRegex) {
        this.customAccessionRegex = customAccessionRegex;
    }

    public String getCustomAccessionTemplate() {
        return customAccessionTemplate;
    }

    public void setCustomAccessionTemplate(String customAccessionTemplate) {
        this.customAccessionTemplate = customAccessionTemplate;
    }
}
