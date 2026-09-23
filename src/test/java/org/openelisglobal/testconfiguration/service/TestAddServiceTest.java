package org.openelisglobal.testconfiguration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestAddController;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

// The fixture does not reset every table written by the service. Roll back each
// case to prevent inserted rows from leaking into other integration tests.
@Transactional
public class TestAddServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestAddService testAddService;

    @Autowired
    private TestAddController testAddController;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    private static final String SECTION_ID = "1";
    private static final String SAMPLE_TYPE_ID = "1";

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/test-add-service.xml");
    }

    private TestSet buildTestSet(org.openelisglobal.test.valueholder.Test test, TypeOfSample typeOfSample) {
        TypeOfSampleTest sampleTypeTest = new TypeOfSampleTest();
        sampleTypeTest.setTypeOfSampleId(typeOfSample.getId());

        TestSet set = testAddController.new TestSet();
        set.test = test;
        set.typeOfSample = typeOfSample;
        set.sampleTypeTest = sampleTypeTest;
        return set;
    }

    private org.openelisglobal.test.valueholder.Test buildTest(String name, String loinc, String guid,
            TestSection section) {
        org.openelisglobal.test.valueholder.Test t = new org.openelisglobal.test.valueholder.Test();
        t.setName(name);
        t.setDescription(name + "(whole blood)");
        t.setLoinc(loinc);
        t.setIsActive("Y");
        t.setOrderable(true);
        t.setIsReportable("N");
        t.setGuid(guid);
        t.setTestSection(section);
        return t;
    }

    private Localization nameLocalization(String en, String fr) {
        return LocalizationServiceImpl.createNewLocalization(en, fr,
                LocalizationServiceImpl.LocalizationType.TEST_NAME);
    }

    private Localization reportingLocalization(String en, String fr) {
        return LocalizationServiceImpl.createNewLocalization(en, fr,
                LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
    }

    @Test
    public void addTests_insertsTestWithLocalizationsAndSampleTypeLinkAndActivatesSection() throws Exception {
        TestSection section = testSectionService.get(SECTION_ID);
        assertEquals("Fixture: section must start INACTIVE", "N", section.getIsActive());

        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        assertEquals("Fixture: sample type id must be 1", SAMPLE_TYPE_ID, sampleType.getId());

        org.openelisglobal.test.valueholder.Test newTest = buildTest("HIV Load Test", "20447-9", "guid-hiv-load-001",
                section);
        TestSet testSet = buildTestSet(newTest, sampleType);

        Localization nameLoc = nameLocalization("HIV Load Test", "Test de charge VIH");
        Localization reportingLoc = reportingLocalization("HIV Load Test Report", "Rapport de charge VIH");

        testAddService.addTests(List.of(testSet), nameLoc, reportingLoc, TEST_SYS_USER_ID);

        org.openelisglobal.test.valueholder.Test inserted = testService
                .getTestByDescription("HIV Load Test(whole blood)");
        assertEquals("Test name must match exactly", "HIV Load Test", inserted.getName());
        assertEquals("Test LOINC must match exactly", "20447-9", inserted.getLoinc());
        assertEquals("Test must be active", "Y", inserted.getIsActive());
        assertEquals("Test description must match exactly", "HIV Load Test(whole blood)", inserted.getDescription());
        assertEquals("Test must be orderable", Boolean.TRUE, inserted.getOrderable());

        assertEquals("Name localization description must be 'test name'", "test name",
                inserted.getLocalizedTestName().getDescription());
        assertEquals("Reporting name localization description must be 'test report name'", "test report name",
                inserted.getLocalizedReportingName().getDescription());

        assertFalse("Name and reporting localization must be different rows",
                inserted.getLocalizedTestName().getId().equals(inserted.getLocalizedReportingName().getId()));

        List<TypeOfSampleTest> links = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(SAMPLE_TYPE_ID);
        long matchingLinks = links.stream().filter(l -> inserted.getId().equals(l.getTestId())).count();
        assertEquals("Exactly 1 sampletype_test link must exist for the new test", 1L, matchingLinks);

        TestSection reloadedSection = testSectionService.get(SECTION_ID);
        assertEquals("TestSection must be ACTIVE after test is added", "Y", reloadedSection.getIsActive());
    }

    @Test
    public void addTests_persistsTestResultLinkedToInsertedTest() throws Exception {
        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        TestSection section = testSectionService.get(SECTION_ID);

        org.openelisglobal.test.valueholder.Test newTest = buildTest("Glucose Test", "2345-7", "guid-glucose-001",
                section);
        TestSet testSet = buildTestSet(newTest, sampleType);

        TestResult testResult = new TestResult();
        testResult.setTestResultType("N"); // Numeric
        testResult.setSortOrder("1");
        testResult.setIsActive(true);
        testSet.testResults.add(testResult);

        testAddService.addTests(List.of(testSet), nameLocalization("Glucose Test", "Test de glucose"),
                reportingLocalization("Glucose Report", "Rapport de glucose"), TEST_SYS_USER_ID);

        org.openelisglobal.test.valueholder.Test inserted = testService
                .getTestByDescription("Glucose Test(whole blood)");
        assertEquals("Inserted test name must be 'Glucose Test'", "Glucose Test", inserted.getName());
        assertEquals("Inserted test LOINC must be '2345-7'", "2345-7", inserted.getLoinc());

        List<TestResult> results = testResultService.getActiveTestResultsByTest(inserted.getId());
        assertEquals("Exactly 1 TestResult must be persisted for the new test", 1, results.size());
        assertEquals("TestResult type must be 'N' (Numeric)", "N", results.get(0).getTestResultType());
        assertTrue("TestResult must be active", results.get(0).getIsActive());
        assertEquals("TestResult sort order must be '1'", "1", results.get(0).getSortOrder());
        assertEquals("TestResult must be linked to the inserted test's id", inserted.getId(),
                results.get(0).getTest().getId());
    }

    @Test
    public void addTests_persistsResultLimitLinkedToInsertedTest() throws Exception {
        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        TestSection section = testSectionService.get(SECTION_ID);

        org.openelisglobal.test.valueholder.Test newTest = buildTest("Hemoglobin Test", "718-7", "guid-hgb-001",
                section);
        TestSet testSet = buildTestSet(newTest, sampleType);

        ResultLimit limit = new ResultLimit();
        limit.setResultTypeId("1");
        limit.setLowNormal(10.0);
        limit.setHighNormal(17.0);
        limit.setMinAge(0.0);
        limit.setMaxAge(Double.POSITIVE_INFINITY);
        testSet.resultLimits.add(limit);

        testAddService.addTests(List.of(testSet), nameLocalization("Hemoglobin Test", "Test Hémoglobine"),
                reportingLocalization("Hemoglobin Report", "Rapport Hémoglobine"), TEST_SYS_USER_ID);

        org.openelisglobal.test.valueholder.Test inserted = testService
                .getTestByDescription("Hemoglobin Test(whole blood)");
        assertEquals("Inserted test name must be 'Hemoglobin Test'", "Hemoglobin Test", inserted.getName());
        assertEquals("Inserted test LOINC must be '718-7'", "718-7", inserted.getLoinc());

        List<ResultLimit> limits = resultLimitService.getAllResultLimitsForTest(inserted.getId());
        assertEquals("Exactly 1 ResultLimit must be persisted for the new test", 1, limits.size());
        assertEquals("ResultLimit testId must match inserted test's id", inserted.getId(), limits.get(0).getTestId());
        assertEquals("ResultLimit low normal must be 10.0", 10.0, limits.get(0).getLowNormal(), 0.0001);
        assertEquals("ResultLimit high normal must be 17.0", 17.0, limits.get(0).getHighNormal(), 0.0001);
    }

    @Test
    public void addTests_keepsAlreadyActiveSectionAndPanelAndReordersSortedTests() throws Exception {
        TestSection section = testSectionService.get(SECTION_ID);
        section.setIsActive("Y");
        section.setSysUserId(TEST_SYS_USER_ID);
        testSectionService.update(section);

        Panel panel = panelService.getPanelById("1");
        panel.setIsActive("Y");
        panel.setSysUserId(TEST_SYS_USER_ID);
        panelService.update(panel);

        org.openelisglobal.test.valueholder.Test orderedTest = buildTest("Ordered Preexisting Test", "1234-5",
                "guid-ordered-001", section);
        orderedTest.setIsActive("Y");
        orderedTest.setOrderable(true);
        orderedTest.setIsReportable("N");
        orderedTest.setSortOrder("99");
        testService.insert(orderedTest);

        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        org.openelisglobal.test.valueholder.Test newTest = buildTest("No-Flip Test", "9876-5", "guid-no-flip-001",
                section);
        TestSet testSet = buildTestSet(newTest, sampleType);
        orderedTest.setSortOrder("0");
        testSet.sortedTests.add(orderedTest);

        PanelItem panelItem = new PanelItem();
        panelItem.setPanel(panel);
        testSet.panelItems.add(panelItem);

        testAddService.addTests(List.of(testSet), nameLocalization("No-Flip Test", "Test No-Flip"),
                reportingLocalization("No-Flip Report", "Rapport No-Flip"), TEST_SYS_USER_ID);

        TestSection reloadedSection = testSectionService.get(SECTION_ID);
        assertEquals("Section should remain ACTIVE when already active", "Y", reloadedSection.getIsActive());

        Panel reloadedPanel = panelService.getPanelById("1");
        assertEquals("Panel should remain ACTIVE when already active", "Y", reloadedPanel.getIsActive());

        org.openelisglobal.test.valueholder.Test reloadedOrderedTest = testService.get(orderedTest.getId());
        assertEquals("Sorted test sort order should be updated by addTests", "0", reloadedOrderedTest.getSortOrder());
    }

    @Test
    public void addTests_multipleTestSets_eachPersistsAsIndependentTestWithSharedLocalizations() throws Exception {
        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        TestSection section = testSectionService.get(SECTION_ID);

        org.openelisglobal.test.valueholder.Test test1 = buildTest("Multi Test Alpha", "88881-1", "guid-multi-001",
                section);
        org.openelisglobal.test.valueholder.Test test2 = buildTest("Multi Test Beta", "88882-2", "guid-multi-002",
                section);

        TestSet set1 = buildTestSet(test1, sampleType);
        TestSet set2 = buildTestSet(test2, sampleType);

        Localization nameLoc = nameLocalization("Multi Test", "Multi Test FR");
        Localization reportingLoc = reportingLocalization("Multi Report", "Multi Rapport");

        testAddService.addTests(List.of(set1, set2), nameLoc, reportingLoc, TEST_SYS_USER_ID);

        List<org.openelisglobal.test.valueholder.Test> tests1 = testService.getTestsByLoincCode("88881-1");
        List<org.openelisglobal.test.valueholder.Test> tests2 = testService.getTestsByLoincCode("88882-2");
        assertEquals("Exactly 1 test must be found for LOINC 88881-1", 1, tests1.size());
        assertEquals("Exactly 1 test must be found for LOINC 88882-2", 1, tests2.size());
        org.openelisglobal.test.valueholder.Test inserted1 = tests1.get(0);
        org.openelisglobal.test.valueholder.Test inserted2 = tests2.get(0);

        assertEquals("Test Alpha name must be the localization value", "Multi Test", inserted1.getName());
        assertEquals("Test Beta name must be the localization value", "Multi Test", inserted2.getName());

        assertEquals("Test Alpha description must match", "Multi Test Alpha(whole blood)", inserted1.getDescription());
        assertEquals("Test Beta description must match", "Multi Test Beta(whole blood)", inserted2.getDescription());
        assertEquals("Test Alpha LOINC must be 88881-1", "88881-1", inserted1.getLoinc());
        assertEquals("Test Beta LOINC must be 88882-2", "88882-2", inserted2.getLoinc());
        assertEquals("Test Alpha must be ACTIVE", "Y", inserted1.getIsActive());
        assertEquals("Test Beta must be ACTIVE", "Y", inserted2.getIsActive());

        assertEquals("Both tests must share the same name localization id", inserted1.getLocalizedTestName().getId(),
                inserted2.getLocalizedTestName().getId());

        assertEquals("Both tests must share the same reporting localization id",
                inserted1.getLocalizedReportingName().getId(), inserted2.getLocalizedReportingName().getId());

        List<TypeOfSampleTest> links = typeOfSampleTestService.getTypeOfSampleTestsForSampleType(SAMPLE_TYPE_ID);
        long linksForTest1 = links.stream().filter(l -> inserted1.getId().equals(l.getTestId())).count();
        long linksForTest2 = links.stream().filter(l -> inserted2.getId().equals(l.getTestId())).count();
        assertEquals("Exactly 1 sampletype_test link for Multi Test Alpha", 1L, linksForTest1);
        assertEquals("Exactly 1 sampletype_test link for Multi Test Beta", 1L, linksForTest2);
    }

    @Test
    public void addTests_persistsPanelItemsAndActivatesPanel() throws Exception {
        TypeOfSample sampleType = typeOfSampleService.get(SAMPLE_TYPE_ID);
        TestSection section = testSectionService.get(SECTION_ID);

        org.openelisglobal.test.valueholder.Test newTest = buildTest("Panel Test", "33333-3", "guid-panel-001",
                section);
        TestSet testSet = buildTestSet(newTest, sampleType);

        Panel panel = panelService.getPanelById("1");
        assertEquals("Fixture: Panel must start INACTIVE", "N", panel.getIsActive());

        PanelItem panelItem = new PanelItem();
        panelItem.setPanel(panel);
        panelItem.setSortOrder("1");
        testSet.panelItems.add(panelItem);

        testAddService.addTests(List.of(testSet), nameLocalization("Panel Test", "Test de Panel"),
                reportingLocalization("Panel Report", "Rapport de Panel"), TEST_SYS_USER_ID);

        org.openelisglobal.test.valueholder.Test inserted = testService.getTestByDescription("Panel Test(whole blood)");
        assertEquals("Inserted test name must be 'Panel Test'", "Panel Test", inserted.getName());
        assertEquals("Inserted test LOINC must be '33333-3'", "33333-3", inserted.getLoinc());

        List<PanelItem> items = panelItemService.getPanelItemByTestId(inserted.getId());
        assertEquals("Exactly 1 PanelItem must be persisted", 1, items.size());
        assertEquals("PanelItem must link to Panel 1", "1", items.get(0).getPanel().getId());
        assertEquals("PanelItem sort order must be 1", "1", items.get(0).getSortOrder());

        Panel updatedPanel = panelService.getPanelById("1");
        assertEquals("Panel must be ACTIVE after panel item is added", "Y", updatedPanel.getIsActive());
    }
}
