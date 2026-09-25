package org.openelisglobal.testresult.service;

import java.util.List;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.security.access.prepost.PreAuthorize;

// Despite the name, TEST_RESULT rows are catalogue configuration: the POSSIBLE
// values a test can report, select-list options, dictionary mappings, sort
// order, not any patient's result. Order entry reads them for every test it
// offers (TestService.getResultType -> getPossibleTestResults), so the gate also
// accepts PRIV_CATALOGUE_VIEW. Patient results live in RESULT and stay on
// PRIV_RESULT_VIEW via ResultService.
@PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
// Writes are pinned separately: without this, the widened read gate above would
// also govern the inherited insert/update/delete, letting any catalogue reader
// edit a test's possible values. Those writes come from the test-catalogue
// editor (TestResultComponentServiceImpl), which is PRIV_TEST_CONFIGURE work.
@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestResultService extends BaseObjectService<TestResult, String> {
    void getData(TestResult testResult);

    TestResult getTestResultById(TestResult testResult);

    List<TestResult> getAllActiveTestResultsPerTest(Test test);

    List<TestResult> getActiveTestResultsByTest(String testId);

    List<TestResult> getPageOfTestResults(int startingRecNo);

    List<TestResult> getAllTestResults();

    TestResult getTestResultsByTestAndDictonaryResult(String testId, String result);

    /**
     * The option row for a dictionary result id on one result component, falling
     * back to the test-wide lookup when the component is blank or owns no such row.
     */
    TestResult getTestResultsByTestAndDictonaryResult(String testId, String result, String componentId);

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
     *
     * <p>
     * A catalogue WRITE, pinned so it does not inherit the interface's read gate,
     * which accepts PRIV_CATALOGUE_VIEW.
     */
    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestResult> saveOptionsForComponent(Test test, String componentId, List<TestResult> desired, String sysUserId);

}
