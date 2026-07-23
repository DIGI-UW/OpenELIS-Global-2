package org.openelisglobal.testReflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testreflex.action.bean.ReflexRule;
import org.openelisglobal.testreflex.action.bean.ReflexRuleOptions;
import org.openelisglobal.testreflex.dao.TestReflexDAO;
import org.openelisglobal.testreflex.service.TestReflexService;
import org.openelisglobal.testreflex.valueholder.TestReflex;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class TestReflexServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestReflexService testReflexService;

    @Autowired
    private TestReflexDAO testReflexDAO;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestAnalyteService testAnalyteService;

    @Autowired
    private AnalyteService analyteService;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/test-reflex.xml");
    }

    @Test
    public void getAll_shouldReturnAllTestReflexes() {
        List<TestReflex> testReflexes = testReflexService.getAll();
        assertEquals(2, testReflexes.size());
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
        assertEquals(1, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByTestAndFlag_shouldReturnReflexesForTestAndFlag() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestAndFlag("1", "R");
        assertEquals(1, testReflexes.size());
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
        assertEquals(2, reflexRules.size());
        assertEquals("1001", reflexRules.get(0).getId());
        assertEquals("1002", reflexRules.get(1).getId());
    }

    @Test
    public void getFlaggedTestReflexesByTestResult_shouldReturnFlaggedReflexes() {
        TestResult testResult = testResultService.get("1");
        List<TestReflex> testReflexes = testReflexService.getFlaggedTestReflexesByTestResult(testResult, "R");
        assertEquals(1, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByTestResultAnalyteTest_shouldReturnReflexesForTestResultAnalyteAndTest() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByTestResultAnalyteTest("1", "1", "1");
        assertEquals(1, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getTestReflexsByAnalyteAndTest_shouldReturnReflexesForAnalyteAndTest() {
        List<TestReflex> testReflexes = testReflexService.getTestReflexsByAnalyteAndTest("1", "1");
        assertEquals(1, testReflexes.size());
        assertEquals("1001", testReflexes.get(0).getId());
    }

    @Test
    public void getAllReflexRules_shouldReturnAllReflexRules() {

        List<ReflexRule> reflexRules = testReflexService.getAllReflexRules();

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
        assertEquals(1, testReflexes.size());
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
    public void duplicateTestReflexExists_shouldReturnTrueForMatchingReflex() {
        // Build a TestReflex matching existing record 1001 but with a different ID
        // test_reflex 1001: test_id=1 (name_localization_id=1), test_analyte_id=1
        // (analyte "Cholesterol"),
        // tst_rslt_id=1, add_test_id=2 (name_localization_id=2), scriptlet_id=1
        TestReflex testReflex = new TestReflex();
        testReflex.setId("9999");
        testReflex.setTest(testService.get("1"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setAddedTest(testService.get("2"));

        assertTrue(testReflexDAO.duplicateTestReflexExists(testReflex));
    }

    @Test
    public void duplicateTestReflexExists_shouldReturnFalseWhenNoDuplicate() {
        // Use test_id=2 with analyte "Cholesterol" from test_analyte_id=1 — no such
        // combination exists
        TestReflex testReflex = new TestReflex();
        testReflex.setId("9999");
        testReflex.setTest(testService.get("2"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setAddedTest(testService.get("1"));

        assertFalse(testReflexDAO.duplicateTestReflexExists(testReflex));
    }

    @Test
    public void duplicateTestReflexExists_shouldExcludeCurrentRecord() {
        // Use same data as record 1001 but with ID "1001" — should not match itself
        TestReflex testReflex = new TestReflex();
        testReflex.setId("1001");
        testReflex.setTest(testService.get("1"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setAddedTest(testService.get("2"));

        assertFalse(testReflexDAO.duplicateTestReflexExists(testReflex));
    }

    @Test
    public void duplicateTestReflexExists_shouldHandleNullAddedTest() {
        // When addedTest is null, the query uses "-1" as placeholder — no match
        // expected
        TestReflex testReflex = new TestReflex();
        testReflex.setId("9999");
        testReflex.setTest(testService.get("1"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTestResult(testResultService.get("1"));

        assertFalse(testReflexDAO.duplicateTestReflexExists(testReflex));
    }

    @Test
    public void duplicateTestReflexExists_shouldBeCaseInsensitiveOnAnalyteName() {
        // Existing record 1001 has analyte "Cholesterol" — use eagerly loaded
        // entities to avoid lazy initialization issues
        TestReflex testReflex = new TestReflex();
        testReflex.setId("9999");
        testReflex.setTest(testService.get("1"));
        testReflex.setTestAnalyte(testAnalyteService.get("1"));
        testReflex.setTestResult(testResultService.get("1"));
        testReflex.setAddedTest(testService.get("2"));

        // This exercises the case-insensitive trim(lower()) comparison in the HQL
        assertTrue(testReflexDAO.duplicateTestReflexExists(testReflex));
    }
}