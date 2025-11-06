package org.openelisglobal.testconfiguration.form;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.test.valueholder.Test;

public class ResultSelectListForm extends BaseForm {

    private String nameEnglish;
    private String nameFrench;
    private String loincCode;

    private boolean normal;

    private boolean qualifiable;

    private String page;

    private List<Test> tests;

    private String testSelectListJson;

    private Map<String, List<IdValuePair>> testDictionary;

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

    public boolean isNormal() {
        return normal;
    }

    public void setNormal(boolean normal) {
        this.normal = normal;
    }

    public boolean isQualifiable() {
        return qualifiable;
    }

    public void setQualifiable(boolean qualifiable) {
        this.qualifiable = qualifiable;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public List<Test> getTests() {
        return tests;
    }

    public void setTests(List<Test> tests) {
        this.tests = tests;
    }

    public String getTestSelectListJson() {
        return testSelectListJson;
    }

    public void setTestSelectListJson(String testSelectListJson) {
        this.testSelectListJson = testSelectListJson;
    }

    public Map<String, List<IdValuePair>> getTestDictionary() {
        return testDictionary;
    }

    public void setTestDictionary(Map<String, List<IdValuePair>> testDictionary) {
        this.testDictionary = testDictionary;
    }

    public String getLoincCode() {
        return loincCode;
    }

    public void setLoincCode(String loincCode) {
        this.loincCode = loincCode;
    }
}
