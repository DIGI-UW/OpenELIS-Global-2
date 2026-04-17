package org.openelisglobal.testreflex.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestReflexService extends BaseObjectService<TestReflex, String> {
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getData(TestReflex testReflex);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestReflex> getPageOfTestReflexs(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexesByTestResult(TestResult testResult);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexsByTestAndFlag(String testId, String flag);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalTestReflexCount();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<TestReflex> getAllTestReflexs();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean isReflexedTest(Analysis analysis);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getFlaggedTestReflexesByTestResult(TestResult testResult, String flag);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexesByTestResultAndTestAnalyte(TestResult testResult, TestAnalyte testAnalyte);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexsByTestResultAnalyteTest(String testResultId, String analyteId, String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexsByAnalyteAndTest(String analyteId, String testId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void saveOrUpdateReflexRule(ReflexRule reflexRule);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<ReflexRule> getAllReflexRules();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    /**
     * Flip the rule's Active flag to {@code false}. Returns {@code true} when a
     * rule with the given id existed and was updated; {@code false} when no such
     * rule was found.
     */
    boolean deactivateReflexRule(String id);

    /**
     * Flip the rule's Active flag to {@code true}. Returns {@code true} when a rule
     * with the given id existed and was updated; {@code false} when no such rule
     * was found.
     */
    boolean activateReflexRule(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ReflexRule getReflexRuleByAnalyteId(String analyteId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId);
}
