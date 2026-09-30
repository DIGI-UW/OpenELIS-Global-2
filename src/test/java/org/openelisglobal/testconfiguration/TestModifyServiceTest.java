package org.openelisglobal.testconfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.localization.valueholder.LocalizationValue;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestSet;
import org.openelisglobal.testconfiguration.service.TestModifyService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TestModifyServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private TestModifyService testModifyService;

    @Autowired
    private TestModifyEntryController testModifyEntryController;

    @Autowired
    private TestService testService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private org.openelisglobal.panel.service.PanelService panelService;

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private LocalizationService localizationService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/test-modify-service.xml");
    }

    @Test
    public void updateTestSets_shouldReplaceTypeOfSampleTestLinkWithNewSampleType() {
        List<TypeOfSampleTest> before = typeOfSampleTestService.getTypeOfSampleTestsForTest("1");
        assertEquals(1, before.size());
        assertEquals("100", before.get(0).getTypeOfSampleId());

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("200", "1"));
        List<TestSet> sets = buildTestSets(params);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        List<TypeOfSampleTest> after = typeOfSampleTestService.getTypeOfSampleTestsForTest("1");
        assertEquals(1, after.size());
        assertEquals("200", after.get(0).getTypeOfSampleId());
    }

    @Test
    public void updateTestSets_shouldInsertMultipleSampleTypeLinksWhenMultipleSampleTypesGiven() {
        List<TypeOfSampleTest> before = typeOfSampleTestService.getTypeOfSampleTestsForTest("1");
        assertEquals(1, before.size());

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        params.sampleList.add(buildSampleEntry("200", "1"));
        List<TestSet> sets = buildTestSets(params);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        List<TypeOfSampleTest> after = typeOfSampleTestService.getTypeOfSampleTestsForTest("1");
        assertEquals(2, after.size());
        List<String> sampleTypeIds = new ArrayList<>();
        after.forEach(st -> sampleTypeIds.add(st.getTypeOfSampleId()));
        assertTrue(sampleTypeIds.contains("100"));
        assertTrue(sampleTypeIds.contains("200"));
    }

    @Test
    public void updateTestSets_shouldDeleteExistingPanelItemsAndInsertNew() {
        List<PanelItem> before = panelItemService.getPanelItemByTestId("1");
        assertEquals(1, before.size());

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        PanelItem replacement = new PanelItem();
        replacement.setPanel(panelService.getPanelById(before.get(0).getPanel().getId()));
        replacement.setSortOrder("2");
        sets.get(0).panelItems.add(replacement);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        List<PanelItem> after = panelItemService.getPanelItemByTestId("1");
        assertEquals(1, after.size());
        assertEquals("2", after.get(0).getSortOrder());
    }

    @Test
    public void updateTestSets_shouldDeleteExistingResultLimitsAndInsertNewOnes() {
        List<ResultLimit> before = resultLimitService.getAllResultLimitsForTest("1");
        assertEquals(1, before.size());
        assertEquals("U", before.get(0).getGender());
        assertEquals(4.0, before.get(0).getLowNormal(), 0.0001);

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        ResultLimit replacement = new ResultLimit();
        replacement.setResultTypeId("1");
        replacement.setGender("M");
        replacement.setMinAge(0.0);
        replacement.setMaxAge(18.0);
        replacement.setLowNormal(3.5);
        replacement.setHighNormal(10.5);
        replacement.setLowValid(1.0);
        replacement.setHighValid(20.0);
        sets.get(0).resultLimits.add(replacement);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        List<ResultLimit> after = resultLimitService.getAllResultLimitsForTest("1");
        assertEquals(1, after.size());
        assertEquals("M", after.get(0).getGender());
        assertEquals(0.0, after.get(0).getMinAge(), 0.0001);
        assertEquals(18.0, after.get(0).getMaxAge(), 0.0001);
        assertEquals(3.5, after.get(0).getLowNormal(), 0.0001);
        assertEquals(10.5, after.get(0).getHighNormal(), 0.0001);
    }

    @Test
    public void updateTestSets_shouldUpdateLocalizationNamesForAllActiveLocales() {
        org.openelisglobal.test.valueholder.Test testBefore = testService.get("1");
        assertEquals("Complete Blood Count", testBefore.getLocalizedTestName().getLocalizedValue(Locale.ENGLISH));
        assertEquals("Numération Formule Sanguine", testBefore.getLocalizedTestName().getLocalizedValue(Locale.FRENCH));
        assertEquals("CBC Report", testBefore.getLocalizedReportingName().getLocalizedValue(Locale.ENGLISH));
        assertEquals("Rapport NFS", testBefore.getLocalizedReportingName().getLocalizedValue(Locale.FRENCH));

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        Localization updatedName = LocalizationServiceImpl.createNewLocalization("Updated CBC", "NFS Mise à Jour",
                LocalizationServiceImpl.LocalizationType.TEST_NAME);
        Localization updatedReport = LocalizationServiceImpl.createNewLocalization("Updated CBC Report",
                "Rapport NFS Mis à Jour", LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);

        testModifyService.updateTestSets(sets, params, updatedName, updatedReport, "1");

        org.openelisglobal.test.valueholder.Test testAfter = testService.get("1");
        assertEquals("Updated CBC", testAfter.getLocalizedTestName().getLocalizedValue(Locale.ENGLISH));
        assertEquals("NFS Mise à Jour", testAfter.getLocalizedTestName().getLocalizedValue(Locale.FRENCH));
        assertEquals("Updated CBC Report", testAfter.getLocalizedReportingName().getLocalizedValue(Locale.ENGLISH));
        assertEquals("Rapport NFS Mis à Jour", testAfter.getLocalizedReportingName().getLocalizedValue(Locale.FRENCH));
    }

    @Test
    public void updateTestSets_shouldUpdateTestEntityScalarFields() {
        org.openelisglobal.test.valueholder.Test testBefore = testService.get("1");
        assertEquals("11111", testBefore.getLoinc());
        assertEquals("1", testBefore.getUnitOfMeasure().getId());
        assertFalse(testBefore.isNotifyResults());
        assertFalse(testBefore.isInLabOnly());
        assertFalse(testBefore.getAntimicrobialResistance());
        assertTrue(testBefore.getOrderable());

        TestAddParams params = buildBaseParams("1", "1");
        params.loinc = "99999-UPDATED";
        params.uomId = "2";
        params.notifyResults = "Y";
        params.inLabOnly = "Y";
        params.antimicrobialResistance = "Y";
        params.orderable = "N";
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        org.openelisglobal.test.valueholder.Test testAfter = testService.get("1");
        assertEquals("99999-UPDATED", testAfter.getLoinc());
        assertEquals("2", testAfter.getUnitOfMeasure().getId());
        assertTrue(testAfter.isNotifyResults());
        assertTrue(testAfter.isInLabOnly());
        assertTrue(testAfter.getAntimicrobialResistance());
        assertFalse(testAfter.getOrderable());
    }

    @Test
    public void updateTestSets_shouldActivateInactiveTestSectionDuringUpdate() {
        TestSection sectionBefore = testSectionService.get("2");
        assertEquals("N", sectionBefore.getIsActive());

        TestAddParams params = buildBaseParams("1", "2");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);
        sets.get(0).test.setTestSection(sectionBefore);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        TestSection sectionAfter = testSectionService.get("2");
        assertEquals("Y", sectionAfter.getIsActive());
    }

    @Test
    public void updateTestSets_shouldReplaceTestResultsWithNewSet() {
        org.openelisglobal.test.valueholder.Test testEntity = testService.get("1");
        List<TestResult> before = testResultService.getAllActiveTestResultsPerTest(testEntity);
        assertEquals(1, before.size());
        assertEquals("N", before.get(0).getTestResultType());
        assertEquals("7.5", before.get(0).getValue());
        assertEquals("2", before.get(0).getSignificantDigits());

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        TestResult replacement = new TestResult();
        replacement.setTestResultType("N");
        replacement.setValue("9.9");
        replacement.setSortOrder("1");
        replacement.setIsActive(true);
        replacement.setSignificantDigits("3");
        replacement.setDefault(false);
        sets.get(0).testResults.add(replacement);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        List<TestResult> after = testResultService.getAllActiveTestResultsPerTest(testService.get("1"));
        assertEquals(2, after.size());

        TestResult added = after.stream().filter(tr -> "9.9".equals(tr.getValue())).findFirst().get();
        assertEquals("N", added.getTestResultType());
        assertEquals("3", added.getSignificantDigits());
        assertEquals("1", added.getTest().getId());
    }

    @Test
    public void updateTestSets_shouldUpdateSortOrderOfSortedTests() {
        org.openelisglobal.test.valueholder.Test testBefore = testService.get("1");
        assertEquals("1", testBefore.getSortOrder());

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        org.openelisglobal.test.valueholder.Test orderedTest = testService.get("1");
        orderedTest.setSortOrder("42");
        sets.get(0).sortedTests.clear();
        sets.get(0).sortedTests.add(orderedTest);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        org.openelisglobal.test.valueholder.Test testAfter = testService.get("1");
        assertEquals("42", testAfter.getSortOrder());
    }

    @Test
    public void updateTestSets_shouldReassignTestSectionToNewActiveSection() {
        org.openelisglobal.test.valueholder.Test testBefore = testService.get("1");
        assertEquals("1", testBefore.getTestSection().getId());

        TestSection hematology = testSectionService.get("1");
        hematology.setIsActive("Y");

        TestAddParams params = buildBaseParams("1", "1");
        params.sampleList.add(buildSampleEntry("100", "1"));
        List<TestSet> sets = buildTestSets(params);

        testModifyService.updateTestSets(sets, params, makeNameLocalization(), makeReportLocalization(), "1");

        org.openelisglobal.test.valueholder.Test testAfter = testService.get("1");
        assertEquals("1", testAfter.getTestSection().getId());
        assertEquals("Hematology", testAfter.getTestSection().getTestSectionName());
    }

    private TestAddParams buildBaseParams(String testId, String testSectionId) {
        TestAddParams params = testModifyEntryController.new TestAddParams();
        params.testId = testId;
        params.testNameEnglish = "Complete Blood Count";
        params.testNameFrench = "Numération Formule Sanguine";
        params.testReportNameEnglish = "CBC Report";
        params.testReportNameFrench = "Rapport NFS";
        params.testSectionId = testSectionId;
        params.uomId = "1";
        params.loinc = "11111";
        params.active = "Y";
        params.orderable = "Y";
        params.notifyResults = "N";
        params.inLabOnly = "N";
        params.antimicrobialResistance = "N";
        return params;
    }

    private TestModifyEntryController.SampleTypeListAndTestOrder buildSampleEntry(String sampleTypeId, String testId) {
        TestModifyEntryController.SampleTypeListAndTestOrder entry = testModifyEntryController.new SampleTypeListAndTestOrder();
        entry.sampleTypeId = sampleTypeId;
        entry.orderedTests.add("0");
        return entry;
    }

    private List<TestSet> buildTestSets(TestAddParams params) {
        List<TestSet> sets = new ArrayList<>();
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(params.testId);
        TestSection section = testSectionService.get(params.testSectionId);
        test.setTestSection(section);
        test.setIsActive(params.active);
        test.setOrderable("Y".equals(params.orderable));
        test.setNotifyResults("Y".equals(params.notifyResults));
        test.setInLabOnly("Y".equals(params.inLabOnly));
        test.setAntimicrobialResistance("Y".equals(params.antimicrobialResistance));

        for (TestModifyEntryController.SampleTypeListAndTestOrder entry : params.sampleList) {
            TestSet set = testModifyEntryController.new TestSet();
            set.test = test;

            TypeOfSampleTest sampleTypeTest = new TypeOfSampleTest();
            sampleTypeTest.setTypeOfSampleId(entry.sampleTypeId);
            sampleTypeTest.setTestId(params.testId);
            set.sampleTypeTest = sampleTypeTest;

            set.sortedTests.add(test);
            sets.add(set);
        }
        return sets;
    }

    private Localization makeNameLocalization() {
        Localization l = new Localization();
        l.setDescription("Test Mod Name");
        LocalizationValue en = new LocalizationValue();
        en.setLocale("en");
        en.setValue("Test Mod Name EN");
        LocalizationValue fr = new LocalizationValue();
        fr.setLocale("fr");
        fr.setValue("Test Mod Name FR");
        java.util.Map<String, LocalizationValue> map = new java.util.HashMap<>();
        map.put("en", en);
        map.put("fr", fr);
        l.setValues(map);
        return l;
    }

    private Localization makeReportLocalization() {
        Localization l = new Localization();
        l.setDescription("Test Mod Report");
        LocalizationValue en = new LocalizationValue();
        en.setLocale("en");
        en.setValue("Test Mod Report EN");
        LocalizationValue fr = new LocalizationValue();
        fr.setLocale("fr");
        fr.setValue("Test Mod Report FR");
        java.util.Map<String, LocalizationValue> map = new java.util.HashMap<>();
        map.put("en", en);
        map.put("fr", fr);
        l.setValues(map);
        return l;
    }
}
