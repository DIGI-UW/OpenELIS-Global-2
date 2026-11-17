package org.openelisglobal.siteinformation.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.validation.annotations.SafeHtml;

public class SiteInformationForm extends BaseForm {
    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String paramName = "";

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String description = "";

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String value = "";

    // what about this is encrypted?
    private boolean encrypted;

    // in validator
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String valueType = "text";

    // in validator
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String siteInfoDomainName;

    // for display
    private List<String> dictionaryValues;

    @NotNull
    private Boolean editable = Boolean.TRUE;

    // in validator
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String tag = "";

    @Pattern(regexp = ValidationHelper.MESSAGE_KEY_REGEX)
    private String descriptionKey = "";

    // Settings accept translations only through values; these aliases are
    // display-only.
    @JsonIgnoreProperties(value = { "english", "french", "localizedValue" }, allowGetters = true)
    @Valid
    private Localization localization;

    // OGC-1364 v3: the version the page loaded, so a save from an older page is
    // refused instead of overwriting a newer value.
    private Long lastupdated;

    public Long getLastupdated() {
        return lastupdated;
    }

    public void setLastupdated(Long lastupdated) {
        this.lastupdated = lastupdated;
    }

    public SiteInformationForm() {
        setFormName("siteInformationForm");
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getSiteInfoDomainName() {
        return siteInfoDomainName;
    }

    public void setSiteInfoDomainName(String siteInfoDomainName) {
        this.siteInfoDomainName = siteInfoDomainName;
    }

    public List<String> getDictionaryValues() {
        return dictionaryValues;
    }

    public void setDictionaryValues(List<String> dictionaryValues) {
        this.dictionaryValues = dictionaryValues;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public void setDescriptionKey(String descriptionKey) {
        this.descriptionKey = descriptionKey;
    }

    public void setLocalization(Localization localization) {
        this.localization = localization;
    }

    public Localization getLocalization() {
        return this.localization;
    }
}
