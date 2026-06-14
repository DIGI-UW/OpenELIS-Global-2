package org.openelisglobal.testresultcomponent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

public interface TestResultComponentService extends BaseObjectService<TestResultComponent, String> {

    List<TestResultComponent> getComponentsByTestId(String testId);

    List<TestResultComponent> getActiveComponentsByTestId(String testId);

    TestResultComponent getByTestIdAndCode(String testId, String code);
}
