package org.openelisglobal.analysis;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisServiceTest extends BaseWebContextSensitiveTest {
    @Autowired
    AnalysisService aService;

    @Autowired
    SampleService sampleService;

    @Autowired
    TestService tService;

    @Autowired
    SampleItemService sampleItemService;

    @Autowired
    ResultService resultService;

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
        Set<Integer> statusIds = new HashSet<>();
        statusIds.add(2);
        List<Analysis> analyses = aService.getAnalysesBySampleIdExcludedByStatusId("1", statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysisByTestIdAndTestSectionIdsAndStartedInDateRange_shouldReturnAnalysis() throws Exception {
        List<Integer> testSectionIds = Arrays.asList(1);
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
        List<Integer> testSectionIds = Arrays.asList(1, 2);
        List<Integer> sampleIdList = Arrays.asList(1, 2);
        List<Integer> statusIdList = Arrays.asList(1, 2);
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
        Set<Integer> statusIds = new HashSet<>();
        statusIds.add(2);
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
        Set<Integer> statusIds = new HashSet<>();
        statusIds.add(2);
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
        List<Integer> statusIdList = Arrays.asList(2);
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
        Set<Integer> statusIds = new HashSet<>();
        statusIds.add(1);
        List<Analysis> analyses = aService.getAnalysesBySampleIdAndStatusId("1", statusIds);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAnalysesByPriorityAndStatusId_shouldReturnAnalysis() throws Exception {
        List<Integer> statusIdList = Arrays.asList(1, 2);
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
        List<Integer> testIds = Arrays.asList(1);
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
        List<Integer> statusIdList = Arrays.asList(1, 2);
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
        Assert.assertTrue(displayName.contains("Serum"));
    }

    @Test
    public void getAllAnalysisByTestAndStatus_shouldReturnAnalysis() throws Exception {
        List<Integer> statusIdList = Arrays.asList(1);
        List<Analysis> analyses = aService.getAllAnalysisByTestAndStatus("1", statusIdList);
        Assert.assertNotNull(analyses);
        Assert.assertEquals(1, analyses.size());
        Assert.assertEquals("ROUTINE", analyses.get(0).getAnalysisType());
    }

    @Test
    public void getAllAnalysisByTestsAndStatus_shouldReturnAnalysis() throws Exception {
        List<Integer> statusIdList = Arrays.asList(1, 2);
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
}
