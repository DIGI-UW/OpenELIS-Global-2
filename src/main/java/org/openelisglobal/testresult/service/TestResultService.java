package org.openelisglobal.testresult.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
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

}
