package org.openelisglobal.testresult.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;

public interface TestResultService extends BaseObjectService<TestResult, String> {
    void getData(TestResult testResult);

    TestResult getTestResultById(TestResult testResult);

    List<TestResult> getAllActiveTestResultsPerTest(Test test);

    List<TestResult> getActiveTestResultsByTest(String testId);

    List<TestResult> getPageOfTestResults(int startingRecNo);

    List<TestResult> getAllTestResults();

    TestResult getTestResultsByTestAndDictonaryResult(String testId, String result);

    List<TestResult> getTestResultsByTestAndResultGroup(TestAnalyte testAnalyte);

    List<TestResult> getAllSortedTestResults();

    /**
     * OGC-949 M5 / OGC-964 — active select-list options for a result component,
     * ordered by sort order. Options are TEST_RESULT rows scoped by component_id.
     */
    List<TestResult> getActiveOptionsByComponentId(String componentId);

    /**
     * Reconciles a component's active select-list options to the desired set:
     * update by id, insert new (sequence-assigned id, FK to the given test),
     * soft-delete (is_active=false) those omitted. Returns the resulting active
     * list. {@code test} must be the persistent Test (its id fills
     * TEST_RESULT.TEST_ID).
     */
    List<TestResult> saveOptionsForComponent(Test test, String componentId, List<TestResult> desired, String sysUserId);

}
