package org.openelisglobal.testreflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleCondition;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testreflex.service.ReflexRuleService;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflexServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService testReflexService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestAnalyteService testAnalyteService;
    @Autowired
    private ReflexRuleService reflexRuleService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void getData_ShouldReturnDataOfTheTestReflexPassedAsParameter() {
        TestReflex testReflex = testReflexService.get("1004");
        testReflexService.getData(testReflex);
        assertEquals("positive", testReflex.getFlags());
        assertEquals(Timestamp.valueOf("2025-07-13 08:45:00"), testReflex.getLastupdated());
        assertEquals("4001", testReflex.getTestId());
        assertEquals("7002", testReflex.getActionScriptletId());
    }

    @Test
    public void getPageOfTestReflexes_ShouldReturnAllTestReflexesOnAPage() {
        List<TestReflex> testReflexes = testReflexService.getPageOfTestReflexs(1);
        assertNotNull(testReflexes);
        assertEquals(4, testReflexes.size());
        assertEquals("positive", testReflexes.get(2).getFlags());
    }

    @Test
    public void getTestReflexesByTestResult_ShouldReturnAllTestReflexesWithTheParameterTestResult() {
        TestResult testResult = testResultService.get("2002");
        List<TestReflex> testReflexes = testReflexService.getTestReflexesByTestResult(testResult);
        assertNotNull(testReflexes);
        assertEquals(3, testReflexes.size());
        assertEquals("negative", testReflexes.get(0).getFlags());
    }

    @Test
    public void getTestReflexesByTestAndFlag_ShouldReturnTestReflexesWithTestIdAndFlagsPassedAsParameter() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestAndFlag("4001", "positive");
        assertNotNull(testReflexes);
        assertEquals(1, testReflexes.size());
        assertEquals(Timestamp.valueOf("2025-07-10 14:30:00.0"), testReflexes.get(0).getLastupdated());
    }

    @Test
    public void getTotalTestReflexCount_ShouldReturnNumberOfTestReflexesInTheDB() {
        Integer numberOfTestReflexes = testReflexService.getTotalTestReflexCount();
        assertEquals(Integer.valueOf("4"), numberOfTestReflexes);
    }

    @Test
    public void getAllTestReflexes_ShouldReturnAllTestReflexesInTheDB() {
        List<TestReflex> testReflexes = testReflexService.getAllTestReflexs();
        assertNotNull(testReflexes);
        assertEquals(4, testReflexes.size());
        assertEquals("1002", testReflexes.get(3).getId());
        assertEquals("pending", testReflexes.get(1).getFlags());
    }

    @Test
    public void isReflexedTest_ShouldReturnTrueIfTestReflexWasReflexedOrLinked() {
        Analysis analysis = analysisService.get("303");
        boolean isReflexedTest = testReflexService.isReflexedTest(analysis);
        assertTrue(isReflexedTest);
    }

    @Test
    public void getFlaggedTestReflexesByTestResult_ShouldReturnAllTestReflexesWithTestResultAndFlagValuesPassedAsParameter() {
        TestResult testResult = testResultService.get("2001");
        List<TestReflex> testReflexes = testReflexService.getFlaggedTestReflexesByTestResult(testResult, "positive");
        assertNotNull(testReflexes);
        assertEquals(1, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexesByTestResultAndTestAnalyte_ShouldReturnTestReflexesWithTestResultAndTestAnalyteValuesPassedAsParameter() {
        TestResult testResult = testResultService.get("2002");
        TestAnalyte testAnalyte = testAnalyteService.get("3001");
        List<TestReflex> testReflexes = testReflexService.getTestReflexesByTestResultAndTestAnalyte(testResult,
                testAnalyte);
        assertNotNull(testReflexes);
        assertEquals(2, testReflexes.size());
        assertEquals("1003", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByTestResultAnalyteTest_ShouldReturnTestReflexesWithTestResultIdAndAnalyteIdAndTestIdPassedAsParameters() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestResultAnalyteTest("2002", "701", "4001");
        assertNotNull(testReflexes);
        assertEquals(2, testReflexes.size());
        assertEquals("1003", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByAnalyteAndTest_ShouldReturnTestReflexesThatMatchAnalyteIdAndTestIdValuesPassedAsParameters() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByAnalyteAndTest("702", "4002");
        assertNotNull(testReflexes);
        assertEquals(1, testReflexes.size());
        assertEquals("1002", testReflexes.get(0).getId());
    }

    @Test
    public void saveOrUpdateReflexRule_TestUpdatingTheExistingReflexRule() {
        ReflexRule reflexRule = reflexRuleService.get(1402);
        assertEquals("Low WBC Alert", reflexRule.getRuleName());
        assertTrue(reflexRule.getActive());
        reflexRule.setRuleName("High Hemoglobin test Alert");
        reflexRule.setActive(false);
        testReflexService.saveOrUpdateReflexRule(reflexRule);
        assertEquals("High Hemoglobin test Alert", reflexRule.getRuleName());
        assertFalse(reflexRule.getActive());
    }

    @Test
    public void saveOrUpdateReflexRule_TestInsertingANewReflexRule() {
        ReflexRuleCondition condition = new ReflexRuleCondition();
        condition.setSampleId("4002");
        condition.setTestName("Urinalysis");
        condition.setTestId("4001");
        condition.setRelation(ReflexRuleOptions.NumericRelationOptions.GREATER_THAN);
        condition.setValue("Value1");
        List<ReflexRuleCondition> reflexRuleConditions = new ArrayList<>(Arrays.asList(condition));

        Set<ReflexRuleCondition> reflexRuleConditionSet = new HashSet<>(reflexRuleConditions);
        ReflexRule reflexRule = new ReflexRule();
        reflexRule.setRuleName("Endoscopy rule");
        reflexRule.setOverall(ReflexRuleOptions.OverallOptions.ANY);
        reflexRule.setToggled(true);
        reflexRule.setActive(true);
        reflexRule.setLastupdated(Timestamp.valueOf("2021-12-28 08:45:00"));
        reflexRule.setConditions(reflexRuleConditionSet);

        List<ReflexRule> reflexRules = reflexRuleService.getAll();
        assertEquals(3, reflexRules.size());
        assertFalse("Endoscopy rule", reflexRules.stream().anyMatch(rl -> "Endoscopy rule".equals(rl.getRuleName())));
        testReflexService.saveOrUpdateReflexRule(reflexRule);
        List<ReflexRule> updatedReflexRules = reflexRuleService.getAll();
        assertEquals(4, updatedReflexRules.size());
        assertTrue("Endoscopy rule",
                updatedReflexRules.stream().anyMatch(rl -> "Endoscopy rule".equals(rl.getRuleName())));
    }

    @Test
    public void getAllReflexRules_ShouldReturnAllReflexRulesInTheDB() {
        List<ReflexRule> reflexRules = testReflexService.getAllReflexRules();
        assertNotNull(reflexRules);
        assertEquals(3, reflexRules.size());
        assertEquals(Integer.valueOf("1403"), reflexRules.get(2).getId());
    }

    @Test
    public void deactivateReflexRule_ShouldSetActiveToFalse() {
        ReflexRule reflexRule = reflexRuleService.get(1402);
        assertTrue(reflexRule.getActive());
        testReflexService.deactivateReflexRule("1402");
        ReflexRule updatedReflexRule = reflexRuleService.get(1402);
        System.out.println(updatedReflexRule.getActive());
        assertFalse(updatedReflexRule.getActive());
    }

    @Test
    public void getReflexRuleByAnalyteId_ShouldReturnAReflexRuleMatchingTheAnalyteIdPassedAsParameter() {
        ReflexRule reflexRule = testReflexService.getReflexRuleByAnalyteId("701");
        assertNotNull(reflexRule);
        assertEquals(Integer.valueOf("1401"), reflexRule.getId());
    }

    @Test
    public void getTestReflexesByTestAnalyteId_ShouldReturnAllTestReflexesWithTheAnalyteIdPassedAsParameter() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestAnalyteId("3001");
        assertNotNull(testReflexes);
        assertEquals(3, testReflexes.size());
        assertEquals("1004", testReflexes.get(2).getId());
    }
}
