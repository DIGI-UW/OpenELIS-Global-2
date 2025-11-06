package org.openelisglobal.testconfiguration.form;

import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestCatalog;

public class TestCatalogForm extends BaseForm {

    // for display
    private List<Test> testList;

    // for display
    private List<String> testSectionList;

    // for display
    private List<TestCatalog> testCatalogList;

    public TestCatalogForm() {
        setFormName("testCatalogForm");
    }

    public List<Test> getTestList() {
        return testList;
    }

    public void setTestList(List<Test> testList) {
        this.testList = testList;
    }

    public List<String> getTestSectionList() {
        return testSectionList;
    }

    public void setTestSectionList(List<String> testSectionList) {
        this.testSectionList = testSectionList;
    }

    public List<TestCatalog> getTestCatalogList() {
        return testCatalogList;
    }

    public void setTestCatalogList(List<TestCatalog> testCatalogList) {
        this.testCatalogList = testCatalogList;
    }
}
