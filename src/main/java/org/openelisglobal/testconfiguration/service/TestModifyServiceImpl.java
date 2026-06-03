package org.openelisglobal.testconfiguration.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.qc.dao.TestQcThresholdDAO;
import org.openelisglobal.qc.valueholder.TestQcThreshold;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestModifyEntryController.TestSet;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestModifyServiceImpl implements TestModifyService {

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private TestService testService;
    @Autowired
    private ResultLimitService resultLimitService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestQcThresholdDAO testQcThresholdDAO;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TestSectionService testSectionService;

    @Override
    @Transactional
    public void updateTestSets(List<TestSet> testSets, TestAddParams testAddParams, Localization nameLocalization,
            Localization reportingNameLocalization, String currentUserId) {
        if (!testSets.isEmpty()) {
            Set<String> submittedSampleTypeIds = testSets.stream().map(s -> s.sampleTypeTest.getTypeOfSampleId())
                    .collect(Collectors.toSet());
            List<TypeOfSampleTest> typeOfSampleTest = typeOfSampleTestService
                    .getTypeOfSampleTestsForTest(testAddParams.testId);
            for (TypeOfSampleTest tost : typeOfSampleTest) {
                if (submittedSampleTypeIds.contains(tost.getTypeOfSampleId())) {
                    typeOfSampleTestService.delete(tost.getId(), currentUserId);
                }
            }
        }

        List<PanelItem> panelItems = panelItemService.getPanelItemByTestId(testAddParams.testId);
        for (PanelItem item : panelItems) {
            item.setSysUserId(currentUserId);
        }
        panelItemService.deleteAll(panelItems);

        List<ResultLimit> resultLimitItems = resultLimitService.getAllResultLimitsForTest(testAddParams.testId);
        for (ResultLimit item : resultLimitItems) {
            item.setSysUserId(currentUserId);
            resultLimitService.delete(item);
        }
        // resultLimitService.delete(resultLimitItems);

        for (TestSet set : testSets) {
            set.test.setSysUserId(currentUserId);
            set.test.setLocalizedTestName(nameLocalization);
            set.test.setLocalizedReportingName(reportingNameLocalization);

            TestSection testSection = set.test.getTestSection();
            if ("N".equals(testSection.getIsActive())) {
                testSection.setIsActive("Y");
                testSection.setSysUserId(currentUserId);
                testSectionService.update(testSection);
            }

            // gnr: based on testAddUpdate,
            // added existing testId to process in createTestSets using
            // testAddParams.testId, delete then insert to modify for most elements

            for (Test test : set.sortedTests) {
                updateTestSortOrder(test.getId(), test.getSortOrder(), currentUserId);
            }

            updateTestNames(testAddParams.testId, nameLocalization, reportingNameLocalization, currentUserId);
            updateTestEntities(testAddParams.testId, testAddParams.loinc, currentUserId, testAddParams.uomId,
                    testAddParams.testSectionId, set.test.isNotifyResults(), set.test.isInLabOnly(),
                    set.test.getAntimicrobialResistance(), set.test.getIsActive(), set.test.getOrderable(),
                    set.test.getTimeHolding());

            set.sampleTypeTest.setSysUserId(currentUserId);
            set.sampleTypeTest.setTestId(set.test.getId());
            typeOfSampleTestService.insert(set.sampleTypeTest);

            for (PanelItem item : set.panelItems) {
                item.setSysUserId(currentUserId);
                Test nonTransiantTest = testService.getTestById(set.test.getId());
                item.setTest(nonTransiantTest);
                panelItemService.insert(item);
                if (item.getPanel() != null) {
                    TypeOfSample sampleType = typeOfSampleService.get(set.sampleTypeTest.getTypeOfSampleId());
                    if (typeOfSamplePanelService.getTypeOfSamplePanelsForSampleType(sampleType.getId()).isEmpty()) {
                        TypeOfSamplePanel tosp = new TypeOfSamplePanel();
                        tosp.setPanelId(item.getPanel().getId());
                        tosp.setTypeOfSampleId(sampleType.getId());
                        typeOfSamplePanelService.insert(tosp);
                    }
                    Panel panel = item.getPanel();
                    if ("N".equals(panel.getIsActive())) {
                        panel.setIsActive("Y");
                        panel.setSysUserId(currentUserId);
                        panelService.update(panel);
                    }
                }
            }

            for (TestResult testResult : set.testResults) {
                testResult.setSysUserId(currentUserId);
                Test nonTransiantTest = testService.getTestById(set.test.getId());
                testResult.setTest(nonTransiantTest);
                testResultService.insert(testResult);
                if (testResult.getDefault()) {
                    set.test.setDefaultTestResult(testResult);
                    updateTestDefault(testAddParams.testId, testResult, currentUserId);
                }
            }

            for (ResultLimit resultLimit : set.resultLimits) {
                resultLimit.setSysUserId(currentUserId);
                resultLimit.setTestId(set.test.getId());
                resultLimitService.insert(resultLimit);
            }
        }

        saveQcThresholds(testAddParams, currentUserId);
    }

    private void saveQcThresholds(TestAddParams testAddParams, String currentUserId) {
        boolean hasAnyValue = !GenericValidator.isBlankOrNull(testAddParams.qcBlankThreshold)
                || !GenericValidator.isBlankOrNull(testAddParams.qcRpdThreshold)
                || !GenericValidator.isBlankOrNull(testAddParams.qcRecoveryWindowPct);

        if (!hasAnyValue) {
            return;
        }

        Optional<TestQcThreshold> existing = testQcThresholdDAO.findByTestId(Integer.valueOf(testAddParams.testId));

        TestQcThreshold threshold;
        boolean isNew = existing.isEmpty();
        if (isNew) {
            threshold = new TestQcThreshold();
            threshold.setId(UUID.randomUUID().toString());
            threshold.setTestId(Integer.valueOf(testAddParams.testId));
        } else {
            threshold = existing.get();
        }

        threshold.setBlankThreshold(parseBigDecimal(testAddParams.qcBlankThreshold));
        threshold.setRpdThreshold(parseBigDecimal(testAddParams.qcRpdThreshold));
        threshold.setRecoveryWindowPct(parseBigDecimal(testAddParams.qcRecoveryWindowPct));
        threshold.setSystemUserId(Integer.valueOf(currentUserId));
        threshold.setSysUserId(currentUserId);

        if (isNew) {
            testQcThresholdDAO.insert(threshold);
        } else {
            testQcThresholdDAO.update(threshold);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateTestSortOrder(String testId, String sortOrder, String currentUserId) {
        Test test = testService.get(testId);

        if (test != null) {
            test.setSysUserId(currentUserId);
            test.setSortOrder(sortOrder);
            testService.update(test);
        }
    }

    private void updateTestDefault(String testId, TestResult testResult, String currentUserId) {
        Test test = testService.get(testId);

        if (test != null) {
            test.setSysUserId(currentUserId);
            test.setDefaultTestResult(testResult);
            testService.update(test);
        }
    }

    private void updateTestEntities(String testId, String loinc, String userId, String uomId, String testSectionId,
            Boolean notifyResults, boolean inLabOnly, Boolean antimicrobialResistance, String isActive,
            Boolean orderable, String timeHolding) {
        Test test = testService.get(testId);
        TestSection testSection = testSectionService.get(testSectionId);

        if (test != null) {
            test.setSysUserId(userId);
            test.setLoinc(loinc);
            test.setUnitOfMeasure(unitOfMeasureService.getUnitOfMeasureById(uomId));
            test.setNotifyResults(notifyResults);
            test.setInLabOnly(inLabOnly);
            test.setAntimicrobialResistance(antimicrobialResistance);
            test.setTestSection(testSection);
            test.setIsActive(isActive);
            test.setOrderable(orderable);
            test.setTimeHolding(timeHolding);
            testService.update(test);
        }
    }

    private void updateTestNames(String testId, Localization nameLocalization, Localization reportingNameLocalization,
            String userId) {
        Test test = testService.get(testId);

        if (test != null) {
            Localization name = test.getLocalizedTestName();
            Localization reportingName = test.getLocalizedReportingName();
            for (Locale locale : localizationService.getAllActiveLocales()) {
                name.setLocalizedValue(locale, nameLocalization.getLocalizedValue(locale).trim());
                reportingName.setLocalizedValue(locale, reportingNameLocalization.getLocalizedValue(locale).trim());
            }
            name.setSysUserId(userId);
            reportingName.setSysUserId(userId);

            localizationService.update(name);
            localizationService.update(reportingName);
        }

        // Refresh test names
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ALL_TESTS);
        DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ORDERABLE_TESTS);
    }
}
