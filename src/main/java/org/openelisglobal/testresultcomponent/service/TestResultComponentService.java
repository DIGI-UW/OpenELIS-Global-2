package org.openelisglobal.testresultcomponent.service;

import java.util.List;
import java.util.Map;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;

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

    /**
     * Atomically persists the Sample &amp; Results config: reconciles the test's
     * components (insert/update/soft-delete), then per component (keyed by code)
     * reconciles its interpretations — all in one transaction. Returns the active
     * components.
     */
    List<TestResultComponent> saveSampleResults(String testId, List<TestResultComponent> components,
            Map<String, List<TestResultInterpretation>> interpretationsByComponentCode, String sysUserId);
}
