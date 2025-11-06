package org.openelisglobal.testconfiguration.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.SafeHtml;

public class TestModifyEntryForm extends BaseForm {
    // for display
    private List testList;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameEnglish = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String nameFrench = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reportNameEnglish = "";

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String reportNameFrench = "";

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String testId = "";

    @Pattern(regexp = "^[0-9-]*$")
    private String loinc;

    // additional in validator
    @NotBlank
    private String jsonWad = "";

    // for display
    private List sampleTypeList;

    // for display
    private List panelList;

    // for display
    private List uomList;

    // for display
    private List resultTypeList;

    // for display
    private List ageRangeList;

    // for display
    private List labUnitList;

    // for display
    private List dictionaryList;

    // for display
    private List groupedDictionaryList;

    // for display
    private List testCatBeanList;

    public TestModifyEntryForm() {
        setFormName("testModifyEntryForm");
    }

    public List getTestList() {
        return testList;
    }

    public void setTestList(List testList) {
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

    public String getLoinc() {
        return loinc;
    }

    public void setLoinc(String loinc) {
        this.loinc = loinc;
    }

    public String getJsonWad() {
        return jsonWad;
    }

    public void setJsonWad(String jsonWad) {
        this.jsonWad = jsonWad;
    }

    public List getSampleTypeList() {
        return sampleTypeList;
    }

    public void setSampleTypeList(List sampleTypeList) {
        this.sampleTypeList = sampleTypeList;
    }

    public List getPanelList() {
        return panelList;
    }

    public void setPanelList(List panelList) {
        this.panelList = panelList;
    }

    public List getUomList() {
        return uomList;
    }

    public void setUomList(List uomList) {
        this.uomList = uomList;
    }

    public List getResultTypeList() {
        return resultTypeList;
    }

    public void setResultTypeList(List resultTypeList) {
        this.resultTypeList = resultTypeList;
    }

    public List getAgeRangeList() {
        return ageRangeList;
    }

    public void setAgeRangeList(List ageRangeList) {
        this.ageRangeList = ageRangeList;
    }

    public List getLabUnitList() {
        return labUnitList;
    }

    public void setLabUnitList(List labUnitList) {
        this.labUnitList = labUnitList;
    }

    public List getDictionaryList() {
        return dictionaryList;
    }

    public void setDictionaryList(List dictionaryList) {
        this.dictionaryList = dictionaryList;
    }

    public List getGroupedDictionaryList() {
        return groupedDictionaryList;
    }

    public void setGroupedDictionaryList(List groupedDictionaryList) {
        this.groupedDictionaryList = groupedDictionaryList;
    }

    public List getTestCatBeanList() {
        return testCatBeanList;
    }

    public void setTestCatBeanList(List testCatBeanList) {
        this.testCatBeanList = testCatBeanList;
    }
}
