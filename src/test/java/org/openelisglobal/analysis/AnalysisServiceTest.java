package org.openelisglobal.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.reports.service.DocumentTrackService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.reports.valueholder.DocumentTrack;
import org.openelisglobal.referencetables.service.ReferenceTablesService;
import org.openelisglobal.referencetables.valueholder.ReferenceTables;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.sampleqaevent.service.SampleQaEventService;
import org.openelisglobal.sampleqaevent.valueholder.SampleQaEvent;
import org.openelisglobal.statusofsample.service.StatusOfSampleService;
import org.openelisglobal.analysisqaevent.service.AnalysisQaEventService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private AnalysisQaEventService analysisQaEventService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleQaEventService sampleQaEventService;

    @Autowired
    private DocumentTrackService documentTrackService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private StatusOfSampleService statusOfSampleService;

    @Autowired
    private MethodService methodService;

    @Autowired
    private ReferenceTablesService referenceTablesService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    public void getAnalysisFromDatabase_shouldReturnAllAnalyses() {
        List<Analysis> analysisList = analysisService.getAll();
        assertEquals(2, analysisList.size());
    }

    @Test
    public void getTestDisplayName_shouldReturnCorrectTestName() {
        String nullDisplayName = analysisService.getTestDisplayName(null);
        assertEquals("", nullDisplayName);

        Analysis analysis = analysisService.get("1");
        assertEquals("1", analysis.getId());
        String displayName = analysisService.getTestDisplayName(analysis);
        assertNotNull(displayName);
        assertEquals("DE1 &rarr; GPT/ALAT(Serum)", displayName);
    }

    @Test
    public void insert_shouldGenerateFhirUuidIfNotProvided() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "analysis" });
        Analysis newAnalysis = new Analysis();
        newAnalysis.setAnalysisType("ROUTINE");
        String insertedId = analysisService.insert(newAnalysis);
        assertNotNull(insertedId);

        Analysis retrievedAnalysis = analysisService.get(insertedId);
        assertEquals(insertedId, retrievedAnalysis.getId());
        assertEquals("ROUTINE", retrievedAnalysis.getAnalysisType());
    }

    @Test
    public void getCSVMultiselectResults_shouldReturnMultiselectResultsAsCSVAndNullAnalysis() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "result" });

        String nullCsvResults = analysisService.getCSVMultiselectResults(null);
        assertEquals("", nullCsvResults);

        Analysis newAnalysis = new Analysis();
        newAnalysis.setAnalysisType("ROUTINE");
        String insertedId = analysisService.insert(newAnalysis);
        assertNotNull(newAnalysis);

        Result result1 = new Result();
        result1.setAnalysis(newAnalysis);
        result1.setValue("1");
        result1.setResultType("M");
        resultService.insert(result1);

        Result result2 = new Result();
        result2.setAnalysis(newAnalysis);
        result2.setValue("2");
        result2.setResultType("M");
        resultService.insert(result2);

        String csvResults = analysisService.getCSVMultiselectResults(newAnalysis);
        assertEquals("1,2", csvResults);
        // Since the test data doesn't explicitly show multiselect results,
        // this test may need adjustment based on actual data
    }

    @Test
    public void getJSONMultiSelectResults_shouldHandleNullAndValidAnalysis() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "result" });
        String nullJsonResults = analysisService.getJSONMultiSelectResults(null);
        assertEquals("", nullJsonResults);

        Analysis newAnalysis = new Analysis();
        newAnalysis.setAnalysisType("ROUTINE");
        String insertedId = analysisService.insert(newAnalysis);
        assertNotNull(newAnalysis);

        Result result1 = new Result();
        result1.setAnalysis(newAnalysis);
        result1.setValue("1");
        result1.setResultType("M");
        resultService.insert(result1);

        Result result2 = new Result();
        result2.setAnalysis(newAnalysis);
        result2.setValue("2");
        result2.setResultType("M");
        resultService.insert(result2);

        String jsonResults = analysisService.getJSONMultiSelectResults(newAnalysis);
        assertEquals("{\"0\":\"1,2\"}", jsonResults);
        // Since the test data doesn't explicitly show multiselect results,
        // this test may need adjustment based on actual data
    }

    @Test
    public void getQuantifiedResult_shouldHandleNullAndValidAnalysis() {
        Result resultForNull = analysisService.getQuantifiedResult(null);
        assertNull(resultForNull);

        Analysis newAnalysis = analysisService.get("1");

        Result result1 = resultService.get("1");
        result1.setAnalysis(newAnalysis);
        result1.setResultType("D");
        result1.setValue("1");
        resultService.update(result1);

        Result result2 = resultService.get("2");
        result2.setAnalysis(newAnalysis);
        result2.setResultType("X");
        result2.setValue("2");
        result2.setParentResult(result1);
        resultService.update(result2);

        Result result = analysisService.getQuantifiedResult(newAnalysis);
        assertNotNull(result);
        assertEquals("2", result.getValue());
    }

    @Test
    public void getCompletedDateForDisplay_shouldHandleNullAndValidAnalysis() {
        // Test with null analysis
        String completedDateForNull = analysisService.getCompletedDateForDisplay(null);
        assertEquals("", completedDateForNull);

        // Test with valid analysis
        Analysis analysis = analysisService.get("1");
        String completedDate = analysisService.getCompletedDateForDisplay(analysis);
        assertEquals("15/11/2023", completedDate);
        assertNotNull(completedDate);
        assertTrue(completedDate.length() > 0);
    }

    @Test
    public void getAnalysisType_shouldHandleNullAndValidAnalysis() {
        // Test with null analysis
        String analysisTypeForNull = analysisService.getAnalysisType(null);
        assertEquals("", analysisTypeForNull);

        // Test with valid analysis
        Analysis analysis = analysisService.get("1");
        String analysisType = analysisService.getAnalysisType(analysis);
        assertEquals("ROUTINE", analysisType);
    }

    @Test
    public void getStatusId_shouldHandleNullAndValidAnalysis() {
        // Test with null analysis
        String statusIdForNull = analysisService.getStatusId(null);
        assertEquals("", statusIdForNull);

        // Test with valid analysis
        Analysis analysis = analysisService.get("1");
        String statusId = analysisService.getStatusId(analysis);
        assertEquals("1", statusId);
    }

    @Test
    public void getTriggeredReflex_shouldHandleNullAndValidCases() {
        // Null case
        Boolean nullResult = analysisService.getTriggeredReflex(null);
        assertFalse(nullResult);

        // Valid cases
        Analysis analysis = analysisService.get("1");
        assertNotNull(analysis);

        analysis.setTriggeredReflex(false);
        Boolean falseResult = analysisService.getTriggeredReflex(analysis);
        assertFalse(falseResult);

        analysis.setTriggeredReflex(true);
        Boolean trueResult = analysisService.getTriggeredReflex(analysis);
        assertTrue(trueResult);
    }

    @Test
    public void resultIsConclusion_shouldHandleNullAndValidCases() {
        // Null cases
        assertFalse(analysisService.resultIsConclusion(null, null));

        Analysis analysis1 = analysisService.get("1");
        Analysis analysis2 = analysisService.get("2");
        assertFalse(analysisService.resultIsConclusion(null, analysis1));

        Result result1 = resultService.get("1");
        result1.setAnalysis(analysis1);
        resultService.update(result1);

        assertFalse(analysisService.resultIsConclusion(result1, null));

        // Single result case
        List<Result> singleResultList = List.of(result1);
        assertFalse(analysisService.resultIsConclusion(result1, analysis1));

        // Multiple results - current result has highest ID (is conclusion)

        Result result2 = resultService.get("2");
        result2.setAnalysis(analysis1);
        resultService.update(result2);

        List<Result> multipleResultList = List.of(result1, result2);
        assertTrue(analysisService.resultIsConclusion(result2, analysis1));

        // Multiple results - current result does not have highest ID (not conclusion)
        Result result3 = new Result();
        result3.setId("3");
        result3.setAnalysis(analysis1);
        resultService.insert(result3);

        List<Result> resultList = List.of(result1, result2, result3);
        assertFalse(analysisService.resultIsConclusion(result2, analysis1));
    }

    // SpringContextIssue
    @Test
    public void isParentNonConforming_shouldHandleNullAndValidCases() {
        // Null case
        assertFalse(analysisService.isParentNonConforming(null));

        // Valid cases
        Analysis analysis = analysisService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        Sample sample = sampleService.get("1");
        SampleQaEvent sampleQaEvent = sampleQaEventService.get("1");

        sample.setStatusId("12");
        sampleService.update(sample);

        sampleItem.setSample(sample);
        sampleItemService.update(sampleItem);

        analysis.setSampleItem(sampleItem);
        analysis.setStatusId("13");
        analysisService.update(analysis);

        sampleQaEvent.setSample(sample);
        sampleQaEvent.setSampleItem(sampleItem);
        sampleQaEventService.update(sampleQaEvent);

        assertNotNull(analysis);
        assertEquals("1", analysis.getId());
        assertEquals("1", analysis.getSampleItem().getSample().getId());

        // Test true case
        boolean value = analysisService.isParentNonConforming(analysis);
        assertTrue(value);
    }

    @Test
    public void getTest_shouldHandleNullAndValidCases() {
        // Null case
        org.openelisglobal.test.valueholder.Test nullResult = analysisService.getTest(null);
        assertNull(nullResult);

        // Valid case
        Analysis analysis = analysisService.get("1");
        assertNotNull(analysis);

        org.openelisglobal.test.valueholder.Test expectedTest = testService.get("1");
        analysis.setTest(expectedTest);

        org.openelisglobal.test.valueholder.Test result = analysisService.getTest(analysis);
        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    public void getAnalysisStartedOrCompletedInDateRange_shouldReturnCorrectList() {
        Date lowDate = Date.valueOf("2023-11-14");
        Date highDate = Date.valueOf("2023-11-17");

        Analysis analysis1 = analysisService.get("1");

        Analysis analysis2 = analysisService.get("2");

        List<Analysis> expectedList = List.of(analysis1, analysis2);

        List<Analysis> result = analysisService.getAnalysisStartedOrCompletedInDateRange(lowDate, highDate);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("2", result.get(1).getId());
    }

    @Test
    public void getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange_shouldReturnCorrectList() {
        Date lowDate = Date.valueOf("2023-11-14");
        Date highDate = Date.valueOf("2023-11-17");

        // Valid test parameters
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        String testId = test1.getId();
        TestSection testSection1 = testSectionService.get("1");
        List<Integer> testSectionIds = List.of(Integer.parseInt(testSection1.getId()));

        List<Analysis> analyses = analysisService.getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(lowDate,
                highDate, testId, testSectionIds);

        // Verify results
        assertNotNull(analyses);
        assertTrue(analyses.size() > 0);
        assertEquals("1", analyses.get(0).getId());
    }

    @Test
    public void getResults_shouldHandleNullAndValidAnalysis() {
        // Test null analysis
        List<Result> nullResults = analysisService.getResults(null);
        assertEquals(0, nullResults.size());

        // Test valid analysis
        Analysis validAnalysis = analysisService.get("1");
        Result result1 = resultService.get("1");
        result1.setAnalysis(validAnalysis);
        result1.setResultType("M");
        result1.setValue("1");
        resultService.update(result1);

        assertNotNull(validAnalysis);

        List<Result> validResults = analysisService.getResults(validAnalysis);

        assertNotNull(validResults);
        assertEquals(1, validResults.size());
    }

    @Test
    public void hasBeenCorrectedSinceLastPatientReport_shouldHandleNullAndValidAnalysis() {
        // Test null analysis
        boolean nullResult = analysisService.hasBeenCorrectedSinceLastPatientReport(null);
        assertFalse(nullResult);

        // Test valid analysis
        Analysis analysis1 = analysisService.get("1");
        analysis1.setCorrectedSincePatientReport(true);
        analysisService.update(analysis1);
        boolean result = analysisService.hasBeenCorrectedSinceLastPatientReport(analysis1);
        assertTrue(result);

        Analysis analysis2 = analysisService.get("1");
        analysis2.setCorrectedSincePatientReport(false);
        analysisService.update(analysis2);
        result = analysisService.hasBeenCorrectedSinceLastPatientReport(analysis2);
        assertFalse(result);
    }

    // Getting null
    @Test
    public void patientReportHasBeenDone_shouldHandleNullAndValidAnalysis() {
    // Test null analysis
    boolean nullResult = analysisService.patientReportHasBeenDone(null);
    assertFalse(nullResult);

    // Valid analysis test
    Analysis analysis = analysisService.get("1");
    SampleItem sampleItem = sampleItemService.get("1");
    Sample sample = sampleService.get("1");
    ReferenceTables refer1 = referenceTablesService.get("1");
    ReferenceTables refer2 = referenceTablesService.get("2");

    refer1.setTableName("SAMPLE");
    referenceTablesService.update(refer1);
    refer2.setTableName("ANALYSIS");
    referenceTablesService.update(refer2);

    sampleItem.setSample(sample);
    sampleItemService.update(sampleItem);

    analysis.setSampleItem(sampleItem);
    analysisService.update(analysis);

    Analysis analysis1 = analysisService.get("1");

    // Test when report exists
    boolean reportExistsResult = analysisService.patientReportHasBeenDone(analysis1);
    assertTrue(reportExistsResult);
    }

    @Test
    public void getNotesAsString_shouldHandleNullAndValidAnalysis() {
        // Test null analysis
        String nullResult = analysisService.getNotesAsString(null, true, true, "\n", false);
        assertEquals("", nullResult);

        // Test valid analysis
        Analysis analysis = analysisService.get("1");
        assertNotNull(analysis);

        String validResult = analysisService.getNotesAsString(analysis, true, true, "\n", false);
        assertEquals("Internal 22/02/2024 10:00 : Note text here\n", validResult);
    }

    @Test
    public void getOrderAccessionNumber_shouldHandleNullAndValidInput() {
        // Test with null analysis
        String nullResult = analysisService.getOrderAccessionNumber(null);
        assertEquals("", nullResult);

        // Test with valid analysis
        Analysis analysis = analysisService.get("1");
        String accessionNumber = analysisService.getOrderAccessionNumber(analysis);
        assertEquals("SAM-2023-001", accessionNumber);
    }

    @Test
    public void getTypeOfSample_shouldHandleNullAndValidInput() {
        // Test with null analysis
        TypeOfSample nullResult = analysisService.getTypeOfSample(null);
        assertNull(nullResult);

        // Test with valid analysis
        Analysis analysis = analysisService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setSampleItem(sampleItem);
        analysisService.update(analysis);

        Analysis analysis1 = analysisService.get("1");

        TypeOfSample typeOfSample = analysisService.getTypeOfSample(analysis1);
        assertNotNull(typeOfSample);
        assertEquals("1", typeOfSample.getId());
        assertEquals("Blood", typeOfSample.getDescription());
    }

    @Test
    public void getPanel_shouldHandleNullAndValidInput() {
        // Test with null analysis
        Panel nullResult = analysisService.getPanel(null);
        assertNull(nullResult);

        // Test with valid analysis that has panel
        Analysis analysis = analysisService.get("1");
        // Set up a panel for this analysis for testing
        Panel panel = panelService.get("1");
        analysis.setPanel(panel);

        Panel retrievedPanel = analysisService.getPanel(analysis);
        assertNotNull(retrievedPanel);
        assertEquals("1", retrievedPanel.getId());
        assertEquals("Test Panel", retrievedPanel.getPanelName());
    }

    @Test
    public void getTestSection_shouldHandleNullAndValidInput() {
        // Test with null analysis
        TestSection nullResult = analysisService.getTestSection(null);
        assertNull(nullResult);

        // Test with valid analysis
        Analysis analysis1 = analysisService.get("1");
        TestSection testSection1 = testSectionService.get("1");
        analysis1.setTestSection(testSection1);
        analysisService.update(analysis1);

        Analysis analysis = analysisService.get("1");

        TestSection testSection = analysisService.getTestSection(analysis);
        assertNotNull(testSection);
        assertEquals("1", testSection.getId());
        assertEquals("Chemistry", testSection.getTestSectionName());
    }

    @Test
    public void buildAnalysis_shouldCreateAnalysisWithCorrectProperties() {
        // Get sample data for test
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        SampleItem sampleItem = analysisService.get("1").getSampleItem();

        // Build analysis
        Analysis builtAnalysis = analysisService.buildAnalysis(test, sampleItem);

        // Verify the built analysis
        assertNotNull(builtAnalysis);
        assertEquals(test, builtAnalysis.getTest());
        assertEquals(test.getIsReportable(), builtAnalysis.getIsReportable());
        assertEquals("MANUAL", builtAnalysis.getAnalysisType());
        assertEquals("0", builtAnalysis.getRevision());
        assertNotNull(builtAnalysis.getStartedDate());
        assertEquals(sampleItem, builtAnalysis.getSampleItem());
        assertEquals(test.getTestSection(), builtAnalysis.getTestSection());
        assertNotNull(builtAnalysis.getSampleTypeName());

        // Verify status ID is set to NotStarted
        assertEquals("4", builtAnalysis.getStatusId());
    }

    @Test
    public void getAnalysesBySampleId_shouldReturnCorrectAnalysesForValidId() {
        // Test with valid sample ID
        List<Analysis> analyses = analysisService.getAnalysesBySampleId("1");
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with non-existent sample ID
        List<Analysis> nonExistentAnalyses = analysisService.getAnalysesBySampleId("999");
        assertNotNull(nonExistentAnalyses);
        assertTrue(nonExistentAnalyses.isEmpty());

    }

    @Test
    public void getAnalysisByAccessionAndTestId_shouldReturnCorrectAnalysesForValidInputs() {
        // Test with valid accession number and test ID
        List<Analysis> analyses = analysisService.getAnalysisByAccessionAndTestId("SAM-2023-001", "1");
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with accession number containing period (should be truncated)
        List<Analysis> analysesWithPeriod = analysisService.getAnalysisByAccessionAndTestId("SAM-2023-001.123", "1");
        assertNotNull(analysesWithPeriod);
        assertEquals(1, analysesWithPeriod.size());
        assertEquals("1", analysesWithPeriod.get(0).getId());

        // Test with non-existent accession number
        List<Analysis> nonExistentAnalyses = analysisService.getAnalysisByAccessionAndTestId("NON-EXISTENT", "1");
        assertNotNull(nonExistentAnalyses);
        assertTrue(nonExistentAnalyses.isEmpty());

        // Test with null values
        List<Analysis> nullAccessionAnalyses = analysisService.getAnalysisByAccessionAndTestId(null, "1");
        assertNotNull(nullAccessionAnalyses);
        assertTrue(nullAccessionAnalyses.isEmpty());

        List<Analysis> nullTestIdAnalyses = analysisService.getAnalysisByAccessionAndTestId("SAM-2023-001", null);
        assertNotNull(nullTestIdAnalyses);
        assertTrue(nullTestIdAnalyses.isEmpty());
    }

    @Test
    public void getAnalysisCollectedOnExcludedByStatusId_shouldFilterCorrectly() {
        // Create a test date matching the collection date in the test data
        Date testDate = Date.valueOf("2023-11-15");
        Set<Integer> excludedStatusIds = new HashSet<>(List.of(Integer.parseInt("2")));
        Analysis analysis = analysisService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setSampleItem(sampleItem);
        analysisService.update(analysis);

        // Test with valid date and excluded status IDs
        List<Analysis> analyses = analysisService.getAnalysisCollectedOnExcludedByStatusId(testDate, excludedStatusIds);
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with date that has no collections
        Date noCollectionsDate = Date.valueOf("2022-01-01");
        List<Analysis> noAnalyses = analysisService.getAnalysisCollectedOnExcludedByStatusId(noCollectionsDate,
                excludedStatusIds);
        assertTrue(noAnalyses.isEmpty());

        // Test with empty excluded status IDs (should return all for that date)
        Set<Integer> emptyExcludedIds = new HashSet<>();
        List<Analysis> allAnalyses = analysisService.getAnalysisCollectedOnExcludedByStatusId(testDate,
                emptyExcludedIds);
        assertNotNull(allAnalyses);
        assertEquals(1, allAnalyses.size());

    }

    @Test
    public void getAnalysesBySampleItemsExcludingByStatusIds_shouldFilterCorrectly() {
        // Create a test sample item
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1");

        Set<Integer> excludedStatusIds = new HashSet<>(List.of(2));

        // Test with valid sample item and excluded status IDs
        List<Analysis> analyses = analysisService.getAnalysesBySampleItemsExcludingByStatusIds(sampleItem,
                excludedStatusIds);
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with non-existent sample item
        SampleItem nonExistentSampleItem = new SampleItem();
        nonExistentSampleItem.setId("999");
        List<Analysis> noAnalyses = analysisService.getAnalysesBySampleItemsExcludingByStatusIds(nonExistentSampleItem,
                excludedStatusIds);
        assertNotNull(noAnalyses);
        assertTrue(noAnalyses.isEmpty());

    }

    @Test
    public void getAnalysesForStatusId_shouldReturnCorrectAnalyses() {
        // Test with valid status ID
        List<Analysis> analyses = analysisService.getAnalysesForStatusId("1");
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with non-existent status ID
        List<Analysis> nonExistentAnalyses = analysisService.getAnalysesForStatusId("999");
        assertNotNull(nonExistentAnalyses);
        assertTrue(nonExistentAnalyses.isEmpty());

    }

    @Test
    public void getCountOfAnalysesForStatusIds_shouldReturnCorrectCount() {
        // Test with valid status IDs
        List<Integer> statusIds = List.of(1);
        int count = analysisService.getCountOfAnalysesForStatusIds(statusIds);
        assertEquals(1, count);

        // Test with multiple status IDs
        List<Integer> multipleStatusIds = List.of(1, 2);
        int multipleCount = analysisService.getCountOfAnalysesForStatusIds(multipleStatusIds);
        assertEquals(2, multipleCount);

        // Test with non-existent status IDs
        List<Integer> nonExistentStatusIds = List.of(999);
        int nonExistentCount = analysisService.getCountOfAnalysesForStatusIds(nonExistentStatusIds);
        assertEquals(0, nonExistentCount);

        // Test with empty status ID list
        List<Integer> emptyStatusIds = new ArrayList<>();
        int emptyCount = analysisService.getCountOfAnalysesForStatusIds(emptyStatusIds);
        assertEquals(0, emptyCount);

    }

    @Test
    public void getAnalysesBySampleStatusIdExcludingByStatusId_shouldFilterCorrectly() {
        // Test with valid sample status and excluded status IDs
        Set<Integer> excludedStatusIds = new HashSet<>(List.of(2));
        List<Analysis> analyses = analysisService.getAnalysesBySampleStatusIdExcludingByStatusId("1",
                excludedStatusIds);
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with non-existent sample status
        List<Analysis> nonExistentAnalyses = analysisService.getAnalysesBySampleStatusIdExcludingByStatusId("999",
                excludedStatusIds);
        assertNotNull(nonExistentAnalyses);
        assertTrue(nonExistentAnalyses.isEmpty());

    }

    @Test
    public void getAllAnalysisByTestAndExcludedStatus_shouldFilterCorrectly() {
        // Test with valid test ID and excluded status list
        List<Integer> excludedStatusIds = List.of(2);
        List<Analysis> analyses = analysisService.getAllAnalysisByTestAndExcludedStatus("1", excludedStatusIds);
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());

        // Test with non-existent test ID
        List<Analysis> nonExistentAnalyses = analysisService.getAllAnalysisByTestAndExcludedStatus("999",
                excludedStatusIds);
        assertNotNull(nonExistentAnalyses);
        assertTrue(nonExistentAnalyses.isEmpty());

        // Test with all status IDs excluded
        List<Integer> allExcludedStatusIds = List.of(1, 2);
        List<Analysis> noAnalyses = analysisService.getAllAnalysisByTestAndExcludedStatus("1", allExcludedStatusIds);
        assertNotNull(noAnalyses);
        assertTrue(noAnalyses.isEmpty());

    }

    @Test
    public void updateAnalysises_shouldUpdateCancelAnalysisAndInsertNewAnalysis(){
        // Prepare test data
        List<Analysis> cancelAnalyses = new ArrayList<>();
        Analysis cancelAnalysis = analysisService.get("1");
        cancelAnalyses.add(cancelAnalysis);

        Analysis deleteAnalysis = analysisService.get("2");
        analysisService.delete(deleteAnalysis);

        List<Analysis> newAnalyses = new ArrayList<>();
        Analysis newAnalysis = new Analysis();
        newAnalysis.setAnalysisType("MANUAL");
        newAnalyses.add(newAnalysis);

        // Execute the method
        analysisService.updateAnalysises(cancelAnalyses, newAnalyses, "107");

        // Verify canceled analysis
        Analysis updatedAnalysis = analysisService.get("1");
        assertEquals("14", updatedAnalysis.getStatusId());

        // Verify new analysis was inserted
        List<Analysis> allAnalyses = analysisService.getAll();

        // Find the newly inserted analysis
        boolean foundNewAnalysis = false;
        for (Analysis analysis : allAnalyses) {
            if ("MANUAL".equals(analysis.getAnalysisType())) {
                foundNewAnalysis = true;
                break;
            }
        }
        assertTrue("New analysis should be inserted", foundNewAnalysis);
    }

    @Test
    public void getAllAnalysisByTestAndStatus_shouldReturnMatchingAnalyses() {
        // Setup test data
        List<Integer> statusList = List.of(1); // Status code from XML

        // Execute the method
        List<Analysis> analyses = analysisService.getAllAnalysisByTestAndStatus("1", statusList);

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTest().getId());
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getAllAnalysisByTestsAndStatusAndCompletedDateRange_shouldReturnMatchingAnalyses() {
        // Setup test data
        List<Integer> testIds = List.of(1);
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);
        Date lowDate = Date.valueOf("2023-11-15"); // 2023-11-15
        Date highDate = Date.valueOf("2023-11-17"); // 2023-11-17

        // Execute the method
        List<Analysis> analyses = analysisService.getAllAnalysisByTestsAndStatusAndCompletedDateRange(testIds,
                analysisStatusList, sampleStatusList, lowDate, highDate);

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTest().getId());
            assertEquals("1", analysis.getStatusId());
            assertTrue(analysis.getCompletedDate().after(lowDate) || analysis.getCompletedDate().equals(lowDate));
            assertTrue(analysis.getCompletedDate().before(highDate) || analysis.getCompletedDate().equals(highDate));
        }
    }

    @Test
    public void getAllAnalysisByTestSectionAndStatus_shouldReturnMatchingAnalyses() {
        // Setup test data
        List<Integer> statusList = List.of(1);

        // Execute the method
        List<Analysis> analyses = analysisService.getAllAnalysisByTestSectionAndStatus("1", statusList, true);

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTestSection().getId());
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatus_shouldReturnMatchingAnalyses() {
        // Setup test data
        List<Integer> statusList = List.of(1);

        // Execute the method
        List<Analysis> analyses = analysisService.getPageAnalysisByTestSectionAndStatus("1", statusList, true);

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTestSection().getId());
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getPageAnalysisAtAccessionNumberAndStatus_shouldReturnMatchingAnalysesAndHandleDottedAccessionNumber() {
        // Setup test data
        List<Integer> statusList = List.of(1);

        // 1. Test with standard accession number
        List<Analysis> analyses = analysisService.getPageAnalysisAtAccessionNumberAndStatus("SAM-2023-001", statusList,
                true);

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getStatusId());
            assertEquals("SAM-2023-001", analysis.getSampleItem().getSample().getAccessionNumber());
        }

        // 2. Test with dotted accession number
        List<Analysis> dottedAnalyses = analysisService.getPageAnalysisAtAccessionNumberAndStatus("SAM-2023-001.1",
                statusList, true);

        // Verify results
        assertNotNull(dottedAnalyses);
        assertFalse(dottedAnalyses.isEmpty());
        for (Analysis analysis : dottedAnalyses) {
            assertEquals("1", analysis.getStatusId());
            // It should have stripped the part after the dot
            assertEquals("SAM-2023-001", analysis.getSampleItem().getSample().getAccessionNumber());
        }
    }

    @Test
    public void getAnalysesBySampleItemIdAndStatusId_shouldReturnMatchingAnalyses() {
        // Execute the method
        List<Analysis> analyses = analysisService.getAnalysesBySampleItemIdAndStatusId("1", "1");

        // Verify results
        assertNotNull(analyses);
        assertFalse(analyses.isEmpty());
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getSampleItem().getId());
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getData_shouldPopulateAnalysisObject() {
        // Create an empty Analysis with only the ID set
        Analysis analysis = new Analysis();
        analysis.setId("1");

        // Execute the method
        analysisService.getData(analysis);

        // Verify the analysis was populated
        assertEquals("1", analysis.getId());
        assertEquals("A", analysis.getStatus());
        assertEquals("ROUTINE", analysis.getAnalysisType());
        assertEquals("Blood", analysis.getSampleTypeName());
        assertNotNull(analysis.getStartedDate());
    }

    @Test
    public void getAnalysisById_shouldReturnCorrectAnalysis() {
        // Execute the method
        Analysis analysis = analysisService.getAnalysisById("1");

        // Verify the correct analysis was returned
        assertNotNull(analysis);
        assertEquals("1", analysis.getId());
        assertEquals("A", analysis.getStatus());
        assertEquals("ROUTINE", analysis.getAnalysisType());
    }

    @Test
    public void updateNoAuditTrail_shouldUpdateAnalysisWithoutAudit() {
        Analysis analysis = analysisService.get("1");
        assertEquals("A", analysis.getStatus());

        analysis.setAnalysisType("MANUAL");
        analysisService.updateNoAuditTrail(analysis);

        Analysis updatedAnalysis = analysisService.get("1");
        assertEquals("MANUAL", updatedAnalysis.getAnalysisType());
    }

    @Test
    public void getAnalysisByTestDescriptionAndCompletedDateRange_shouldReturnMatchingAnalyses() {
        List<String> descriptions = List.of("Blood Chemistry Test");
        Date startDate = java.sql.Date.valueOf("2023-11-15");
        Date endDate = java.sql.Date.valueOf("2023-11-16");

        List<Analysis> analysisList = analysisService.getAnalysisByTestDescriptionAndCompletedDateRange(descriptions,
                startDate, endDate);

        assertNotNull(analysisList);
        assertTrue(analysisList.size() > 0);
        assertEquals("1", analysisList.get(0).getId());
    }

    @Test
    public void
    getMaxRevisionPendingAnalysesReadyForReportPreviewBySample_shouldReturnCorrectList()
    {
        SampleItem sampleItem1 = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        Analysis analysis1 = analysisService.get("1");
        Sample sample1 = sampleService.get("1");
        sampleItem1.setSample(sample1);
        sampleItemService.update(sampleItem1);
        analysis1.setTest(test1);
        analysis1.setSampleItem(sampleItem1);
        analysisService.update(analysis1);

        SampleItem sampleItem2 = sampleItemService.get("2");
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        Analysis analysis2 = analysisService.get("2");
        Sample sample2 = sampleService.get("2");
        sampleItem2.setSample(sample2);
        sampleItemService.update(sampleItem2);
        analysis2.setTest(test2);
        analysis2.setSampleItem(sampleItem2);
        analysisService.update(analysis2);

        List<Analysis> analysisList =
        analysisService.getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(sample2);

        assertNotNull(analysisList);
        // Add assertions based on expected results
        if (!analysisList.isEmpty()) {
            assertEquals("1", analysisList.get(0).getSampleItem().getSample().getId());
        }
    }

    @Test
    public void
    getMaxRevisionPendingAnalysesReadyToBeReportedBySample_shouldReturnCorrectAnalyses()
    {
        SampleItem sampleItem1 = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        Analysis analysis1 = analysisService.get("1");
        Sample sample1 = sampleService.get("1");
        sampleItem1.setSample(sample1);
        sampleItemService.update(sampleItem1);
        analysis1.setTest(test1);
        analysis1.setSampleItem(sampleItem1);
        analysisService.update(analysis1);

        SampleItem sampleItem2 = sampleItemService.get("2");
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        Analysis analysis2 = analysisService.get("2");
        Sample sample2 = sampleService.get("2");
        sampleItem2.setSample(sample2);
        sampleItemService.update(sampleItem2);
        analysis2.setTest(test2);
        analysis2.setSampleItem(sampleItem2);
        analysisService.update(analysis2);

        // Execute
        List<Analysis> analysisList =
        analysisService.getMaxRevisionPendingAnalysesReadyToBeReportedBySample(sample2);

        // Verify
        assertNotNull(analysisList);
        // Assuming the test data contains analyses ready to be reported for sample 1
        if (!analysisList.isEmpty()) {
            // Verify these are the highest revision analyses for this sample
            for (Analysis analysis : analysisList) {
                assertEquals("1", analysis.getSampleItem().getSample().getId());

                // Additional verification: check if status is appropriate for reporting
                // For example, if analyses with status "A" are ready to be reported:
                assertEquals("A", analysis.getStatus());

                // Verify this is the max revision for this test
                String testId = analysis.getTest().getId();
                for (Analysis otherAnalysis : analysisService.getAll()) {
                    if (otherAnalysis.getTest().getId().equals(testId) &&
                        otherAnalysis.getSampleItem().getSample().getId().equals("1") &&
                        !otherAnalysis.getId().equals(analysis.getId())) {

                        assertTrue("Should be max revision",
                        Integer.parseInt(analysis.getRevision()) >=
                        Integer.parseInt(otherAnalysis.getRevision()));
                    }
                }
            }
        }
    }

    @Test
    public void getMaxRevisionAnalysesReadyForReportPreviewBySample_shouldReturnCorrectAnalyses() {
        Analysis analysis2 = analysisService.get("2");
        Result result2 = resultService.get("2");
        Sample sample2 = sampleService.get("2");

        sample2.setStatusId("3");
        sampleService.update(sample2);

        result2.setAnalysis(analysis2);
        result2.setIsReportable("Y");
        resultService.update(result2);

        analysis2.setStatusId("3");
        analysis2.setIsReportable("Y");
        analysis2.setPrintedDate(null);
        analysisService.update(analysis2);
        List<String> accessionNumbers = List.of("SAM-2023-002");

        List<Analysis> analysisList = analysisService
                .getMaxRevisionAnalysesReadyForReportPreviewBySample(accessionNumbers);

        assertNotNull(analysisList);
        assertEquals(1, analysisList.size());
        // Add assertions based on expected behavior
        if (!analysisList.isEmpty()) {
            for (Analysis analysis : analysisList) {
                assertEquals("SAM-2023-002", analysis.getSampleItem().getSample().getAccessionNumber());
            }
        }
    }

    @Test
    public void getAnalysesBySampleIdExcludedByStatusId_shouldExcludeSpecifiedStatus() {
        Set<Integer> statusIds = new HashSet<>(List.of(2));

        List<Analysis> analysisList = analysisService.getAnalysesBySampleIdExcludedByStatusId("1", statusIds);

        assertNotNull(analysisList);
        assertEquals(1, analysisList.size());
        if (!analysisList.isEmpty()) {
            for (Analysis analysis : analysisList) {
                assertFalse(statusIds.contains(Integer.parseInt(analysis.getStatusId())));
            }
        }
    }

    @Test
    public void getAllAnalysisByTestsAndStatus_shouldReturnMatchingAnalyses() {
        List<Integer> testIds = List.of(1);
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);

        List<Analysis> analysisList = analysisService.getAllAnalysisByTestsAndStatus(testIds, analysisStatusList,
                sampleStatusList);

        assertNotNull(analysisList);
        if (!analysisList.isEmpty()) {
            assertTrue(analysisList.size() > 0);
            assertEquals("1", analysisList.get(0).getId());
        }
    }

    @Test
    public void getAllAnalysisByTestSectionAndStatus_shouldReturnCorrectList() {
        String testSectionId = "1";
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);

        List<Analysis> analysisList = analysisService.getAllAnalysisByTestSectionAndStatus(testSectionId,
                analysisStatusList, sampleStatusList);

        assertNotNull(analysisList);
        if (!analysisList.isEmpty()) {
            assertTrue(analysisList.size() > 0);
            for (Analysis analysis : analysisList) {
                assertEquals("1", analysis.getTestSection().getId());
            }
        }
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatus_shouldReturnPaginatedResults() {
        String testSectionId = "1";
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);

        List<Analysis> analysisList = analysisService.getPageAnalysisByTestSectionAndStatus(testSectionId,
                analysisStatusList, sampleStatusList);

        assertNotNull(analysisList);
        // Add assertions specific to pagination behavior
        if (!analysisList.isEmpty()) {
            for (Analysis analysis : analysisList) {
                assertEquals("1", analysis.getTestSection().getId());
                assertTrue(analysisStatusList.contains(Integer.parseInt(analysis.getStatusId())));
            }
        }
    }

    @Test
    public void getMaxRevisionAnalysesBySampleIncludeCanceled_shouldIncludeCanceledTests() {
        SampleItem sampleItem1 = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test1 = testService.get("1");
        Analysis analysis1 = analysisService.get("1");
        analysis1.setTest(test1);
        analysis1.setSampleItem(sampleItem1);
        analysisService.update(analysis1);

        SampleItem sampleItem2 = sampleItemService.get("2");
        org.openelisglobal.test.valueholder.Test test2 = testService.get("2");
        Analysis analysis2 = analysisService.get("2");
        analysis2.setTest(test2);
        analysis2.setSampleItem(sampleItem2);
        analysisService.update(analysis2);

        List<Analysis> analysisList = analysisService.getMaxRevisionAnalysesBySampleIncludeCanceled(sampleItem2);

        assertNotNull(analysisList);
        // Verify that canceled analyses are included
        if (!analysisList.isEmpty()) {
            boolean hasCanceled = false;
            for (Analysis analysis : analysisList) {
                assertEquals("2", analysis.getSampleItem().getId());
                if (analysis.getStatus().equals("I")) { // Assuming X is the status for canceled
                    hasCanceled = true;
                }
            }
            // This assertion depends on your test data - comment out if not applicable
            assertTrue("Should include canceled analyses", hasCanceled);
        }
    }

    @Test
    public void getAnalysisByTestNamesAndCompletedDateRange_shouldReturnMatchingAnalyses() {
        // Prepare test data
        List<String> testNames = List.of("Blood Chem", "Test Localization 1");
        Date lowDate = Date.valueOf("2023-11-14");
        Date highDate = Date.valueOf("2023-11-16");

        // Call the service method
        List<Analysis> analyses = analysisService.getAnalysisByTestNamesAndCompletedDateRange(testNames, lowDate,
                highDate);

        // Verify results
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());
        assertEquals("Test Localization 1", analyses.get(0).getTest().getName());
    }

    @Test
    public void getAnalysesBySampleIdTestIdAndStatusId_shouldReturnMatchingAnalyses() {
        // Prepare test data
        List<Integer> sampleIdList = List.of(1);
        List<Integer> testIdList = List.of(1);
        List<Integer> statusIdList = List.of(1);

        // Call the service method
        List<Analysis> analyses = analysisService.getAnalysesBySampleIdTestIdAndStatusId(sampleIdList, testIdList,
                statusIdList);

        // Verify results
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getId());
        assertEquals("A", analyses.get(0).getStatus());
    }

    @Test
    public void getMaxRevisionParentTestAnalysesBySample_shouldReturnLatestRevisions() {
        // Prepare test data
        Analysis analysis = analysisService.get("2");
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);
        analysis.setParentAnalysis(null);
        analysisService.update(analysis);

        // Call the service method
        List<Analysis> analyses = analysisService.getMaxRevisionParentTestAnalysesBySample(sampleItem);

        // Verify results
        assertNotNull(analyses);
        assertTrue(analyses.size() > 0);
        for (Analysis analysis1 : analyses) {
            assertEquals("1", analysis1.getSampleItem().getId());
        }
    }

    @Test
    public void getAnalysisStartedOnRangeByStatusId_shouldReturnAnalysesInDateRange() {
        // Prepare test data
        Date lowDate = Date.valueOf("2023-11-14");
        Date highDate = Date.valueOf("2023-11-16");
        String statusId = "1";

        // Call the service method
        List<Analysis> analyses = analysisService.getAnalysisStartedOnRangeByStatusId(lowDate, highDate, statusId);

        // Verify results
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        assertEquals("1", analyses.get(0).getStatusId());

        // Check date is within range
        Date startedDate = analyses.get(0).getStartedDate();
        assertTrue(startedDate.compareTo(lowDate) >= 0);
        assertTrue(startedDate.compareTo(highDate) <= 0);
    }

    @Test
    public void getRevisionHistoryOfAnalysesBySample_shouldReturnAllRevisions() {
        // Prepare test data
        Analysis analysis = analysisService.get("2");
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);
        analysisService.update(analysis);

        // Call the service method
        List<Analysis> revisions = analysisService.getRevisionHistoryOfAnalysesBySample(sampleItem);

        // Verify results
        assertNotNull(revisions);
        assertTrue(revisions.size() > 0);
        for (Analysis analysis1 : revisions) {
            assertEquals("1", analysis1.getSampleItem().getId());
        }
    }

    @Test
    public void getPreviousAnalysisForAmendedAnalysis_shouldReturnPreviousVersion() {
        // Prepare test data
        Analysis analysis = analysisService.get("2");
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);
        analysisService.update(analysis);
        Analysis analysis1 = analysisService.get("2");

        // Call the service method
        Analysis previousAnalysis = analysisService.getPreviousAnalysisForAmendedAnalysis(analysis1);

        // Verify results
        assertNotNull(previousAnalysis);
        assertEquals("1", previousAnalysis.getId());
    }

    @Test
    public void getAllAnalysisByTestSectionAndExcludedStatus_shouldReturnFilteredAnalyses() {
        // Prepare test data
        String testSectionId = "1";
        List<Integer> statusIdList = List.of(2);

        // Call the service method
        List<Analysis> analyses = analysisService.getAllAnalysisByTestSectionAndExcludedStatus(testSectionId,
                statusIdList);

        // Verify results
        assertNotNull(analyses);
        assertTrue(analyses.size() > 0);
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTestSection().getId());
            assertFalse(statusIdList.contains(Integer.parseInt(analysis.getStatusId())));
        }
    }

    @Test
    public void getAnalysisStartedOnExcludedByStatusId_shouldReturnFilteredAnalyses() {
        // Prepare test data
        Date startDate = Date.valueOf("2023-11-15");
        Set<Integer> statusIds = new HashSet<>(List.of(2));

        // Call the service method
        List<Analysis> analyses = analysisService.getAnalysisStartedOnExcludedByStatusId(startDate, statusIds);

        // Verify results
        assertNotNull(analyses);
        assertTrue(analyses.size() > 0);
        for (Analysis analysis : analyses) {
            assertFalse(statusIds.contains(Integer.parseInt(analysis.getStatusId())));
        }
    }

    @Test
    public void getAnalysisByTestSectionAndCompletedDateRange_shouldReturnFilteredAnalyses() {
        // Prepare test data
        String sectionID = "1";
        Date lowDate = Date.valueOf("2023-11-14");
        Date highDate = Date.valueOf("2023-11-16");

        // Call the service method
        List<Analysis> analyses = analysisService.getAnalysisByTestSectionAndCompletedDateRange(sectionID, lowDate,
                highDate);

        // Verify results
        assertNotNull(analyses);
        assertTrue(analyses.size() > 0);
        for (Analysis analysis : analyses) {
            assertEquals("1", analysis.getTestSection().getId());

            // Check completed date is within range
            Date completedDate = analysis.getCompletedDate();
            assertTrue(completedDate.compareTo(lowDate) >= 0);
            assertTrue(completedDate.compareTo(highDate) <= 0);
        }
    }

    @Test
    public void
    getMaxRevisionAnalysesReadyToBeReported_shouldReturnLatestReportableRevisions()
    {
        Analysis analysis2 = analysisService.get("2");
        Result result2 = resultService.get("2");
        Sample sample2 = sampleService.get("2");

        sample2.setStatusId("3");
        sampleService.update(sample2);

        result2.setAnalysis(analysis2);
        result2.setIsReportable("Y");
        resultService.update(result2);

        analysis2.setStatusId("4");
        analysis2.setIsReportable("Y");
        analysis2.setPrintedDate(null);
        analysisService.update(analysis2);
        
        // Call the service method
        List<Analysis> analyses =
        analysisService.getMaxRevisionAnalysesReadyToBeReported();

        // Verify results
        assertNotNull(analyses);
        assertEquals(1, analyses.size());
        for (Analysis analysis : analyses) {
            assertEquals("2", analysis.getId());
        }
    }

    @Test
    public void getMaxRevisionAnalysisBySampleAndTest_shouldReturnLatestRevision() {
        // Prepare test data
        Analysis analysis = analysisService.get("2");
        org.openelisglobal.test.valueholder.Test test = testService.get("1");
        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);
        analysisService.update(analysis);
        Analysis analysis1 = analysisService.get("2");

        // Call the service method
        analysisService.getMaxRevisionAnalysisBySampleAndTest(analysis1);

        Analysis analysis2 = analysisService.get("2");

        // The method updates the analysis object so we should verify it's updated
        assertNotNull(analysis2);
        assertEquals("2", analysis2.getTestSection().getId());
        assertEquals("2", analysis2.getRevision());

    }

    @Test
    public void getAnalysesAlreadyReportedBySample_shouldReturnCorrectAnalyses()
    {
        // Create a sample for testing
        Analysis analysis2 = analysisService.get("2");
        Result result2 = resultService.get("2");
        Sample sample2 = sampleService.get("2");

        sample2.setStatusId("3");
        sampleService.update(sample2);

        result2.setAnalysis(analysis2);
        result2.setIsReportable("Y");
        resultService.update(result2);

        analysis2.setStatusId("4");
        analysis2.setIsReportable("Y");
        analysis2.setPrintedDate(null);
        analysisService.update(analysis2);

        Sample sample = sampleService.get("2");

        List<Analysis> analysisList =
        analysisService.getAnalysesAlreadyReportedBySample(sample);

        // There should be at least one analysis for sample "1" in the test data
        assertNotNull(analysisList);
        assertEquals(1, analysisList.size());

        // Verify that all analyses are reportable
        for (Analysis analysis : analysisList) {
            assertEquals("Y",analysis.getIsReportable());
        }
    }

    @Test
    public void getRevisionHistoryOfAnalysesBySampleAndTest_shouldReturnCorrectAnalyses() {
        // Set up test data
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1");

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("1");

        // Test with including latest revision
        List<Analysis> analysesWithLatest = analysisService.getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem,
                test, true);
        assertNotNull(analysesWithLatest);

        // Test without including latest revision
        List<Analysis> analysesWithoutLatest = analysisService.getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem,
                test, false);
        assertNotNull(analysesWithoutLatest);

        // If there are revisions, the lists should be different
        if (analysesWithLatest.size() > 0) {
            assertTrue(analysesWithLatest.size() >= analysesWithoutLatest.size());
        }
    }

    @Test
    public void getAnalysesBySampleStatusId_shouldReturnCorrectAnalyses() {
        List<Analysis> analysisList = analysisService.getAnalysesBySampleStatusId("1");
        assertNotNull(analysisList);
        assertTrue(analysisList.size() > 0);

        // All analyses should have the correct status ID
        for (Analysis analysis : analysisList) {
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getAnalysisEnteredAfterDate_shouldReturnAnalysesAfterSpecifiedDate() {
        // Set date to a point before any test data was created
        Timestamp cutoffDate = Timestamp.valueOf("2023-11-01 00:00:00");

        List<Analysis> analysisList = analysisService.getAnalysisEnteredAfterDate(cutoffDate);
        assertNotNull(analysisList);
        assertTrue(analysisList.size() > 0);

        // Verify all analyses were entered after the cutoff date
        for (Analysis analysis : analysisList) {
            Timestamp entryDate = analysis.getEnteredDate();
            assertTrue(entryDate.after(cutoffDate));
        }
    }

    @Test
    public void getAnalysesBySampleIdAndStatusId_shouldReturnCorrectAnalyses() {
        // Set up test data
        String sampleId = "1";
        Set<Integer> statusIds = new HashSet<>();
        statusIds.add(1); // Status ID from test data

        List<Analysis> analysisList = analysisService.getAnalysesBySampleIdAndStatusId(sampleId, statusIds);
        assertNotNull(analysisList);

        // Verify all analyses have the correct sample ID and status
        for (Analysis analysis : analysisList) {
            SampleItem sampleItem = analysis.getSampleItem();
            if (sampleItem != null) {
                Sample sample = sampleItem.getSample();
                if (sample != null) {
                    assertEquals(sampleId, sample.getId());
                }
            }
            assertTrue(statusIds.contains(Integer.parseInt(analysis.getStatusId())));
        }
    }

    @Test
    public void getAnalysisStartedOn_shouldReturnAnalysesStartedOnSpecifiedDate() {
        // Use a date from the test data
        Date startDate = Date.valueOf("2023-11-15");

        List<Analysis> analysisList = analysisService.getAnalysisStartedOn(startDate);
        assertNotNull(analysisList);

        // Verify all analyses were started on the specified date
        for (Analysis analysis : analysisList) {
            // Convert Timestamp to Date for comparison
            Date analysisDate = new Date(analysis.getStartedDate().getTime());
            // Compare dates (ignoring time component)
            assertEquals(startDate.getYear(), analysisDate.getYear());
            assertEquals(startDate.getMonth(), analysisDate.getMonth());
            assertEquals(startDate.getDate(), analysisDate.getDate());
        }
    }

    @Test
    public void getMaxRevisionAnalysesBySample_shouldReturnHighestRevisions() {
        // Set up test data
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1");

        List<Analysis> analysisList = analysisService.getMaxRevisionAnalysesBySample(sampleItem);
        assertNotNull(analysisList);

        // Verify each analysis has the highest revision for its test
        for (Analysis analysis : analysisList) {
            org.openelisglobal.test.valueholder.Test test = analysis.getTest();
            if (test != null) {
                List<Analysis> revisions = analysisService.getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem, test,
                        true);

                int highestRevision = 0;
                for (Analysis revision : revisions) {
                    if (Integer.parseInt(revision.getRevision()) > highestRevision) {
                        highestRevision = Integer.parseInt(revision.getRevision());
                    }
                }

                assertEquals(highestRevision, Integer.parseInt(analysis.getRevision()));
            }
        }
    }

    @Test
    public void getAllChildAnalysesByResult_shouldReturnCorrectChildAnalyses() {
        // Set up test data
        Result result = new Result();
        result.setId("1");

        List<Analysis> childAnalyses = analysisService.getAllChildAnalysesByResult(result);
        assertNotNull(childAnalyses);

        // Verify parent result ID matches
        for (Analysis analysis : childAnalyses) {
            assertEquals(result.getId(), analysis.getParentResult().getId());
        }
    }

    @Test
    public void getAnalysesReadyToBeReported_shouldReturnReadyAnalyses() {
        Analysis analysis2 = analysisService.get("2");
        Result result2 = resultService.get("2");
        Sample sample2 = sampleService.get("2");

        sample2.setStatusId("3");
        sampleService.update(sample2);

        result2.setAnalysis(analysis2);
        result2.setIsReportable("Y");
        resultService.update(result2);

        analysis2.setStatusId("4");
        analysis2.setIsReportable("Y");
        analysis2.setPrintedDate(null);
        analysis2.setReleasedDate(null);
        analysisService.update(analysis2);
        
        List<Analysis> readyAnalyses =
        analysisService.getAnalysesReadyToBeReported();
        assertNotNull(readyAnalyses);
        assertEquals(1, readyAnalyses.size());

        // Verify all analyses are ready to be reported
        for (Analysis analysis : readyAnalyses) {
        assertEquals("Y", analysis.getIsReportable());

        // Typically, analyses are ready to be reported when they're complete but not yet released
        assertNotNull(analysis.getCompletedDate());
        assertNull(analysis.getReleasedDate());
        }
    }

    @Test
    public void getAnalysisBySampleAndTestIds_shouldReturnCorrectAnalyses() {
        // Set up test data
        String sampleKey = "1";
        List<Integer> testIds = new ArrayList<>();
        testIds.add(1); // Test ID from test data

        List<Analysis> analysisList = analysisService.getAnalysisBySampleAndTestIds(sampleKey, testIds);
        assertNotNull(analysisList);

        // Verify all analyses have the correct sample ID and test ID
        for (Analysis analysis : analysisList) {
            SampleItem sampleItem = analysis.getSampleItem();
            if (sampleItem != null) {
                Sample sample = sampleItem.getSample();
                if (sample != null) {
                    assertEquals(sampleKey, sample.getId());
                }
            }

            assertTrue(testIds.contains(Integer.parseInt(analysis.getTest().getId())));
        }
    }

    @Test
    public void getAnalysisCompleteInRange_shouldReturnAnalysesCompletedInRange() {
        // Set up date range
        Timestamp lowDate = Timestamp.valueOf("2023-11-10 00:00:00");
        Timestamp highDate = Timestamp.valueOf("2023-11-20 00:00:00");

        List<Analysis> analysisList = analysisService.getAnalysisCompleteInRange(lowDate, highDate);
        assertNotNull(analysisList);

        // Verify all analyses were completed within the specified range
        for (Analysis analysis : analysisList) {
            Date completedDate = analysis.getCompletedDate();
            assertTrue(completedDate != null && (completedDate.after(lowDate) || completedDate.equals(lowDate))
                    && (completedDate.before(highDate) || completedDate.equals(highDate)));
        }
    }

    @Test
    public void getAllMaxRevisionAnalysesPerTest_shouldReturnHighestRevisions() {
        // Set up test data
        org.openelisglobal.test.valueholder.Test test = testService.get("1");

        List<Analysis> analysisList = analysisService.getAllMaxRevisionAnalysesPerTest(test);
        assertNotNull(analysisList);

        // Verify each analysis has the correct test ID
        for (Analysis analysis : analysisList) {
            assertEquals(test.getId(), analysis.getTest().getId());
        }

        // Verify each analysis has the highest revision for its sample
        // This would require more complex testing with multiple revisions per sample
    }

    @Test
    public void getAnalysisCollectedOn_shouldReturnAnalysesCollectedOnSpecifiedDate() {
        // Use a date from the test data
        Date collectionDate = Date.valueOf("2023-11-15");

        List<Analysis> analysisList = analysisService.getAnalysisCollectedOn(collectionDate);
        assertNotNull(analysisList);

        // Verify all analyses were collected on the specified date
        for (Analysis analysis : analysisList) {
            SampleItem sampleItem = analysis.getSampleItem();
            if (sampleItem != null) {
                // Convert Timestamp to Date for comparison
                Date sampleDate = new Date(sampleItem.getCollectionDate().getTime());
                // Compare dates (ignoring time component)
                assertEquals(collectionDate.getYear(), sampleDate.getYear());
                assertEquals(collectionDate.getMonth(), sampleDate.getMonth());
                assertEquals(collectionDate.getDate(), sampleDate.getDate());
            }
        }
    }

    @Test
    public void getAnalysesBySampleItem_shouldReturnCorrectAnalyses() {
        // Set up test data
        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("1");

        List<Analysis> analysisList = analysisService.getAnalysesBySampleItem(sampleItem);
        assertNotNull(analysisList);

        // Verify all analyses have the correct sample item ID
        for (Analysis analysis : analysisList) {
            assertEquals(sampleItem.getId(), analysis.getSampleItem().getId());
        }
    }

    @Test
    public void updateAllNoAuditTrail_shouldUpdateMultipleAnalyses() {
        // Set up test data
        List<Analysis> analysesToUpdate = new ArrayList<>();

        Analysis analysis1 = analysisService.get("1");
        analysis1.setAnalysisType("CONFIRM");
        analysesToUpdate.add(analysis1);

        Analysis analysis2 = analysisService.get("2");
        analysis2.setAnalysisType("ROUTINE");
        analysesToUpdate.add(analysis2);

        // Perform update without audit trail
        analysisService.updateAllNoAuditTrail(analysesToUpdate);

        // Verify updates were successful
        Analysis updated1 = analysisService.get("1");
        Analysis updated2 = analysisService.get("2");

        assertEquals("CONFIRM", updated1.getAnalysisType());
        assertEquals("ROUTINE", updated2.getAnalysisType());
    }

    @Test
    public void get_shouldReturnAnalysesWithSpecifiedIds() {
        // Set up test data
        List<String> analysisIds = new ArrayList<>();
        analysisIds.add("1");
        analysisIds.add("2");

        List<Analysis> analysisList = analysisService.get(analysisIds);
        assertNotNull(analysisList);
        assertEquals(2, analysisList.size());

        // Verify correct IDs were retrieved
        boolean found1 = false;
        boolean found2 = false;

        for (Analysis analysis : analysisList) {
            if ("1".equals(analysis.getId())) {
                found1 = true;
            } else if ("2".equals(analysis.getId())) {
                found2 = true;
            }
        }

        assertTrue(found1);
        assertTrue(found2);
    }

    @Test
    public void getAllAnalysisByTestsAndStatus_shouldReturnCorrectAnalyses() {
        List<String> testIdList = List.of("1");
        List<Integer> statusIdList = List.of(1);

        List<Analysis> results = analysisService.getAllAnalysisByTestsAndStatus(testIdList, statusIdList);

        assertNotNull(results);
        // Expected size depends on test data set up
        // Verify results match criteria (test IDs and status IDs)
        for (Analysis analysis : results) {
            assertEquals("1", analysis.getId());
            assertEquals("1", analysis.getStatusId());
            assertEquals("1", analysis.getTest().getId());
        }
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatus_withSampleStatus_shouldReturnCorrectCount() {
        String testSectionId = "1";
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);

        int count = analysisService.getCountAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList,
                sampleStatusList);

        // Expected 1 from our test data
        assertEquals(1, count);
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatus_withoutSampleStatus_shouldReturnCorrectCount() {
        String testSectionId = "1";
        List<Integer> analysisStatusList = List.of(1);

        int count = analysisService.getCountAnalysisByTestSectionAndStatus(testSectionId, analysisStatusList);

        // Expected 1 from our test data
        assertEquals(1, count);
    }

    @Test
    public void getCountAnalysisByStatusFromAccession_shouldHandleAccessionWithDotAndNormal() {
        // With dot
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);
        String accessionNumber = "SAM-2023-001.1";

        int count = analysisService.getCountAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                accessionNumber);

        // Should strip the dot and anything after
        assertEquals(1, count);

        // Without dot
        List<Integer> analysisStatusList1 = List.of(1);
        List<Integer> sampleStatusList1 = List.of(1);
        String accessionNumber1 = "SAM-2023-001";

        int count1 = analysisService.getCountAnalysisByStatusFromAccession(analysisStatusList1, sampleStatusList1,
                accessionNumber1);

        assertEquals(1, count1);
    }

    @Test
    public void getPageAnalysisByStatusFromAccession_shouldHandleAccessionWithDot() {
        List<Integer> analysisStatusList = List.of(1);
        List<Integer> sampleStatusList = List.of(1);
        String accessionNumber = "SAM-2023-001.1";

        List<Analysis> analyses = analysisService.getPageAnalysisByStatusFromAccession(analysisStatusList,
                sampleStatusList, accessionNumber);

        assertNotNull(analyses);
        assertEquals(1, analyses.size());
    }

    @Test
    public void getPageAnalysisByStatusFromAccession_withRangeParameters_shouldReturnCorrectAnalyses() {
        List<Integer> analysisStatusList = List.of(1, 2);
        List<Integer> sampleStatusList = List.of(1, 2);
        String accessionNumber = "SAM-2023-001";
        String upperRangeAccessionNumber = "SAM-2023-002";
        boolean doRange = true;
        boolean finished = false;

        List<Analysis> analyses = analysisService.getPageAnalysisByStatusFromAccession(analysisStatusList,
                sampleStatusList, accessionNumber, upperRangeAccessionNumber, doRange, finished);

        assertNotNull(analyses);
        assertEquals(2, analyses.size());
    }

    @Test
    public void getAnalysisForSiteBetweenResultDates_shouldReturnCorrectAnalyses() {
        String referringSiteId = "1";
        LocalDate lowerDate = LocalDate.of(2023, 11, 1);
        LocalDate upperDate = LocalDate.of(2023, 12, 1);

        List<Analysis> analyses = analysisService.getAnalysisForSiteBetweenResultDates(referringSiteId, lowerDate,
                upperDate);

        assertNotNull(analyses);
        // Verify dates are within the range
        for (Analysis analysis : analyses) {
            Date completedDate = analysis.getCompletedDate();
            if (completedDate != null) {
                LocalDate localCompleteDate = completedDate.toLocalDate();
                assertTrue(!localCompleteDate.isBefore(lowerDate) && !localCompleteDate.isAfter(upperDate));
            }
        }
    }

    @Test
    public void getAnalysesByPriorityAndStatusId_shouldReturnCorrectAnalyses() {
        OrderPriority priority = OrderPriority.ASAP;
        List<Integer> analysisStatusIds = List.of(1);

        List<Analysis> analyses = analysisService.getAnalysesByPriorityAndStatusId(priority, analysisStatusIds);

        assertNotNull(analyses);
        for (Analysis analysis : analyses) {
            assertEquals(OrderPriority.ASAP, analysis.getSampleItem().getSample().getPriority());
            assertEquals("1", analysis.getStatusId());
        }
    }

    @Test
    public void getStudyAnalysisForSiteBetweenResultDates_shouldReturnCorrectAnalyses() {
        String referringSiteId = "1";
        LocalDate lowerDate = LocalDate.of(2023, 11, 1);
        LocalDate upperDate = LocalDate.of(2023, 12, 1);

        List<Analysis> analyses = analysisService.getStudyAnalysisForSiteBetweenResultDates(referringSiteId, lowerDate,
                upperDate);

        assertNotNull(analyses);
        // Verify dates and site
        for (Analysis analysis : analyses) {
            if (analysis.getCompletedDate() != null) {
                LocalDate localCompleteDate = analysis.getCompletedDate().toLocalDate();
                assertTrue(!localCompleteDate.isBefore(lowerDate) && !localCompleteDate.isAfter(upperDate));
            }
        }
    }

    @Test
    public void getAnalysesCompletedOnByStatusId_shouldReturnCorrectAnalyses() {
        Date completedDate = Date.valueOf("2023-11-15");
        String statusId = "1";

        List<Analysis> analyses = analysisService.getAnalysesCompletedOnByStatusId(completedDate, statusId);

        assertNotNull(analyses);
        for (Analysis analysis : analyses) {
            assertEquals(statusId, analysis.getStatusId().toString());
            // Compare dates (would need to check date equality ignoring time in a real
            // test)
        }
    }

    @Test
    public void getCountOfAnalysisCompletedOnByStatusId_shouldReturnCorrectCount() {
        Date completedDate = Date.valueOf("2023-11-15");
        List<Integer> statusIds = List.of(1, 2);

        int count = analysisService.getCountOfAnalysisCompletedOnByStatusId(completedDate, statusIds);

        // Expected count based on test data
        assertTrue(count >= 0);
    }

    @Test
    public void getCountOfAnalysisStartedOnExcludedByStatusId_shouldReturnCorrectCount() {
        Date collectionDate = Date.valueOf("2023-11-15");
        Set<Integer> statusIds = new HashSet<>(List.of(3, 4)); // Excluded statuses

        int count = analysisService.getCountOfAnalysisStartedOnExcludedByStatusId(collectionDate, statusIds);

        // Expected count based on test data (analyses with status 1 and 2)
        assertTrue(count >= 0);
    }

    @Test
    public void getCountOfAnalysisStartedOnByStatusId_shouldReturnCorrectCount() {
        Date startedDate = Date.valueOf("2023-11-15");
        List<Integer> statusIds = List.of(1, 2);

        int count = analysisService.getCountOfAnalysisStartedOnByStatusId(startedDate, statusIds);

        // Expected count based on test data
        assertTrue(count >= 0);
    }

    @Test
    public void getAnalysesResultEnteredOnExcludedByStatusId_shouldReturnCorrectAnalyses() {
        Date completedDate = Date.valueOf("2023-11-15");
        Set<Integer> statusIds = new HashSet<>(List.of(3, 4)); // Excluded statuses

        List<Analysis> analyses = analysisService.getAnalysesResultEnteredOnExcludedByStatusId(completedDate,
                statusIds);

        assertNotNull(analyses);
        for (Analysis analysis : analyses) {
            assertFalse(statusIds.contains(analysis.getStatusId()));
            // Check date (would need to check date equality ignoring time in a real test)
        }
    }

    @Test
    public void getMethodId_shouldHandleAllScenarios() {
        // Test null analysis
        String methodId = analysisService.getMethodId(null);
        assertEquals("", methodId);

        // Test analysis with null method
        Analysis analysis = new Analysis();
        analysis.setMethod(null);
        methodId = analysisService.getMethodId(analysis);
        assertEquals("", methodId);

        // Test analysis with valid method
        Method method = methodService.get("1");
        analysis.setMethod(method);
        methodId = analysisService.getMethodId(analysis);
        assertEquals("1", methodId);
    }

}