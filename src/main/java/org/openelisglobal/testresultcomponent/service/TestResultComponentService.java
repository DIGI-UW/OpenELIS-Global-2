package org.openelisglobal.testresultcomponent.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;

public interface TestResultComponentService extends BaseObjectService<TestResultComponent, String> {

    List<TestResultComponent> getComponentsByTestId(String testId);

    List<TestResultComponent> getActiveComponentsByTestId(String testId);

    TestResultComponent getByTestIdAndCode(String testId, String code);

    /**
     * Reconciles a test's active components to the desired set: a desired component
     * whose id matches an existing active row is updated in place; one without a
     * matching id is inserted; an existing active component absent from the desired
     * set is soft-deleted (is_active='N'). Returns the resulting active list.
     */
    List<TestResultComponent> saveComponentsForTest(String testId, List<TestResultComponent> desired, String sysUserId);
}
