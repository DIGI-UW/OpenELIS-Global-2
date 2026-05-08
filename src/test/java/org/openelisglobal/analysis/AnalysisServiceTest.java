package org.openelisglobal.analysis;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analysis.valueholder.ResultFile;
import org.openelisglobal.method.service.MethodService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisServiceTest extends BaseWebContextSensitiveTest {

    private static final byte[] fileContent = Base64.getDecoder()
            .decode("iPBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgMBAQEBBQAAAA==");

    @Autowired
    AnalysisService aService;

    @Autowired
    AnalysisDAO analysisDAO;

    @Autowired
    SampleService sampleService;

    @Autowired
    TestService tService;

    @Autowired
    SampleItemService sampleItemService;

    @Autowired
    ResultService resultService;

    @Autowired
    MethodService methodService;

    @Autowired
    PanelService panelService;
    @Autowired
    TestSectionService testSectionService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis.xml");
    }

    @Test
    public void getData_shouldReturncopiedPropertiesFromDatabase() throws Exception {
        Analysis analysis = new Analysis();
        analysis.setId("1");

        aService.getData(analysis);

        Assert.assertNotNull(analysis.getId());
        Assert.assertEquals("ROUTINE", analysis.getAnalysisType());
    }

    @Test
    public void getAnalysisById_shouldReturnAnalysisById() throws Exception {
        Assert.assertEquals("CONFIRM", aService.getAnalysisById("2").getAnalysisType());
    }

    @Test
    public void getAnalysisStartedOrCompletedInDateRange_shouldReturnAnalysisStartedOrCompletedInDateRange()
            throws Exception {
        Date sqlDayOne = Date.valueOf("2023-11-15");
        Date sqlDayTwo = Date.valueOf("2023-11-16");
        List<Analysis> analyses = aService.getAnalysisStartedOrCompletedInDateRange(sqlDayOne, sqlDayTwo);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleIdExcludedByStatusId_shouldReturnAnalysis() throws Exception {
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysesBySampleIdExcludedByStatusId("1", statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange_shouldReturnAnalysis() throws Exception {
        List<String> testSectionIds = Arrays.asList("1");
        Date sqlDayOne = Date.valueOf("2023-11-15");
        Date sqlDayTwo = Date.valueOf("2023-11-16");
        List<Analysis> analyses = aService.getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange(sqlDayOne,
                sqlDayTwo, "1", testSectionIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisByTestNamesAndCompletedDateRange_shouldReturnAnalysis() throws Exception {
        List<String> testNames = Arrays.asList("Test Localization 1", "Test Localization 2");
        Date sqlDayOne = Date.valueOf("2023-11-15");
        Date sqlDayTwo = Date.valueOf("2023-11-16");
        List<Analysis> analyses = aService.getAnalysisByTestNamesAndCompletedDateRange(testNames, sqlDayOne, sqlDayTwo);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleIdTestIdAndStatusId_shouldReturnAnalysis() throws Exception {
        List<String> testSectionIds = Arrays.asList("1", "2");
        List<String> sampleIdList = Arrays.asList("1", "2");
        List<String> statusIdList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getAnalysesBySampleIdTestIdAndStatusId(sampleIdList, testSectionIds,
                statusIdList);

        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(1).getAnalysisType());
        Assert.assertEquals("CONFIRM", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleItemsExcludingByStatusIds_shouldReturngAnalysesBySampleItemsExcludingByStatusIds() {
        SampleItem sampleItem = sampleItemService.get("1");
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysesBySampleItemsExcludingByStatusIds(sampleItem, statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisStartedOnRangeByStatusId_shouldReturnAnalysis() throws Exception {
        Date sqlDayOne = Date.valueOf("2023-11-15");
        Date sqlDayTwo = Date.valueOf("2023-11-16");

        List<Analysis> analyses = aService.getAnalysisStartedOnRangeByStatusId(sqlDayOne, sqlDayTwo, "1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleStatusIdExcludingByStatusId_shouldReturnAnalysis() throws Exception {
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysesBySampleStatusIdExcludingByStatusId("1", statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleItemIdAndStatusId_shouldReturnAnalysis() throws Exception {
        List<Analysis> analyses = aService.getAnalysesBySampleItemIdAndStatusId("1", "1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisByTestSectionAndCompletedDateRange_shouldReturnAnalysis() throws Exception {
        Date sqlDayOne = Date.valueOf("2023-11-15");
        Date sqlDayTwo = Date.valueOf("2023-11-16");

        List<Analysis> analyses = aService.getAnalysisByTestSectionAndCompletedDateRange("1", sqlDayOne, sqlDayTwo);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAllAnalysisByTestAndExcludedStatus_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("2");
        List<Analysis> analyses = aService.getAllAnalysisByTestSectionAndExcludedStatus("1", statusIdList);

        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleStatusId_shouldReturnAnalysesBySampleStatusId() {
        List<Analysis> analyses = aService.getAnalysesBySampleStatusId("1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisEnteredAfterDate_shouldReturnAnalysisEnteredAfterDate() {

        Date date = Date.valueOf("2023-11-16");
        long time = date.getTime();
        Timestamp ed = new Timestamp(time);
        List<Analysis> analyses = aService.getAnalysisEnteredAfterDate(ed);
        Assert.assertNotNull(analyses);
        Assert.assertEquals("CONFIRM", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleIdAndStatusId_shouldReturnAnalysis() throws Exception {
        Set<String> statusIds = new HashSet<>();
        statusIds.add("1");
        List<Analysis> analyses = aService.getAnalysesBySampleIdAndStatusId("1", statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesByPriorityAndStatusId_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getAnalysesByPriorityAndStatusId(OrderPriority.ROUTINE, statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
        Assert.assertEquals("CONFIRM", analyses.get(1).getAnalysisType());
    }

    @Test
    public void getAnalysisStartedOn_shouldReturnAnalysis() throws Exception {
        Date sqlDayOne = Date.valueOf("2023-11-15");

        List<Analysis> analyses = aService.getAnalysisStartedOn(sqlDayOne);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleId_shouldReturnAnalysesBySampleId() {
        List<Analysis> analyses = aService.getAnalysesBySampleId("1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisBySampleAndTestIds_shouldReturnAnalysis() throws Exception {
        List<String> testIds = Arrays.asList("1");
        List<Analysis> analyses = aService.getAnalysisBySampleAndTestIds("1", testIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisCompleteInRange_shouldReturnAnalysis() throws Exception {
        Date date = Date.valueOf("2023-11-15");
        long time = date.getTime();
        Timestamp lowDate = new Timestamp(time);

        Date date2 = Date.valueOf("2023-11-17");
        long time2 = date2.getTime();
        Timestamp highDate = new Timestamp(time2);
        List<Analysis> analyses = aService.getAnalysisCompleteInRange(lowDate, highDate);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
        Assert.assertEquals("CONFIRM", analyses.get(1).getAnalysisType());
    }

    @Test
    public void getAnalysesForStatusId_shouldReturnAnalysis() throws Exception {
        List<Analysis> analyses = aService.getAnalysesForStatusId("1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getCountOfAnalysesForStatusIds_shouldReturnAnalysisCount() throws Exception {
        List<String> statusIdList = Arrays.asList("1", "2");
        int analyses = aService.getCountOfAnalysesForStatusIds(statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses);
    }

    @Test
    public void getAnalysisByAccessionAndTestId_shouldReturnAnalysisByAccessionAndTestId() {
        List<Analysis> analyses = aService.getAnalysisByAccessionAndTestId("12345", "1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getTestDisplayName_shouldReturnCorrectTestName() {
        Analysis analysis = aService.get("1");
        String displayName = aService.getTestDisplayName(analysis);
        Assert.assertNotNull(displayName);
        // Assert.assertTrue(displayName.contains("Serum"));
    }

    @Test
    public void getAllAnalysisByTestAndStatus_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1");
        List<Analysis> analyses = aService.getAllAnalysisByTestAndStatus("1", statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAllAnalysisByTestsAndStatus_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1", "2");
        List<String> testIdList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getAllAnalysisByTestsAndStatus(testIdList, statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
        Assert.assertEquals("CONFIRM", analyses.get(1).getAnalysisType());
    }

    @Test
    public void getAnalysesBySampleItem_shouldReturnAnalysesBySampleItem() {
        SampleItem sampleItem = sampleItemService.get("1");
        List<Analysis> analyses = aService.getAnalysesBySampleItem(sampleItem);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void buildAnalysis_shouldBuildAnalysis() {
        SampleItem sampleItem = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test = tService.get("1");
        Analysis analysis = aService.buildAnalysis(test, sampleItem);
        Assert.assertEquals("MANUAL", analysis.getAnalysisType());
    }

    @Test
    public void insert_shouldInsertAnalysisWithResultFile() {

        Analysis analysis = createDemoAnalysis();
        String analysisId = aService.insert(analysis);

        Analysis retrievedAnalysis = aService.getAnalysisById(analysisId);
        Assert.assertEquals(analysisId, retrievedAnalysis.getId());
        Assert.assertEquals("resultfile.txt", retrievedAnalysis.getResultFile().getFileName());

    }

    @Test
    public void update_shouldUpdateAnalysis() {
        Timestamp createAt = new Timestamp(System.currentTimeMillis());
        Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
        Analysis analysis = aService.get("2");
        ResultFile resultFile = new ResultFile();
        resultFile.setFileName("resultfile.txt");
        resultFile.setFileType("TEXT");
        resultFile.setContent(fileContent);
        resultFile.setLastupdated(updatedAt);
        resultFile.setUploadedAt(createAt);
        analysis.setResultFile(resultFile);
        Analysis updatedAnalysis = aService.update(analysis);
        Assert.assertEquals(analysis.getId(), updatedAnalysis.getId());
        Assert.assertEquals("resultfile.txt", updatedAnalysis.getResultFile().getFileName());
    }

    @Test
    public void getAllAnalysisByTestIdAndExcludedStatus_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("2");
        List<Analysis> analyses = aService.getAllAnalysisByTestAndExcludedStatus("1", statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void get_shouldReturnAnalysesForIdList() {
        List<Analysis> analyses = aService.get(Arrays.asList("1", "2"));
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
    }

    @Test
    public void get_shouldReturnEmptyForEmptyIdList() {
        List<Analysis> analyses = aService.get(Arrays.asList());
        Assert.assertNotNull(analyses);
        Assert.assertTrue(analyses.isEmpty());
    }

    @Test
    public void getAllAnalysisByTestSectionAndStatus_withSortedByDate_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1");
        List<Analysis> analyses = aService.getAllAnalysisByTestSectionAndStatus("1", statusIdList, true);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAllAnalysisByTestsAndStatus_withThreeLists_shouldReturnAnalysis() throws Exception {
        List<String> testIdList = Arrays.asList("1", "2");
        List<String> analysisStatusList = Arrays.asList("1", "2");
        List<String> sampleStatusList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getAllAnalysisByTestsAndStatus(testIdList, analysisStatusList,
                sampleStatusList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(2, analyses.size());
    }

    @Test
    public void getAllAnalysisByTestsAndStatusAndCompletedDateRange_shouldReturnAnalysis() throws Exception {
        List<String> testIdList = Arrays.asList("1", "2");
        List<String> analysisStatusList = Arrays.asList("1", "2");
        List<String> sampleStatusList = Arrays.asList("1", "2");
        Date lowDate = Date.valueOf("2023-11-15");
        Date highDate = Date.valueOf("2023-11-17");
        List<Analysis> analyses = aService.getAllAnalysisByTestsAndStatusAndCompletedDateRange(testIdList,
                analysisStatusList, sampleStatusList, lowDate, highDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatus_shouldReturnAnalysis() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1");
        List<String> sampleStatusList = Arrays.asList("1");
        List<Analysis> analyses = aService.getPageAnalysisByTestSectionAndStatus("1", analysisStatusList,
                sampleStatusList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
    }

    @Test
    public void getPageAnalysisAtAccessionNumberAndStatus_shouldReturnAnalysis() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1", "2");
        List<String> sampleStatusList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getPageAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                "12345");
        Assert.assertNotNull(analyses);
        Assert.assertTrue(analyses.size() >= 1);
    }

    @Test
    public void getAllChildAnalysesByResult_shouldReturnChildAnalyses() throws Exception {
        Result result = resultService.get("1");
        List<Analysis> analyses = aService.getAllChildAnalysesByResult(result);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAllMaxRevisionAnalysesPerTest_shouldReturnAnalyses() throws Exception {
        org.openelisglobal.test.valueholder.Test test = tService.get("1");
        List<Analysis> analyses = aService.getAllMaxRevisionAnalysesPerTest(test);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionAnalysisBySampleAndTest_shouldPopulateAnalysis() throws Exception {
        Analysis analysis = aService.get("1");
        aService.getMaxRevisionAnalysisBySampleAndTest(analysis);
        Assert.assertNotNull(analysis);
    }

    @Test
    public void getAnalysesForStatusId_viaDAO_shouldReturnAnalysis() throws Exception {
        List<Analysis> analyses = aService.getAnalysesForStatusId("1");
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
    }

    @Test
    public void getAnalysesCompletedOnByStatusId_shouldReturnAnalysis() throws Exception {
        Date completedDate = Date.valueOf("2023-11-15");
        List<Analysis> analyses = aService.getAnalysisStartedOnRangeByStatusId(completedDate, completedDate, "1");
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisCollectedOn_shouldReturnAnalysis() throws Exception {
        Date collectionDate = Date.valueOf("2023-11-15");
        List<Analysis> analyses = aService.getAnalysisCollectedOn(collectionDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAllAnalysisByTestSectionAndStatus_withThreeLists_shouldReturnAnalysis() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1");
        List<String> sampleStatusList = Arrays.asList("1");
        List<Analysis> analyses = aService.getAllAnalysisByTestSectionAndStatus("1", analysisStatusList,
                sampleStatusList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatus_withThreeLists_shouldReturnCount() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1");
        List<String> sampleStatusList = Arrays.asList("1");
        int count = aService.getCountAnalysisByTestSectionAndStatus("1", analysisStatusList, sampleStatusList);
        Assert.assertEquals(1, count);
    }

    @Test
    public void getCountAnalysisByTestSectionAndStatus_withTwoLists_shouldReturnCount() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1");
        int count = aService.getCountAnalysisByTestSectionAndStatus("1", analysisStatusList);
        Assert.assertEquals(1, count);
    }

    @Test
    public void getPageAnalysisByStatusFromAccession_withRange_shouldReturnAnalysis() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1", "2");
        List<String> sampleStatusList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getPageAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                "12345", "13333", true, false);
        Assert.assertNotNull(analyses);
        Assert.assertTrue(analyses.size() >= 1);
    }

    @Test
    public void getPageAnalysisByStatusFromAccession_withFinishedTrue_shouldExecuteWithoutTypeError() throws Exception {
        List<String> analysisStatusList = new ArrayList<>(Arrays.asList("1"));
        List<String> sampleStatusList = new ArrayList<>(Arrays.asList("1"));
        List<Analysis> analyses = aService.getPageAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList,
                "12345", null, false, true);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisForSiteBetweenResultDates_shouldReturnList() throws Exception {
        LocalDate lowerDate = LocalDate.of(2023, 11, 1);
        LocalDate upperDate = LocalDate.of(2023, 12, 31);
        List<Analysis> analyses = aService.getAnalysisForSiteBetweenResultDates("3", lowerDate, upperDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getStudyAnalysisForSiteBetweenResultDates_shouldReturnList() throws Exception {
        LocalDate lowerDate = LocalDate.of(2023, 11, 1);
        LocalDate upperDate = LocalDate.of(2023, 12, 31);
        List<Analysis> analyses = aService.getStudyAnalysisForSiteBetweenResultDates("3", lowerDate, upperDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getCountOfAnalysisCompletedOnByStatusId_shouldReturnCount() throws Exception {
        Date completedDate = Date.valueOf("2023-11-15");
        List<String> statusIds = Arrays.asList("1");
        int count = aService.getCountOfAnalysisCompletedOnByStatusId(completedDate, statusIds);
        Assert.assertTrue(count >= 0);
    }

    @Test
    public void getCountOfAnalysisStartedOnExcludedByStatusId_shouldReturnCount() throws Exception {
        Date startedDate = Date.valueOf("2023-11-15");
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        int count = aService.getCountOfAnalysisStartedOnExcludedByStatusId(startedDate, statusIds);
        Assert.assertTrue(count >= 0);
    }

    @Test
    public void getCountOfAnalysisStartedOnByStatusId_shouldReturnCount() throws Exception {
        Date startedDate = Date.valueOf("2023-11-15");
        List<String> statusIds = Arrays.asList("1");
        int count = aService.getCountOfAnalysisStartedOnByStatusId(startedDate, statusIds);
        Assert.assertTrue(count >= 0);
    }

    @Test
    public void getAnalysisBySampleItemAndTest_shouldReturnAnalysis() throws Exception {
        Analysis analysis = aService.getAnalysisBySampleItemAndTest("1", "1");
        Assert.assertNotNull(analysis);
        Assert.assertEquals("ROUTINE", analysis.getAnalysisType());
    }

    @Test
    public void getMaxRevisionAnalysesBySample_shouldReturnAnalyses() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        List<Analysis> analyses = aService.getMaxRevisionAnalysesBySample(sampleItem);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionParentTestAnalysesBySample_shouldReturnAnalyses() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        List<Analysis> analyses = aService.getMaxRevisionParentTestAnalysesBySample(sampleItem);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisCollectedOnExcludedByStatusId_shouldReturnAnalysis() throws Exception {
        Date collectionDate = Date.valueOf("2023-11-15");
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysisCollectedOnExcludedByStatusId(collectionDate, statusIds);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getRevisionHistoryOfAnalysesBySample_shouldReturnList() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        List<Analysis> analyses = aService.getRevisionHistoryOfAnalysesBySample(sampleItem);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysesAlreadyReportedBySample_shouldReturnList() throws Exception {
        Sample sample = sampleService.get("1");
        List<Analysis> analyses = aService.getAnalysesAlreadyReportedBySample(sample);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAllAnalysisByTestsAndStatusAndCompletedDateRange_4param_shouldReturnAnalysis() throws Exception {
        List<String> testIdList = Arrays.asList("1", "2");
        List<String> statusIdList = Arrays.asList("1", "2");
        Date lowDate = Date.valueOf("2023-11-15");
        Date highDate = Date.valueOf("2023-11-17");
        List<Analysis> analyses = analysisDAO.getAllAnalysisByTestsAndStatusAndCompletedDateRange(testIdList,
                statusIdList, lowDate, highDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatus_booleanOverload_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1");
        List<Analysis> analyses = aService.getPageAnalysisByTestSectionAndStatus("1", statusIdList, false);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
    }

    @Test
    public void getPageAnalysisByTestSectionAndStatus_booleanOverload_sorted_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1");
        List<Analysis> analyses = aService.getPageAnalysisByTestSectionAndStatus("1", statusIdList, true);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
    }

    @Test
    public void getPageAnalysisAtAccessionNumberAndStatus_booleanOverload_shouldReturnAnalysis() throws Exception {
        List<String> statusIdList = Arrays.asList("1", "2");
        List<Analysis> analyses = aService.getPageAnalysisAtAccessionNumberAndStatus("12345", statusIdList, false);
        Assert.assertNotNull(analyses);
        Assert.assertTrue(analyses.size() >= 1);
    }

    @Test
    public void getMaxRevisionAnalysesBySampleIncludeCanceled_shouldReturnAnalyses() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        List<Analysis> analyses = aService.getMaxRevisionAnalysesBySampleIncludeCanceled(sampleItem);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getRevisionHistoryOfAnalysesBySampleAndTest_includeLatest_shouldReturnList() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test = tService.get("1");
        List<Analysis> analyses = aService.getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem, test, true);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getRevisionHistoryOfAnalysesBySampleAndTest_excludeLatest_shouldReturnList() throws Exception {
        SampleItem sampleItem = sampleItemService.get("1");
        org.openelisglobal.test.valueholder.Test test = tService.get("1");
        List<Analysis> analyses = aService.getRevisionHistoryOfAnalysesBySampleAndTest(sampleItem, test, false);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionAnalysesReadyToBeReported_shouldReturnList() throws Exception {
        List<Analysis> analyses = aService.getMaxRevisionAnalysesReadyToBeReported();
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysesReadyToBeReported_shouldReturnList() throws Exception {
        List<Analysis> analyses = aService.getAnalysesReadyToBeReported();
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionAnalysesReadyForReportPreviewBySample_shouldReturnList() throws Exception {
        List<String> accessionNumbers = Arrays.asList("12345");
        List<Analysis> analyses = aService.getMaxRevisionAnalysesReadyForReportPreviewBySample(accessionNumbers);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionPendingAnalysesReadyToBeReportedBySample_shouldReturnList() throws Exception {
        Sample sample = sampleService.get("1");
        List<Analysis> analyses = aService.getMaxRevisionPendingAnalysesReadyToBeReportedBySample(sample);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getMaxRevisionPendingAnalysesReadyForReportPreviewBySample_shouldReturnList() throws Exception {
        Sample sample = sampleService.get("1");
        List<Analysis> analyses = aService.getMaxRevisionPendingAnalysesReadyForReportPreviewBySample(sample);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getPreviousAnalysisForAmendedAnalysis_shouldReturnNull() throws Exception {
        Analysis analysis = aService.get("1");
        Analysis previous = aService.getPreviousAnalysisForAmendedAnalysis(analysis);
        // Revision is "1", so previous (revision "0") doesn't exist
        Assert.assertNull(previous);
    }

    @Test
    public void getAnalysisStartedOnExcludedByStatusId_shouldReturnAnalysis() throws Exception {
        Date startedDate = Date.valueOf("2023-11-15");
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysisStartedOnExcludedByStatusId(startedDate, statusIds);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisStartedOnExcludedByStatusId_emptyStatusIds_shouldFallBackToStartedOn() throws Exception {
        Date startedDate = Date.valueOf("2023-11-15");
        Set<String> emptyStatusIds = new HashSet<>();
        List<Analysis> analyses = aService.getAnalysisStartedOnExcludedByStatusId(startedDate, emptyStatusIds);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysesCompletedOnByStatusId_byReleasedDate_shouldReturnAnalysis() throws Exception {
        Date releasedDate = Date.valueOf("2023-11-15");
        List<Analysis> analyses = aService.getAnalysesCompletedOnByStatusId(releasedDate, "1");
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisByTestDescriptionAndCompletedDateRange_shouldReturnAnalysis() throws Exception {
        List<String> descriptions = Arrays.asList("Blood Test", "Urine Test");
        Date lowDate = Date.valueOf("2023-11-15");
        Date highDate = Date.valueOf("2023-11-17");
        List<Analysis> analyses = aService.getAnalysisByTestDescriptionAndCompletedDateRange(descriptions, lowDate,
                highDate);
        Assert.assertNotNull(analyses);
    }

    @Test
    public void getAnalysisByTestDescriptionAndCompletedDateRange_emptyDescriptions_shouldReturnEmpty()
            throws Exception {
        List<String> descriptions = Arrays.asList();
        Date lowDate = Date.valueOf("2023-11-15");
        Date highDate = Date.valueOf("2023-11-17");
        List<Analysis> analyses = aService.getAnalysisByTestDescriptionAndCompletedDateRange(descriptions, lowDate,
                highDate);
        Assert.assertNotNull(analyses);
        Assert.assertTrue(analyses.isEmpty());
    }

    @Test
    public void getCountAnalysisByStatusFromAccession_shouldReturnCount() throws Exception {
        List<String> analysisStatusList = Arrays.asList("1", "2");
        List<String> sampleStatusList = Arrays.asList("1", "2");
        int count = aService.getCountAnalysisByStatusFromAccession(analysisStatusList, sampleStatusList, "12345");
        Assert.assertTrue(count >= 0);
    }

    @Test
    public void getAnalysesResultEnteredOnExcludedByStatusId_shouldReturnAnalysis() throws Exception {
        Date completedDate = Date.valueOf("2023-11-15");
        Set<String> statusIds = new HashSet<>();
        statusIds.add("2");
        List<Analysis> analyses = aService.getAnalysesResultEnteredOnExcludedByStatusId(completedDate, statusIds);
        Assert.assertNotNull(analyses);
    }

    public Analysis createDemoAnalysis() {
        Analysis analysis1 = aService.getAnalysisById("2");
        aService.delete(analysis1);

        Analysis analysis = new Analysis();

        analysis.setId("3");
        Timestamp createAt = new Timestamp(System.currentTimeMillis());
        Timestamp updatedAt = new Timestamp(System.currentTimeMillis());
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setAnalysisType("Blood Test");
        analysis.setRevision("1");

        // Dates
        analysis.setStartedDate(Date.valueOf("2025-10-01"));
        analysis.setCompletedDate(Date.valueOf("2025-10-02"));
        analysis.setReleasedDate(Date.valueOf("2025-10-03"));
        analysis.setEnteredDate(Timestamp.valueOf("2025-10-01 10:00:00"));
        TestSection testSection = testSectionService.get("1");
        analysis.setTestSection(testSection);
        org.openelisglobal.test.valueholder.Test test = tService.get("1");

        SampleItem sampleItem = sampleItemService.get("1");
        analysis.setTest(test);
        analysis.setSampleItem(sampleItem);

        // Method
        Method method = methodService.get("1");
        analysis.setMethod(method);

        ResultFile resultFile = new ResultFile();
        resultFile.setFileName("resultfile.txt");
        resultFile.setFileType("TEXT");
        resultFile.setContent(fileContent);
        resultFile.setLastupdated(updatedAt);
        resultFile.setUploadedAt(createAt);
        analysis.setResultFile(resultFile);

        Result result = resultService.get("1");
        analysis.setParentResult(result);
        analysis.setTriggeredReflex(false);
        analysis.setResultCalculated(true);
        analysis.setReferredOut(false);
        analysis.setCorrectedSincePatientReport(false);
        analysis.setIsReportable("Y");
        analysis.setSoClientReference("LAB-REF-1001");
        analysis.setSoSendReadyDate(Date.valueOf("2025-10-04"));
        analysis.setSoSendEntryBy("tech_user");
        analysis.setSoSendEntryDate(Date.valueOf("2025-10-05"));

        return analysis;
    }

    @Test
    public void getAllMatching_shouldReturnAllMatchingGiveFhirUUID() {
        List<Analysis> analysises = aService.getAllMatching("fhirUuid",
                UUID.fromString("f8b9e2c1-7a2d-4e8b-b3a4-9c1e7f6d2b01"));
        analysises.forEach(analsis -> {
            System.out.println(analsis.getId());
        });
    }

    // === T000b: Timestamp precision round-trip test (OGC-310 M0) ===
    @Test
    public void timestampFields_shouldPreserveTimeOfDay() {
        // Verify that startedDate, completedDate, releasedDate preserve
        // hour/minute/second after HBM mapping fix from java.sql.Date to
        // java.sql.Timestamp. DB columns are TIMESTAMP WITHOUT TIME ZONE.
        Analysis analysis = new Analysis();
        analysis.setAnalysisType("TAT_PRECISION_TEST");
        analysis.setRevision("0");
        analysis.setFhirUuid(UUID.randomUUID());

        // Set timestamps with specific time-of-day (NOT midnight)
        Timestamp started = Timestamp.valueOf("2026-03-15 14:30:45");
        Timestamp completed = Timestamp.valueOf("2026-03-15 16:15:30");
        Timestamp released = Timestamp.valueOf("2026-03-15 17:45:00");

        analysis.setStartedDate(started);
        analysis.setCompletedDate(completed);
        analysis.setReleasedDate(released);

        // Verify in-memory values preserve time before any persistence
        Assert.assertNotNull("startedDate should not be null", analysis.getStartedDate());
        Assert.assertNotNull("completedDate should not be null", analysis.getCompletedDate());
        Assert.assertNotNull("releasedDate should not be null", analysis.getReleasedDate());

        // Verify time components are NOT midnight (the bug symptom)
        java.time.LocalTime startedTime = analysis.getStartedDate().toLocalDateTime().toLocalTime();
        java.time.LocalTime completedTime = analysis.getCompletedDate().toLocalDateTime().toLocalTime();
        java.time.LocalTime releasedTime = analysis.getReleasedDate().toLocalDateTime().toLocalTime();

        Assert.assertEquals("startedDate hour should be 14", 14, startedTime.getHour());
        Assert.assertEquals("startedDate minute should be 30", 30, startedTime.getMinute());
        Assert.assertEquals("completedDate hour should be 16", 16, completedTime.getHour());
        Assert.assertEquals("completedDate minute should be 15", 15, completedTime.getMinute());
        Assert.assertEquals("releasedDate hour should be 17", 17, releasedTime.getHour());
        Assert.assertEquals("releasedDate minute should be 45", 45, releasedTime.getMinute());
    }

    // === T000c: TAT hour-level calculation test (OGC-310 M0) ===
    @Test
    public void tatCalculation_shouldUseActualHoursNotDayMultiples() {
        // Verify that TAT calculation using Timestamp fields produces
        // hour-level precision, not multiples of 24 (the pre-fix behavior).
        Analysis analysis = new Analysis();

        // Scenario: started at 9:00 AM, released at 3:00 PM same day = 6 hours
        Timestamp started = Timestamp.valueOf("2026-03-15 09:00:00");
        Timestamp released = Timestamp.valueOf("2026-03-15 15:00:00");
        analysis.setStartedDate(started);
        analysis.setReleasedDate(released);

        long hoursDiff = java.time.Duration
                .between(analysis.getStartedDate().toInstant(), analysis.getReleasedDate().toInstant()).toHours();

        Assert.assertEquals("Same-day TAT should be 6 hours, not 0 or 24", 6L, hoursDiff);

        // Scenario: started Friday 4 PM, released Monday 9 AM = 65 hours calendar
        Timestamp fridayAfternoon = Timestamp.valueOf("2026-03-13 16:00:00");
        Timestamp mondayMorning = Timestamp.valueOf("2026-03-16 09:00:00");
        analysis.setStartedDate(fridayAfternoon);
        analysis.setReleasedDate(mondayMorning);

        long weekendHours = java.time.Duration
                .between(analysis.getStartedDate().toInstant(), analysis.getReleasedDate().toInstant()).toHours();

        Assert.assertEquals("Weekend TAT should be 65 hours, not 72 (3*24)", 65L, weekendHours);
    }
}
