package org.openelisglobal.admin.form;

import org.hibernate.validator.constraints.Length;
import org.openelisglobal.common.provider.validation.AccessionNumberValidatorFactory.AccessionFormat;
import org.openelisglobal.validation.annotations.SafeHtml;
import org.openelisglobal.validation.annotations.ValidRegex;

public class LabNumberManagementForm {

    private AccessionFormat labNumberType;

    private Boolean usePrefix;

    @Length(max = 5, min = 0)
    @SafeHtml
    private String alphanumPrefix;

    @Length(max = 512)
    @ValidRegex
    private String alphanumRegex;

    @Length(max = 512)
    @ValidRegex
    private String siteYearnumRegex;

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

    public String getAlphanumRegex() {
        return alphanumRegex;
    }

    public void setAlphanumRegex(String alphanumRegex) {
        this.alphanumRegex = alphanumRegex;
    }

    public String getSiteYearnumRegex() {
        return siteYearnumRegex;
    }

    public void setSiteYearnumRegex(String siteYearnumRegex) {
        this.siteYearnumRegex = siteYearnumRegex;
    }
}
