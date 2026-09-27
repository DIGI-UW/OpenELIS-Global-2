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

    ReflexRule getReflexRuleByAnalyteId(String analyteId);

    /**
     * Whether the rule that owns this executable row is switched on right now.
     *
     * <p>
     * Deactivating a rule is meant to delete its rows and reactivating to write
     * them back, so row presence looks like the switch — but saving a rule rebuilds
     * its rows whatever its state, so a deactivated rule can keep a full set of
     * them and go on firing. The flag on the rule is the switch, and it is asked at
     * execution because it can change between the row being written and the next
     * result arriving.
     *
     * <p>
     * A row with no rule behind it — a reflex seeded before the rule builder
     * existed, or one the user-choice path constructs in memory — has no flag to
     * consult and is treated as active, which is how it behaves today.
     */
    boolean isReflexRuleActive(TestReflex reflex);

    List<TestReflex> getTestReflexsByTestAnalyteId(String testAnalyteId);

    List<TestReflex> getTestReflexsByTestId(String testId);
}
