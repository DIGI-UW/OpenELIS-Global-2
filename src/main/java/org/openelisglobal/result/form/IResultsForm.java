package org.openelisglobal.result.form;

import java.util.List;
import org.openelisglobal.test.beanItems.TestResultItem;

public interface IResultsForm {

    void setTestResult(List<TestResultItem> resultPage);

    List<TestResultItem> getTestResult();

    void setTestSectionId(String string);

    String getTestSectionId();
}
