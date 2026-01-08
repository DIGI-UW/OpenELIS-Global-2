package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.service.ResultSignatureService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestAnalyteService testAnalyteService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalyteService analyteService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private ResultSignatureService resultSignatureService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");

        // Ensure TestService maps are initialized before tests that use static methods
        // The TestServiceImpl constructor calls initializeGlobalVariables(), but in
        // test context
        // we need to ensure maps are refreshed after test data is loaded
        org.openelisglobal.test.service.TestService testServiceBean = org.openelisglobal.spring.util.SpringContext
                .getBean(org.openelisglobal.test.service.TestService.class);

        // Force initialization by calling a static method that triggers
        // ensureEntityMapInitialized
        // This ensures entityToMap is created if it doesn't exist
        org.openelisglobal.test.service.TestServiceImpl.getUserLocalizedReportingTestName("1");

        // Now refresh the maps to include the test data we just loaded
        testServiceBean.refreshTestNames();
    }

    @Test
    public void getAll_shouldReturnAllResults() {
        List<Result> results = resultService.getAll();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());

    }

    @Test
    public void getData_shouldReturnResultData() {
        Result result = resultService.get("3");
        resultService.getData(result);
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("85.0", result.getValue());

    }

    @Test
    public void getResultByAnalysis_shouldReturnResultsForAnalysis() {
        Analysis analysis = analysisService.get("1");
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultByAnalysisAndAnalyte_shouldReturnResultForAnalysisAndAnalyte() {
        Result result = resultService.get("3");
        Analysis analysis = analysisService.get("1");
        TestAnalyte testAnalyte = testAnalyteService.get("1");
        resultService.getResultByAnalysisAndAnalyte(result, analysis, testAnalyte);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getResultByTestResult_shouldReturnResultForTestResult() {
        Result result = resultService.get("3");
        TestResult testResult = testResultService.get("1");
        resultService.getResultByTestResult(result, testResult);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getResultsForSample_shouldReturnResultsForSample() {
        Sample sample = sampleService.get("1");
        List<Result> results = resultService.getResultsForSample(sample);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getUOM_shouldReturnUnitOfMeasure() {
        Result result = resultService.get("3");
        String uom = resultService.getUOM(result);
        assertNotNull(uom);
        assertEquals("mg/dL", uom);
    }

    @Test
    public void getResultById_shouldReturnResultById() {
        Result result = resultService.getResultById("3");
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("85.0", result.getValue());
    }

    @Test
    public void getAllResults_shouldReturnAllResults() {
        List<Result> results = resultService.getAllResults();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getResultById_givenResult_shouldReturnResult() {
        Result result = new Result();
        result.setId("3");
        Result fetchedResult = resultService.getResultById(result);
        assertNotNull(fetchedResult);
        assertEquals("3", fetchedResult.getId());
        assertEquals("85.0", fetchedResult.getValue());
    }

    @Test
    public void getSignature_shouldReturnSignature() {
        Result result = resultService.get("4");
        String signature = resultService.getSignature(result);
        assertNotNull(signature);
        assertEquals("External Doctor", signature);
    }

    @Test
    public void getLastUpdatedTime_shouldReturnLastUpdatedTime() {
        Result result = resultService.get("4");
        String lastUpdatedTime = resultService.getLastUpdatedTime(result);
        assertNotNull(lastUpdatedTime);
        assertEquals("07/07/2025", lastUpdatedTime);
    }

    @Test
    public void getTestType_shouldReturnTestType() {
        Result result = resultService.get("3");
        String testType = resultService.getTestType(result);
        assertNotNull(testType);
        assertEquals("N", testType);
    }

    @Test
    public void getTestTime_shouldReturnTestTime() {
        Result result = resultService.get("3");
        String testTime = resultService.getTestTime(result);
        assertNotNull(testTime);
        assertEquals("07/07/2025", testTime);
    }

    @Test
    public void getLOINCCode_shouldReturnLOINCCode() {
        Result result = resultService.get("3");
        String loincCode = resultService.getLOINCCode(result);
        assertNotNull(loincCode);
        assertEquals("123456", loincCode);
    }

    @Test
    public void getTestDescription_shouldReturnTestDescription() {
        Result result = resultService.get("3");
        String testDescription = resultService.getTestDescription(result);
        assertNotNull(testDescription);
        assertEquals("GPT/ALAT(Serum)", testDescription);
    }

    @Test
    public void getResultForAnalyteAndSampleItem_shouldReturnResultForAnalyteAndSampleItem() {
        String sampleItem = sampleItemService.get("601").getId();
        String analyte = analyteService.get("3").getId();
        Result result = resultService.getResultForAnalyteAndSampleItem(analyte, sampleItem);
        assertNotNull(result);
        assertEquals("3", result.getId());

    }

    @Test
    public void getResultsForTestAndSample_shouldReturnResultsForTestAndSample() {
        String sampleId = testService.get("1").getId();
        String testId = sampleService.get("1").getId();
        List<Result> results = resultService.getResultsForTestAndSample(sampleId, testId);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getReportableResultsByAnalysis_shouldReturnReportableResults() {
        Analysis analysis = analysisService.get("1");
        List<Result> results = resultService.getReportableResultsByAnalysis(analysis);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForAnalysisIdList_shouldReturnResultsForAnalysisIdList() {
        List<Integer> analysisIdList = List.of(1, 2);
        List<Result> results = resultService.getResultsForAnalysisIdList(analysisIdList);
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getResultForAnalyteInAnalysisSet_shouldReturnResultForAnalyteInAnalysisSet() {
        String analyteId = analyteService.get("3").getId();
        List<Integer> analysisIDList = List.of(1, 2);
        Result result = resultService.getResultForAnalyteInAnalysisSet(analyteId, analysisIDList);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getPageOfResults_shouldReturnPageOfResults() {
        List<Result> results = resultService.getPageOfResults(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(results.size() <= expectedPages);

    }

    @Test
    public void getReportingTestName_shouldReturnReportingTestName() {
        Result result = resultService.get("3");
        String reportingTestName = resultService.getReportingTestName(result);
        assertNotNull(reportingTestName);
        assertEquals("GPT/ALAT", reportingTestName);
    }

    @Test
    public void getResultsForTestSectionInDateRange_shouldReturnResultsForTestSectionInDateRange() {
        String testSectionId = testSectionService.get("1").getId();
        Date lowDate = Date.valueOf("2025-01-01");
        Date highDate = Date.valueOf("2025-12-12");

        List<Result> results = resultService.getResultsForTestSectionInDateRange(testSectionId, lowDate, highDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForPanelInDateRange_shouldReturnResultsForPanelInDateRange() {
        String panelId = panelService.get("1").getId();
        Date lowDate = Date.valueOf("2025-01-01");
        Date highDate = Date.valueOf("2025-12-12");
        List<Result> results = resultService.getResultsForPanelInDateRange(panelId, lowDate, highDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForTestInDateRange_shouldReturnResultsForTestInDateRange() {
        String testId = testService.get("1").getId();
        Date startDate = Date.valueOf("2025-01-01");
        Date endDate = Date.valueOf("2025-12-12");
        List<Result> results = resultService.getResultsForTestInDateRange(testId, startDate, endDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getDisplayReferenceRange_shouldReturnDisplayReferenceRange() {
        Result result = resultService.get("3");
        String displayReferenceRange = resultService.getDisplayReferenceRange(result, false);
        assertNotNull(displayReferenceRange);
        assertEquals("70.0-100.0", displayReferenceRange);
    }

    @Test
    public void getSimpleResultValue_shouldReturnSimpleResultValue() {
        Result result = resultService.get("3");
        String simpleResultValue = resultService.getSimpleResultValue(result);
        assertNotNull(simpleResultValue);
        assertEquals("85.0", simpleResultValue);
    }

    @Test
    public void getResultValueForDisplay_shouldReturnResultValueForDisplay() {
        Result result = resultService.get("3");
        String resultValue = resultService.getResultValue(result, ", ", true, true);
        assertNotNull(resultValue);
        assertEquals("85.0 mg/dL", resultValue);
    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingResults() {
        List<Result> results = resultService.getAllMatching("value", "85.0");
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getAllMatchingGivenMap_shouldReturnAllMatchingResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<Result> results = resultService.getAllMatching(map);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getAllOrdered_shouldReturnAllOrderedResults() {
        List<Result> results = resultService.getAllOrdered("id", false);
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getAllOrderedGivenList_shouldReturnAllOrderedResults() {
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getAllOrdered(orderBy, false);
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_shouldReturnMatchingOrderedResults() {
        List<Result> results = resultService.getAllMatchingOrdered("value", "85.0", "id", false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMapAndList_shouldReturnMatchingOrderedResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getAllMatchingOrdered(map, orderBy, false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenList_shouldReturnAllMatchingOrderedResults() {
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getAllMatchingOrdered("value", "85.0", orderBy, false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getAllMatchingOrderedGivenMap_shouldReturnAllMatchingOrderedResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<Result> results = resultService.getAllMatchingOrdered(map, "id", false);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getPage_shouldReturnPageOfResults() {
        List<Result> results = resultService.getPage(1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getMatchingPage_shouldReturnMatchingPageOfResults() {
        List<Result> results = resultService.getMatchingPage("value", "85.0", 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);

    }

    @Test
    public void getMatchingPageGivenMap_shouldReturnMatchingPageOfResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<Result> results = resultService.getMatchingPage(map, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getOrderedPage_shouldReturnOrderedPageOfResults() {
        List<Result> results = resultService.getOrderedPage("id", false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);

    }

    @Test
    public void getOrderedPageGivenList_shouldReturnOrderedPageOfResults() {
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getOrderedPage(orderBy, false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getMatchingOrderedPage_shouldReturnMatchingOrderedPageOfResults() {
        List<Result> results = resultService.getMatchingOrderedPage("value", "85.0", "id", false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getMatchingOrderedPageGivenMap_shouldReturnMatchingOrderedPageOfResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<Result> results = resultService.getMatchingOrderedPage(map, "id", false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getMatchingOrderedPageGivenList_shouldReturnMatchingOrderedPageOfResults() {
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getMatchingOrderedPage("value", "85.0", orderBy, false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getMatchingOrderedPageGivenMapAndList_shouldReturnMatchingOrderedPageOfResults() {
        Map<String, Object> map = Map.of("value", "85.0");
        List<String> orderBy = List.of("id");
        List<Result> results = resultService.getMatchingOrderedPage(map, orderBy, false, 1);
        int expectedPageSize = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertNotNull(results);
        assertTrue(results.size() <= expectedPageSize);
    }

    @Test
    public void getNext_shouldReturnNextResult() {
        String resultId = resultService.get("3").getId();
        Result nextResult = resultService.getNext(resultId);
        assertNotNull(nextResult);
        assertEquals("4", nextResult.getId());
    }

    @Test
    public void getPrevious_shouldReturnPreviousResult() {
        String resultId = resultService.get("4").getId();
        Result previousResult = resultService.getPrevious(resultId);
        assertNotNull(previousResult);
        assertEquals("3", previousResult.getId());
    }

    @Test
    public void deleteAll_shouldDeleteAllResults() {

        List<ResultSignature> signatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(signatures);
        List<Result> results1 = resultService.getAll();
        resultService.deleteAll(results1);
        List<Result> results2 = resultService.getAll();
        assertEquals(0, results2.size());

    }

    @Test
    public void deleteAllGivenList_shouldDeleteAllResults() {

        List<ResultSignature> signatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(signatures);

        List<String> resultIds = List.of("3", "4");
        resultService.deleteAll(resultIds, "");
        List<Result> results = resultService.getAll();
        assertEquals(0, results.size());
    }

    @Test
    public void delete_shouldDeleteAResult() {
        List<ResultSignature> signatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(signatures);
        Result result = resultService.get("3");
        assertNotNull(result);
        resultService.delete(result);
        List<Result> results = resultService.getAll();
        assertEquals(1, results.size());
    }

    @Test
    public void getCount_shouldReturnCountOfResults() {
        int count = resultService.getCount();
        assertEquals(2, count);

    }

    @Test
    public void save_shouldSaveResult() {
        List<ResultSignature> signatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(signatures);
        List<Result> results1 = resultService.getAll();
        resultService.deleteAll(results1);
        Result result = new Result();
        result.setValue("90.0");
        result.setAnalysis(analysisService.get("1"));
        result.setTestResult(testResultService.get("1"));
        result.setAnalyte(analyteService.get("3"));
        Result result1 = resultService.save(result);
        List<Result> results2 = resultService.getAll();
        assertNotNull(result1);
        assertEquals(1, results2.size());
        assertEquals("90.0", results2.get(0).getValue());

    }

    @Test
    public void insert_shouldInsertResult() {
        List<ResultSignature> signatures = resultSignatureService.getAll();
        resultSignatureService.deleteAll(signatures);
        List<Result> results1 = resultService.getAll();
        resultService.deleteAll(results1);
        Result result = new Result();
        result.setValue("90.0");
        result.setAnalysis(analysisService.get("1"));
        result.setTestResult(testResultService.get("1"));
        result.setAnalyte(analyteService.get("3"));
        String result1 = resultService.insert(result);
        List<Result> results2 = resultService.getAll();
        assertNotNull(result1);
        assertEquals(1, results2.size());
        assertEquals(result1, results2.get(0).getId());
        assertEquals("90.0", results2.get(0).getValue());

    }

    @Test
    public void update_shouldUpdateResult() {
        Result result = resultService.get("3");
        result.setValue("95.0");
        Result updatedResult = resultService.update(result);
        assertNotNull(updatedResult);
        assertEquals("95.0", updatedResult.getValue());
    }
}
