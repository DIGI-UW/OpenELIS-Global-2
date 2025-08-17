package org.openelisglobal.testReflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.scriptlet.service.ScriptletService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
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
    private AnalyteService analyteService;
    @Autowired
    private TestAnalyteService testAnalyteService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestService testService;
    @Autowired
    private ScriptletService scriptletService;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void getAll_shouldReturnAllTestReflexes() {
        List<TestReflex> testReflexes = testReflexService.getAll();
        assertEquals(3, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
        assertEquals("1002", testReflexes.get(1).getId());

    }

    @Test
    public void getData_shouldReturnTestReflexiNFO() {
        TestReflex testReflex = new TestReflex();
        testReflex.setId("1001");
        testReflexService.getData(testReflex);
        assertEquals("1001", testReflex.getId());
        assertEquals("R", testReflex.getFlags());
    }

    @Test
    public void getPageOfTestReflexs_shouldReturnPagedResults() {
        List<TestReflex> testReflexes = testReflexService.getPageOfTestReflexs(1);
        int expecteddSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(testReflexes.size() <= expecteddSize);
    }

    @Test
    public void getTestsReflexesByTestResult_shouldReturnReflexesForTestResult() {
        TestResult testResult = new TestResult();
        testResult.setId("1");
        List<TestReflex> testReflexes = testReflexService.getTestReflexesByTestResult(testResult);
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByTestAndFlag_shouldReturnReflexesForTestAndFlag() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestAndFlag("1", "R");
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTotalTestReflexCount_shouldReturnTotalCount() {
        List<TestReflex> testReflexes = testReflexService.getAll();
        Integer count = testReflexService.getTotalTestReflexCount();
        assertEquals(testReflexes.size(), count.intValue());
    }

    @Test
    public void getAllReflexes_shouldReturnAllReflexes() {
        List<TestReflex> reflexRules = testReflexService.getAllTestReflexs();
        assertEquals(3, reflexRules.size());
        assertEquals("1003", reflexRules.get(0).getId());
        assertEquals("1001", reflexRules.get(1).getId());
    }

    @Test
    public void getFlaggedTestReflexesByTestResult_shouldReturnFlaggedReflexes() {
        TestResult testResult = testResultService.get("1");
        List<TestReflex> testReflexes = testReflexService.getFlaggedTestReflexesByTestResult(testResult, "R");
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByTestResultAnalyteTest_shouldReturnReflexesForTestResultAnalyteAndTest() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestResultAnalyteTest("1", "1", "1");
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByAnalyteAndTest_shouldReturnReflexesForAnalyteAndTest() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByAnalyteAndTest("1", "1");
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getAllReflexRules_shouldReturnAllReflexRules() {

        List<ReflexRule> reflexRules = testReflexService.getAllReflexRules();
        assertEquals(2, reflexRules.size());
        assertEquals("Test Name", reflexRules.get(0).getRuleName());
    }

    @Test
    public void getReflexRuleByAnalyteId_shouldReturnReflexRuleForAnalyte() {

        ReflexRule reflexRuleResult = testReflexService.getReflexRuleByAnalyteId("1");
        assertEquals("Test Name", reflexRuleResult.getRuleName());

    }

    @Test
    public void deactivateReflexRule_shouldDeactivateRule() {
        testReflexService.deactivateReflexRule("100");

        List<ReflexRule> reflexRules = testReflexService.getAllReflexRules();
        assertFalse(reflexRules.get(1).getActive());

    }

    @Test
    public void getTestReflexsByTestAnalyteId_shouldReturnTestReflexesByTestAnalyte() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestAnalyteId("1");
        assertEquals(2, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void saveOrUpdateReflexRule_shouldSaveNewReflexRule() {

        Analyte analyte = analyteService.get("2");
        int Id = Integer.parseInt(analyte.getId());
        ReflexRule reflexRule = new ReflexRule();
        reflexRule.setRuleName("Test Name");
        reflexRule.setStringId("100");
        reflexRule.setOverall(ReflexRuleOptions.OverallOptions.ALL);
        reflexRule.setAnalyteId(Id);
        reflexRule.setConditions(new HashSet<>());
        testReflexService.saveOrUpdateReflexRule(reflexRule);
        List<ReflexRule> rules = testReflexService.getAllReflexRules();

        assertEquals("Test Name", rules.get(0).getRuleName());
    }

    @Test
    public void saveOrUpdateReflexRule_TestUpdatingTheExistingReflexRule() {
        List<ReflexRule> reflexRules = testReflexService.getAllReflexRules();
        assertEquals("Test Name1", reflexRules.get(1).getRuleName());
        assertTrue(reflexRules.get(1).getActive());
        reflexRules.get(1).setRuleName("High Hemoglobin test Alert");
        reflexRules.get(1).setActive(false);
        testReflexService.saveOrUpdateReflexRule(reflexRules.get(1));
        assertEquals("High Hemoglobin test Alert", reflexRules.get(1).getRuleName());
        assertFalse(reflexRules.get(1).getActive());
    }

    @Test
    public void isReflexedTest_ShouldReturnTrueIfTestReflexWasReflexedOrLinked() {
        Analysis analysis = analysisService.get("1");
        boolean isReflexedTest = testReflexService.isReflexedTest(analysis);
        assertTrue(isReflexedTest);
    }

    @Test
    public void getTestReflexesByTestResultAndTestAnalyte_ShouldReturnTestReflexesWithTestResultAndTestAnalyteValuesPassedAsParameter() {
        TestResult testResult = testResultService.get("1");
        TestAnalyte testAnalyte = testAnalyteService.get("1");
        List<TestReflex> testReflexes = testReflexService.getTestReflexesByTestResultAndTestAnalyte(testResult,
                testAnalyte);
        assertNotNull(testReflexes);
        assertEquals(2, testReflexes.size());
        assertEquals("1003", testReflexes.get(1).getId());
    }

    @Test
    public void insert_ShouldInsertANewTestReflexInTheDB() {
        List<TestReflex> testReflexes = testReflexService.getAllTestReflexs();
        assertEquals(3, testReflexes.size());
        assertFalse(testReflexes.stream().anyMatch(tr -> "Test Reflex internal note".equals(tr.getInternalNote())));
        TestReflex testReflex = new TestReflex();
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setFlags("C");
        testReflex.setLastupdated(Timestamp.valueOf("2024-11-06 13:26:00"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTest(testService.get("2"));
        testReflex.setInternalNote("Test Reflex internal note");
        testReflex.setExternalNote("Test Reflex external note");

        String testId = testReflexService.insert(testReflex);
        List<TestReflex> updatedTestReflexes = testReflexService.getAllTestReflexs();
        assertEquals(4, updatedTestReflexes.size());
        System.out.println("New TestReflex Id: " + testId);
        assertTrue(
                updatedTestReflexes.stream().anyMatch(tr -> "Test Reflex internal note".equals(tr.getInternalNote())));
    }

    @Test
    public void insert_ShouldThrowDuplicateRecordException() {
        try {
            TestReflex testReflex = new TestReflex();
            testReflex.setTestResult(testResultService.get("1"));
            testReflex.setFlags("R");
            testReflex.setLastupdated(Timestamp.valueOf("2025-08-06 10:00:00"));
            testReflex.setTestAnalyte(testAnalyteService.get("1"));
            testReflex.setTest(testService.get("1"));
            testReflex.setAddedTest(testService.get("1"));
            testReflex.setActionScriptlet(scriptletService.get("1"));
            testReflex.setNonDictionaryValue("Trigger Value A");
            testReflex.setRelation(ReflexRuleOptions.NumericRelationOptions.EQUALS);
            testReflex.setInternalNote("Reflex Internal A");
            testReflex.setExternalNote("Reflex External A");

            testReflexService.insert(testReflex);
            fail("Expected Exception to be thrown ");
        } catch (LIMSDuplicateRecordException exception) {
            assertEquals("Duplicate record exists for GPT/ALATCholesterol5.6GPT/ALAT", exception.getMessage());
        }
    }

    @Test
    public void save_ShouldSaveANewTestReflexInTheDB() {
        List<TestReflex> testReflexes = testReflexService.getAllTestReflexs();
        assertEquals(3, testReflexes.size());
        assertFalse(testReflexes.stream().anyMatch(tr -> "Test Reflex internal note".equals(tr.getInternalNote())));
        TestReflex testReflex = new TestReflex();
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setFlags("C");
        testReflex.setLastupdated(Timestamp.valueOf("2024-11-06 13:26:00"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTest(testService.get("2"));
        testReflex.setInternalNote("Test Reflex internal note");
        testReflex.setExternalNote("Test Reflex external note");

        TestReflex newTestReflex = testReflexService.save(testReflex);
        List<TestReflex> updatedTestReflexes = testReflexService.getAllTestReflexs();
        assertEquals(4, updatedTestReflexes.size());
        System.out.println("New TestReflex Id: " + newTestReflex.getId());
        assertTrue(
                updatedTestReflexes.stream().anyMatch(tr -> "Test Reflex internal note".equals(tr.getInternalNote())));
    }

    @Test
    public void save_ShouldThrowDuplicateRecordException() {
        try {
            TestReflex testReflex = new TestReflex();
            testReflex.setTestResult(testResultService.get("1"));
            testReflex.setFlags("C");
            testReflex.setLastupdated(Timestamp.valueOf("2025-08-06 10:10:00"));
            testReflex.setTestAnalyte(testAnalyteService.get("2"));
            testReflex.setTest(testService.get("2"));
            testReflex.setAddedTest(testService.get("1"));
            testReflex.setSiblingReflexId("1001");
            testReflex.setActionScriptlet(scriptletService.get("2"));
            testReflex.setNonDictionaryValue("Trigger Value B");
            testReflex.setRelation(ReflexRuleOptions.NumericRelationOptions.EQUALS);
            testReflex.setInternalNote("Reflex Internal B");
            testReflex.setExternalNote("Reflex External B");

            testReflexService.save(testReflex);
            fail("Expected Exception to be thrown ");
        } catch (LIMSDuplicateRecordException exception) {
            assertEquals("Duplicate record exists for GOT/ASATPotassium5.6GPT/ALAT", exception.getMessage());
        }
    }
}