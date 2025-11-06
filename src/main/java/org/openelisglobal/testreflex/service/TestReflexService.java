package org.openelisglobal.testreflex.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;

public interface TestReflexService extends BaseObjectService<TestReflex, String> {
    void getData(TestReflex testReflex);

    List<TestReflex> getPageOfTestReflexs(int startingRecNo);

    List<TestReflex> getTestReflexesByTestResult(TestResult testResult);

    List<TestReflex> getTestReflexsByTestAndFlag(String testId, String flag);

    Integer getTotalTestReflexCount();

    List<TestReflex> getAllTestReflexs();

    boolean isReflexedTest(Analysis analysis);

    List<TestReflex> getFlaggedTestReflexesByTestResult(TestResult testResult, String flag);

    List<TestReflex> getTestReflexesByTestResultAndTestAnalyte(TestResult testResult, TestAnalyte testAnalyte);

    List<TestReflex> getTestReflexsByTestResultAnalyteTest(String testResultId, String analyteId, String testId);

    List<TestReflex> getTestReflexsByAnalyteAndTest(String analyteId, String testId);

    void saveOrUpdateReflexRule(ReflexRule reflexRule);

    List<ReflexRule> getAllReflexRules();

    void deactivateReflexRule(String id);

    ReflexRule getReflexRuleByAnalyteId(String analyteId);

    List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId);
}
