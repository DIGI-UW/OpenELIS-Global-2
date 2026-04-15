package org.openelisglobal.dataexchange.externalcatalog.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dataexchange.externalcatalog.dto.CatalogDefinitionRequest;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.event.PanelCreatedOrUpdatedEvent;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemusermodule.valueholder.RoleModule;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testconfiguration.service.PanelCreateService;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogInboundService {

    @Autowired
    private TestService testService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TestAddService testAddService;
    @Autowired
    private PanelCreateService panelCreateService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private TypeOfTestResultService typeOfTestResultService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private org.openelisglobal.localization.service.LocalizationService localizationService;

    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private org.openelisglobal.testresult.service.TestResultService testResultService;
    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;
    @Autowired
    private org.openelisglobal.typeofsample.service.TypeOfSamplePanelService typeOfSamplePanelService;
    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private org.openelisglobal.testconfiguration.controller.TestAddController testAddController;

    @Autowired
    private CatalogEntityResolver entityResolver;

    @Autowired
    private DisplayListService displayListService;

    @Autowired
    private org.openelisglobal.dataexchange.externalcatalog.validation.CatalogRequestValidator catalogRequestValidator;

    @Transactional
    public String upsert(CatalogDefinitionRequest request, String currentUserId) {
        // Second line of defence — catches any call that bypasses the controller
        // (e.g. internal service-to-service calls, tests, future integrations).
        catalogRequestValidator.validate(request);
        if (request.isPanel()) {
            return upsertPanel(request, currentUserId);
        } else {
            return upsertTest(request, currentUserId);
        }
    }

    @Transactional
    public String upsertTest(CatalogDefinitionRequest request, String currentUserId) {
        Test existing = findExistingTest(request);
        if (existing != null) {
            updateExistingTest(existing, request, currentUserId);
            return existing.getGuid();
        } else {
            return createNewTest(request, currentUserId);
        }
    }

    private Test findExistingTest(CatalogDefinitionRequest request) {
        if (!GenericValidator.isBlankOrNull(request.getTestUuid())) {
            return testService.getTestByGUID(request.getTestUuid());
        }
        if (!GenericValidator.isBlankOrNull(request.getLoincCode())) {
            List<Test> tests = testService.getActiveTestsByLoinc(request.getLoincCode());
            return (tests != null && !tests.isEmpty()) ? tests.get(0) : null;
        }
        return null;
    }

    private String createNewTest(CatalogDefinitionRequest request, String currentUserId) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "createNewTest",
                "Creating new test: " + request.getNameEnglish());

        Localization nameLocalization = LocalizationServiceImpl.createNewLocalization(request.getNameEnglish(),
                request.getNameFrench() != null ? request.getNameFrench() : request.getNameEnglish(),
                LocalizationServiceImpl.LocalizationType.TEST_NAME);
        Localization reportLocalization = LocalizationServiceImpl.createNewLocalization(
                request.getReportNameEnglish() != null ? request.getReportNameEnglish() : request.getNameEnglish(),
                request.getReportNameFrench() != null ? request.getReportNameFrench()
                        : (request.getReportNameEnglish() != null ? request.getReportNameEnglish()
                                : request.getNameEnglish()),
                LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);

        List<TestSet> testSets = buildTestSets(request, currentUserId);
        testAddService.addTests(testSets, nameLocalization, reportLocalization, currentUserId);

        eventPublisher
                .publishEvent(new org.openelisglobal.dataexchange.externalcatalog.event.CatalogUpsertedEvent(this));
        return request.getTestUuid() != null ? request.getTestUuid() : testSets.get(0).test.getGuid();
    }

    private void updateExistingTest(Test existing, CatalogDefinitionRequest request, String currentUserId) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "updateExistingTest", "Updating test: " + existing.getId());

        existing.setUnitOfMeasure(entityResolver.resolveUOM(request.getUomId(), request.getUomName(), currentUserId));
        // Only overwrite LOINC if the caller explicitly sent one — never wipe an
        // existing value with null
        if (!GenericValidator.isBlankOrNull(request.getLoincCode())) {
            existing.setLoinc(request.getLoincCode());
        }
        existing.setPrice(roundPrice(request.getPrice()));
        existing.setNotifyResults(request.isNotifyResults());
        existing.setInLabOnly(request.isInLabOnly());
        existing.setAntimicrobialResistance(request.isAntimicrobialResistance());
        existing.setTestSection(entityResolver.resolveTestSection(request.getTestSectionId(),
                request.getTestSectionName(), currentUserId));
        existing.setIsActive(request.isActive() ? "Y" : "N");
        existing.setOrderable(request.isOrderable());
        existing.setSysUserId(currentUserId);
        testService.update(existing);

        // Step 2: Update localizations
        updateLocalizations(existing, request, currentUserId);

        // Step 3: Build new TestSets (declarative desired state)
        List<TestSet> testSets = buildTestSets(request, currentUserId);

        // Step 4: Granular Sync of associations
        syncSampleTypes(existing, testSets, currentUserId);
        syncPanelItems(existing, testSets, currentUserId);
        syncResultsAndLimits(existing, testSets, request, currentUserId);

        eventPublisher
                .publishEvent(new org.openelisglobal.dataexchange.externalcatalog.event.CatalogUpsertedEvent(this));
        eventPublisher.publishEvent(new org.openelisglobal.test.event.TestCreatedEvent(this, existing, true));
    }

    private void syncSampleTypes(Test test, List<TestSet> testSets, String currentUserId) {
        List<TypeOfSampleTest> existingStts = typeOfSampleTestService.getTypeOfSampleTestsForTest(test.getId());
        List<String> payloadSampleTypeIds = testSets.stream().map(set -> set.typeOfSample.getId()).toList();

        // Remove those not in payload
        for (TypeOfSampleTest stt : existingStts) {
            if (!payloadSampleTypeIds.contains(stt.getTypeOfSampleId())) {
                typeOfSampleTestService.delete(stt.getId(), currentUserId);
            }
        }

        // Add those not in DB
        for (String stId : payloadSampleTypeIds) {
            boolean exists = existingStts.stream().anyMatch(stt -> stt.getTypeOfSampleId().equals(stId));
            if (!exists) {
                TypeOfSampleTest newStt = new TypeOfSampleTest();
                newStt.setTestId(test.getId());
                newStt.setTypeOfSampleId(stId);
                newStt.setSysUserId(currentUserId);
                typeOfSampleTestService.insert(newStt);
            }
        }
    }

    private void syncPanelItems(Test test, List<TestSet> testSets, String currentUserId) {
        List<PanelItem> existingItems = panelItemService.getPanelItemByTestId(test.getId());

        // Collect all desired panels from all test sets
        List<String> payloadPanelIds = testSets.stream().flatMap(set -> set.panelItems.stream())
                .filter(pi -> pi.getPanel() != null).map(pi -> pi.getPanel().getId()).distinct().toList();

        // Remove old associations
        for (PanelItem item : existingItems) {
            if (item.getPanel() == null || !payloadPanelIds.contains(item.getPanel().getId())) {
                item.setSysUserId(currentUserId);
                panelItemService.delete(item);
            }
        }

        // Add new associations
        for (String panelId : payloadPanelIds) {
            boolean exists = existingItems.stream()
                    .anyMatch(item -> item.getPanel() != null && item.getPanel().getId().equals(panelId));
            if (!exists) {
                Panel panel = panelService.getPanelById(panelId);
                PanelItem newItem = new PanelItem();
                newItem.setTest(test);
                newItem.setPanel(panel);
                newItem.setSysUserId(currentUserId);
                panelItemService.insert(newItem);

                // Ensure sample type - panel association
                if (panel != null) {
                    // Get all sample-type associations for THIS specific panel (not any panel)
                    List<org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel> panelSampleTypes = typeOfSamplePanelService
                            .getTypeOfSamplePanelsForPanel(panelId);
                    for (TestSet set : testSets) {
                        boolean pairExists = panelSampleTypes.stream()
                                .anyMatch(tosp -> tosp.getTypeOfSampleId().equals(set.typeOfSample.getId()));
                        if (!pairExists) {
                            org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel tosp = new org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel();
                            tosp.setPanelId(panelId);
                            tosp.setTypeOfSampleId(set.typeOfSample.getId());
                            tosp.setSysUserId(currentUserId);
                            typeOfSamplePanelService.insert(tosp);
                        }
                    }
                }
            }
        }
    }

    private void syncResultsAndLimits(Test test, List<TestSet> testSets, CatalogDefinitionRequest request,
            String currentUserId) {
        String resultTypeId = entityResolver.resolveResultTypeId(request.getResultTypeId(),
                request.getResultTypeName());

        if (TypeOfTestResultServiceImpl.ResultType.isNumericById(resultTypeId)) {
            syncNumericResults(test, testSets, resultTypeId, currentUserId);
        } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(resultTypeId)) {
            syncDictionaryResults(test, testSets, resultTypeId, currentUserId);
        }
    }

    private void syncNumericResults(Test test, List<TestSet> testSets, String resultTypeId, String currentUserId) {
        List<TestResult> existingResults = testResultService.getActiveTestResultsByTest(test.getId());
        List<ResultLimit> existingLimits = resultLimitService.getAllResultLimitsForTest(test.getId());

        // For Numeric, we usually expect ONE TestResult
        TestResult desiredTr = testSets.get(0).testResults.get(0);
        if (existingResults.isEmpty()) {
            desiredTr.setTest(test);
            desiredTr.setSysUserId(currentUserId);
            testResultService.insert(desiredTr);
        } else {
            TestResult existingTr = existingResults.get(0);
            if (isDifferent(existingTr.getSignificantDigits(), desiredTr.getSignificantDigits())
                    || isDifferent(existingTr.getTestResultType(), desiredTr.getTestResultType())) {
                existingTr.setSignificantDigits(desiredTr.getSignificantDigits());
                existingTr.setTestResultType(desiredTr.getTestResultType());
                existingTr.setSysUserId(currentUserId);
                testResultService.update(existingTr);
            }
            // Remove extra results if any
            for (int i = 1; i < existingResults.size(); i++) {
                testResultService.delete(existingResults.get(i));
            }
        }

        // Sync Limits
        List<ResultLimit> desiredLimits = testSets.get(0).resultLimits;

        // Remove limits not in payload (match by gender, age range)
        for (ResultLimit existingLimit : existingLimits) {
            boolean found = desiredLimits.stream().anyMatch(dl -> isSameRange(existingLimit, dl));
            if (!found) {
                existingLimit.setSysUserId(currentUserId);
                resultLimitService.delete(existingLimit);
            }
        }

        // Add or Update limits
        for (ResultLimit dl : desiredLimits) {
            ResultLimit match = existingLimits.stream().filter(el -> isSameRange(el, dl)).findFirst().orElse(null);

            if (match == null) {
                dl.setTestId(test.getId());
                dl.setSysUserId(currentUserId);
                resultLimitService.insert(dl);
            } else {
                if (isLimitDifferent(match, dl)) {
                    updateLimitFields(match, dl);
                    match.setSysUserId(currentUserId);
                    resultLimitService.update(match);
                }
            }
        }
    }

    private void syncDictionaryResults(Test test, List<TestSet> testSets, String resultTypeId, String currentUserId) {
        List<TestResult> existingResults = testResultService.getActiveTestResultsByTest(test.getId());
        List<TestResult> desiredResults = testSets.get(0).testResults;

        // Safety guard: if the payload resolved to zero entries (all lookups returned
        // null or no dictionaryConfig was sent), do NOT wipe existing answers.
        // This prevents a garbage payload from destroying real data.
        if (desiredResults.isEmpty()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "syncDictionaryResults",
                    "No resolved dictionary entries for test " + test.getId()
                            + " — skipping result sync to preserve existing answers");
            return;
        }

        // Sync TestResults (Coded Answers)
        // Soft-delete (deactivate) rather than hard-delete — same as the normal UI
        // flow.
        // Hard-deleting TestResult rows breaks Test.default_test_result_id FK and
        // crashes Hibernate on next getAllTests() call.
        for (TestResult existingTr : existingResults) {
            boolean found = desiredResults.stream().anyMatch(dr -> isSameDictionaryEntry(existingTr, dr));
            if (!found) {
                existingTr.setIsActive(false);
                existingTr.setSysUserId(currentUserId);
                testResultService.update(existingTr);

                // If we are deactivating the result that was the default, clear it from the
                // test
                if (test.getDefaultTestResult() != null
                        && test.getDefaultTestResult().getId().equals(existingTr.getId())) {
                    test.setDefaultTestResult(null);
                }
            }
        }

        for (TestResult dr : desiredResults) {
            TestResult match = existingResults.stream().filter(er -> isSameDictionaryEntry(er, dr)).findFirst()
                    .orElse(null);

            if (match == null) {
                dr.setTest(test);
                dr.setSysUserId(currentUserId);
                testResultService.insert(dr);
                if (dr.getDefault()) {
                    test.setDefaultTestResult(dr);
                }
            } else {
                if (isDifferent(match.getDefault(), dr.getDefault())
                        || isDifferent(match.getIsQuantifiable(), dr.getIsQuantifiable())
                        || isDifferent(match.getSortOrder(), dr.getSortOrder())) {
                    match.setDefault(dr.getDefault());
                    match.setIsQuantifiable(dr.getIsQuantifiable());
                    match.setSortOrder(dr.getSortOrder());
                    match.setSysUserId(currentUserId);
                    testResultService.update(match);
                }
                if (dr.getDefault()) {
                    test.setDefaultTestResult(match);
                }
            }
        }

        // Ensure default is flushed back to Test table
        testService.update(test);

        // Sync Dictionary Reference Limit
        List<ResultLimit> existingLimits = resultLimitService.getAllResultLimitsForTest(test.getId());
        List<ResultLimit> desiredLimits = testSets.get(0).resultLimits;

        if (desiredLimits.isEmpty()) {
            existingLimits.forEach(resultLimitService::delete);
        } else {
            ResultLimit dl = desiredLimits.get(0);
            if (existingLimits.isEmpty()) {
                dl.setTestId(test.getId());
                dl.setSysUserId(currentUserId);
                resultLimitService.insert(dl);
            } else {
                ResultLimit el = existingLimits.get(0);
                if (isDifferent(el.getDictionaryNormalId(), dl.getDictionaryNormalId())) {
                    el.setDictionaryNormalId(dl.getDictionaryNormalId());
                    el.setSysUserId(currentUserId);
                    resultLimitService.update(el);
                }
            }
        }
    }

    private boolean isDifferent(Object o1, Object o2) {
        if (o1 == null && o2 == null)
            return false;
        if (o1 == null || o2 == null)
            return true;
        return !o1.equals(o2);
    }

    private boolean isSameRange(ResultLimit l1, ResultLimit l2) {
        return isDifferent(l1.getGender(), l2.getGender()) == false
                && isDifferent(l1.getMinAge(), l2.getMinAge()) == false
                && isDifferent(l1.getMaxAge(), l2.getMaxAge()) == false;
    }

    private boolean isLimitDifferent(ResultLimit l1, ResultLimit l2) {
        return isDifferent(l1.getLowNormal(), l2.getLowNormal()) || isDifferent(l1.getHighNormal(), l2.getHighNormal())
                || isDifferent(l1.getLowValid(), l2.getLowValid()) || isDifferent(l1.getHighValid(), l2.getHighValid())
                || isDifferent(l1.getLowCritical(), l2.getLowCritical())
                || isDifferent(l1.getHighCritical(), l2.getHighCritical())
                || isDifferent(l1.getLowReportingRange(), l2.getLowReportingRange())
                || isDifferent(l1.getHighReportingRange(), l2.getHighReportingRange());
    }

    private void updateLimitFields(ResultLimit target, ResultLimit source) {
        target.setLowNormal(source.getLowNormal());
        target.setHighNormal(source.getHighNormal());
        target.setLowValid(source.getLowValid());
        target.setHighValid(source.getHighValid());
        target.setLowCritical(source.getLowCritical());
        target.setHighCritical(source.getHighCritical());
        target.setLowReportingRange(source.getLowReportingRange());
        target.setHighReportingRange(source.getHighReportingRange());
    }

    private boolean isSameDictionaryEntry(TestResult r1, TestResult r2) {
        return isDifferent(r1.getValue(), r2.getValue()) == false;
    }

    private void updateLocalizations(Test existing, CatalogDefinitionRequest request, String currentUserId) {
        Localization nameLoc = existing.getLocalizedTestName();
        if (nameLoc == null) {
            nameLoc = LocalizationServiceImpl.createNewLocalization(request.getNameEnglish(),
                    request.getNameFrench() != null ? request.getNameFrench() : request.getNameEnglish(),
                    LocalizationServiceImpl.LocalizationType.TEST_NAME);
            nameLoc.setSysUserId(currentUserId);
            localizationService.insert(nameLoc);
            existing.setLocalizedTestName(nameLoc);
        } else {
            // Set unconditionally — Hibernate handles the dirty check at flush time
            nameLoc.setLocalizedValue("en", request.getNameEnglish());
            nameLoc.setLocalizedValue("fr",
                    request.getNameFrench() != null ? request.getNameFrench() : request.getNameEnglish());
            nameLoc.setSysUserId(currentUserId);
            localizationService.update(nameLoc);
        }

        Localization reportLoc = existing.getLocalizedReportingName();
        String reportEn = request.getReportNameEnglish() != null ? request.getReportNameEnglish()
                : request.getNameEnglish();
        String reportFr = request.getReportNameFrench() != null ? request.getReportNameFrench() : reportEn;
        if (reportLoc == null) {
            reportLoc = LocalizationServiceImpl.createNewLocalization(reportEn, reportFr,
                    LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
            reportLoc.setSysUserId(currentUserId);
            localizationService.insert(reportLoc);
            existing.setLocalizedReportingName(reportLoc);
        } else {
            reportLoc.setLocalizedValue("en", reportEn);
            reportLoc.setLocalizedValue("fr", reportFr);
            reportLoc.setSysUserId(currentUserId);
            localizationService.update(reportLoc);
        }
    }

    private List<TestSet> buildTestSets(CatalogDefinitionRequest request, String currentUserId) {
        List<TestSet> testSets = new ArrayList<>();

        UnitOfMeasure uom = entityResolver.resolveUOM(request.getUomId(), request.getUomName(), currentUserId);
        TestSection section = entityResolver.resolveTestSection(request.getTestSectionId(),
                request.getTestSectionName(), currentUserId);
        String resultTypeId = entityResolver.resolveResultTypeId(request.getResultTypeId(),
                request.getResultTypeName());

        List<String> resolvedSampleTypeIds = new ArrayList<>(request.getSampleTypeIds());
        for (String stName : request.getSampleTypeNames()) {
            TypeOfSample tos = entityResolver.resolveSampleType(null, stName, currentUserId);
            if (tos != null)
                resolvedSampleTypeIds.add(tos.getId());
        }

        // Ensure at least one sample type or use default
        if (resolvedSampleTypeIds.isEmpty()) {
            resolvedSampleTypeIds.add(entityResolver.resolveSampleType(null, null, currentUserId).getId());
        }

        for (String sampleTypeId : resolvedSampleTypeIds) {
            TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleById(sampleTypeId);
            if (sampleType == null)
                continue;

            TestSet set = testAddController.new TestSet();
            set.typeOfSample = sampleType;

            Test test = new Test();
            test.setUnitOfMeasure(uom);
            test.setLoinc(request.getLoincCode());
            test.setPrice(roundPrice(request.getPrice()));

            // Hibernate/DB limits: name=255, description=60
            String baseName = request.getNameEnglish();
            test.setName(baseName.length() > 255 ? baseName.substring(0, 255) : baseName);

            String desc = baseName + "(" + sampleType.getDescription() + ")";
            test.setDescription(desc.length() > 60 ? desc.substring(0, 60) : desc);

            test.setIsActive(request.isActive() ? "Y" : "N");
            test.setOrderable(request.isOrderable());
            test.setNotifyResults(request.isNotifyResults());
            test.setInLabOnly(request.isInLabOnly());
            test.setAntimicrobialResistance(request.isAntimicrobialResistance());
            test.setTestSection(section);
            test.setGuid(request.getTestUuid() != null ? request.getTestUuid() : UUID.randomUUID().toString());
            test.setSysUserId(currentUserId);
            // sort_order must not be null —
            // ResultsValidationUtility.sortByAccessionNumberAndOrder
            // does Integer.parseInt(test.getSortOrder()) and crashes on null.

            test.setSortOrder("0");

            set.test = test;

            TypeOfSampleTest stt = new TypeOfSampleTest();
            stt.setTypeOfSampleId(sampleTypeId);
            set.sampleTypeTest = stt;

            if (TypeOfTestResultServiceImpl.ResultType.isNumericById(resultTypeId)) {
                mapNumericConfig(set, request.getNumericConfig(), resultTypeId);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(resultTypeId)) {
                mapDictionaryConfig(set, request.getDictionaryConfig(), resultTypeId, currentUserId);
            }

            for (String panelId : request.getPanelIds()) {
                PanelItem pi = new PanelItem();
                pi.setPanel(panelService.getPanelById(panelId));
                set.panelItems.add(pi);
            }
            for (String panelName : request.getPanelNames()) {
                // Hibernate limit for Panel.panelName is 20
                String lookupName = panelName.length() > 20 ? panelName.substring(0, 20) : panelName;
                Panel p = panelService.getPanelByName(lookupName);
                if (p != null) {
                    PanelItem pi = new PanelItem();
                    pi.setPanel(p);
                    set.panelItems.add(pi);
                }
            }

            testSets.add(set);
        }

        if (testSets.isEmpty()) {
            throw new org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException(
                    "No valid sample types could be resolved — check 'sampleTypeNames' / 'sampleTypeIds'");
        }

        return testSets;
    }

    private void mapNumericConfig(TestSet set, CatalogDefinitionRequest.NumericConfig config, String resultTypeId) {
        TestResult tr = new TestResult();
        // Resolve the character value via the enum — getResultTypeById returns the
        // valueholder which does not have getCharacterValue(); the enum does.
        TypeOfTestResultServiceImpl.ResultType resultTypeEnum = null;
        for (TypeOfTestResultServiceImpl.ResultType rt : TypeOfTestResultServiceImpl.ResultType.values()) {
            if (rt.getId().equals(resultTypeId)) {
                resultTypeEnum = rt;
                break;
            }
        }
        if (resultTypeEnum == null) {
            throw new org.openelisglobal.dataexchange.externalcatalog.exception.CatalogValidationException(
                    "Result type ID '" + resultTypeId + "' not found");
        }
        tr.setTestResultType(resultTypeEnum.getCharacterValue());
        tr.setSignificantDigits(config != null ? config.getSignificantDigits() : "2");
        tr.setIsActive(true);
        tr.setSortOrder("1");
        set.testResults.add(tr);

        Double lowValid = config != null && config.getLowValid() != null
                ? StringUtil.doubleWithInfinity(config.getLowValid())
                : Double.NEGATIVE_INFINITY;
        if (lowValid == null)
            lowValid = Double.NEGATIVE_INFINITY;

        Double highValid = config != null && config.getHighValid() != null
                ? StringUtil.doubleWithInfinity(config.getHighValid())
                : Double.POSITIVE_INFINITY;
        if (highValid == null)
            highValid = Double.POSITIVE_INFINITY;

        Double lowCritical = config != null && config.getLowCritical() != null
                ? StringUtil.doubleWithInfinity(config.getLowCritical())
                : Double.NEGATIVE_INFINITY;
        if (lowCritical == null)
            lowCritical = Double.NEGATIVE_INFINITY;

        Double highCritical = config != null && config.getHighCritical() != null
                ? StringUtil.doubleWithInfinity(config.getHighCritical())
                : Double.POSITIVE_INFINITY;
        if (highCritical == null)
            highCritical = Double.POSITIVE_INFINITY;

        Double lowReporting = config != null && config.getLowReportingRange() != null
                ? StringUtil.doubleWithInfinity(config.getLowReportingRange())
                : Double.NEGATIVE_INFINITY;
        if (lowReporting == null)
            lowReporting = Double.NEGATIVE_INFINITY;

        Double highReporting = config != null && config.getHighReportingRange() != null
                ? StringUtil.doubleWithInfinity(config.getHighReportingRange())
                : Double.POSITIVE_INFINITY;
        if (highReporting == null)
            highReporting = Double.POSITIVE_INFINITY;

        if (config != null && !config.getLimits().isEmpty()) {
            for (CatalogDefinitionRequest.ResultLimitDTO limitDTO : config.getLimits()) {
                ResultLimit limit = new ResultLimit();
                limit.setResultTypeId(resultTypeId);
                limit.setGender(limitDTO.getGender());
                // API accepts age in years (may be decimal e.g. 0.5 = 6 months).
                // OE stores and compares age in days — convert here.
                Double minAgeYears = StringUtil.doubleWithInfinity(limitDTO.getLowAge());
                limit.setMinAge(minAgeYears != null ? yearsToDays(minAgeYears) : 0.0);
                Double maxAgeYears = StringUtil.doubleWithInfinity(limitDTO.getHighAge());
                limit.setMaxAge(maxAgeYears != null ? yearsToDays(maxAgeYears) : Double.POSITIVE_INFINITY);
                Double lowNormal = StringUtil.doubleWithInfinity(limitDTO.getLowNormal());
                limit.setLowNormal(lowNormal != null ? lowNormal : Double.NEGATIVE_INFINITY);
                Double highNormal = StringUtil.doubleWithInfinity(limitDTO.getHighNormal());
                limit.setHighNormal(highNormal != null ? highNormal : Double.POSITIVE_INFINITY);
                limit.setLowValid(lowValid);
                limit.setHighValid(highValid);
                limit.setLowCritical(lowCritical);
                limit.setHighCritical(highCritical);
                limit.setLowReportingRange(lowReporting);
                limit.setHighReportingRange(highReporting);
                set.resultLimits.add(limit);
            }
        } else {
            // Default "intervention" limit
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(resultTypeId);
            limit.setLowValid(lowValid);
            limit.setHighValid(highValid);
            limit.setLowNormal(Double.NEGATIVE_INFINITY);
            limit.setHighNormal(Double.POSITIVE_INFINITY);
            limit.setLowCritical(lowCritical);
            limit.setHighCritical(highCritical);
            limit.setLowReportingRange(lowReporting);
            limit.setHighReportingRange(highReporting);
            set.resultLimits.add(limit);
        }
    }

    private void mapDictionaryConfig(TestSet set, CatalogDefinitionRequest.DictionaryConfig config, String resultTypeId,
            String currentUserId) {
        if (config == null)
            return;

        String characterValue = typeOfTestResultService.getResultTypeById(resultTypeId).getCharacterValue();
        int sortOrder = 10;
        for (CatalogDefinitionRequest.DictionaryEntryDTO entryDTO : config.getEntries()) {
            Dictionary dict = entityResolver.resolveDictionaryEntry(entryDTO.getDictionaryId(),
                    entryDTO.getDictionaryUuid(), entryDTO.getDictionaryLoincCode(), entryDTO.getDictionaryName(),
                    currentUserId);
            if (dict != null) {
                TestResult tr = new TestResult();
                tr.setTestResultType(characterValue);
                tr.setSortOrder(String.valueOf(sortOrder));
                tr.setIsActive(true);
                tr.setValue(dict.getId());
                tr.setDefault(entryDTO.isDefault());
                tr.setIsQuantifiable(entryDTO.isQuantifiable());
                set.testResults.add(tr);
                sortOrder += 10;
            }
        }

        Dictionary refDict = entityResolver.resolveDictionaryEntry(config.getDictionaryReferenceId(), null, null,
                config.getDictionaryReferenceName(), currentUserId);
        if (refDict != null) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(resultTypeId);
            limit.setDictionaryNormalId(refDict.getId());
            set.resultLimits.add(limit);
        }
    }

    @Transactional
    public String upsertPanel(CatalogDefinitionRequest request, String currentUserId) {
        Panel existing = findExistingPanel(request);
        if (existing != null) {
            updateExistingPanel(existing, request, currentUserId);
            return existing.getGuid();
        } else {
            return createNewPanel(request, currentUserId);
        }
    }

    private Panel findExistingPanel(CatalogDefinitionRequest request) {
        if (!GenericValidator.isBlankOrNull(request.getTestUuid())) {
            return panelService.getPanelByGUID(request.getTestUuid());
        }
        if (!GenericValidator.isBlankOrNull(request.getLoincCode())) {
            return panelService.getPanelByLoincCode(request.getLoincCode());
        }
        return null;
    }

    private String createNewPanel(CatalogDefinitionRequest request, String currentUserId) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "createNewPanel",
                "Creating new panel: " + request.getNameEnglish());

        // Fall back to English if French is not provided — localization_value.value is
        // NOT NULL
        String panelNameFr = request.getNameFrench() != null ? request.getNameFrench() : request.getNameEnglish();
        Localization localization = LocalizationServiceImpl.createNewLocalization(request.getNameEnglish(), panelNameFr,
                LocalizationServiceImpl.LocalizationType.PANEL_NAME);

        Panel panel = new Panel();
        // Hibernate limits: name=20, description=60, loinc=10
        String baseName = request.getNameEnglish();
        panel.setPanelName(baseName.length() > 20 ? baseName.substring(0, 20) : baseName);
        panel.setDescription(baseName.length() > 60 ? baseName.substring(0, 60) : baseName);

        panel.setIsActive("N");
        panel.setSortOrderInt(Integer.MAX_VALUE);
        panel.setSysUserId(currentUserId);

        String loinc = request.getLoincCode();
        if (!GenericValidator.isBlankOrNull(loinc)) {
            panel.setLoinc(loinc.length() > 10 ? loinc.substring(0, 10) : loinc);
        }
        panel.setPrice(roundPrice(request.getPrice()));
        panel.setGuid(request.getTestUuid() != null ? request.getTestUuid() : UUID.randomUUID().toString());

        SystemModule workplanModule = createSystemModule("Workplan", request.getNameEnglish(), currentUserId);
        SystemModule resultModule = createSystemModule("LogbookResults", request.getNameEnglish(), currentUserId);
        SystemModule validationModule = createSystemModule("ResultValidation", request.getNameEnglish(), currentUserId);

        Role resultsRole = roleService.getRoleByName(Constants.ROLE_RESULTS);
        Role validationRole = roleService.getRoleByName(Constants.ROLE_VALIDATION);

        RoleModule workplanRM = createRoleModule(currentUserId, workplanModule, resultsRole);
        RoleModule resultRM = createRoleModule(currentUserId, resultModule, resultsRole);
        RoleModule validationRM = createRoleModule(currentUserId, validationModule, validationRole);

        TypeOfSample sampleType = entityResolver.resolveSampleType(request.getSampleTypeId(),
                request.getSampleTypeName(), currentUserId);

        panelCreateService.insert(localization, panel, workplanModule, resultModule, validationModule, workplanRM,
                resultRM, validationRM, sampleType.getId(), currentUserId);

        assignPanelMembers(panel, request, currentUserId);

        eventPublisher
                .publishEvent(new org.openelisglobal.dataexchange.externalcatalog.event.CatalogUpsertedEvent(this));
        return panel.getGuid();
    }

    private void updateExistingPanel(Panel existing, CatalogDefinitionRequest request, String currentUserId) {
        LogEvent.logInfo(this.getClass().getSimpleName(), "updateExistingPanel", "Updating panel: " + existing.getId());

        // Apply all scalar fields unconditionally — Hibernate dirty detection handles

        if (!GenericValidator.isBlankOrNull(request.getNameEnglish())) {
            String baseName = request.getNameEnglish();
            // Hibernate limits: panelName=20, description=60
            String truncatedName = baseName.length() > 20 ? baseName.substring(0, 20) : baseName;
            existing.setPanelName(truncatedName);
            existing.setDescription(baseName.length() > 60 ? baseName.substring(0, 60) : baseName);

            // Update the localization rows — this is what the UI actually renders via
            // panel.getLocalizedName() → LocalizationService.getLocalizedValueById()
            Localization loc = existing.getLocalization();
            if (loc != null) {
                String frName = request.getNameFrench() != null ? request.getNameFrench() : baseName;
                loc.setEnglish(baseName);
                loc.setFrench(frName);
                loc.setSysUserId(currentUserId);
                localizationService.update(loc);
            }
        }
        // Only overwrite LOINC if the caller explicitly sent one — never wipe an
        // existing value with null
        if (!GenericValidator.isBlankOrNull(request.getLoincCode())) {
            String loinc = request.getLoincCode();
            // Hibernate limits: loinc=10
            existing.setLoinc(loinc.length() > 10 ? loinc.substring(0, 10) : loinc);
        }
        // Only overwrite price if the caller explicitly sent one
        if (request.getPrice() != null) {
            existing.setPrice(roundPrice(request.getPrice()));
        }
        existing.setSysUserId(currentUserId);
        panelService.update(existing);

        assignPanelMembers(existing, request, currentUserId);
        eventPublisher
                .publishEvent(new org.openelisglobal.dataexchange.externalcatalog.event.CatalogUpsertedEvent(this));
        eventPublisher.publishEvent(new PanelCreatedOrUpdatedEvent(this, existing));
    }

    private void assignPanelMembers(Panel panel, CatalogDefinitionRequest request, String currentUserId) {
        List<PanelItem> existingItems = panelItemService.getPanelItemsForPanel(panel.getId());
        List<Test> desiredTests = new ArrayList<>();

        // If the panel is being explicitly deactivated, clear all desired tests
        if (!request.isActive()) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "assignPanelMembers",
                    "Deactivating panel " + panel.getId() + ". Clearing all member tests.");
            // No tests are desired, so the existing sync logic will remove all current
            // members
        } else {
            for (String testUuid : request.getMemberTestUuids()) {
                Test test = testService.getTestByGUID(testUuid);
                if (test != null) {
                    desiredTests.add(test);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignPanelMembers",
                            "memberTestUuid '" + testUuid + "' not found — skipping");
                }
            }
            for (String loinc : request.getMemberTestLoincCodes()) {
                List<Test> tests = testService.getActiveTestsByLoinc(loinc);
                if (tests != null && !tests.isEmpty()) {
                    desiredTests.add(tests.get(0));
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignPanelMembers",
                            "memberTestLoincCode '" + loinc + "' matched no active test — skipping");
                }
            }
            for (String testName : request.getMemberTestNames()) {
                Test test = testService.getTestByName(testName);
                if (test != null) {
                    desiredTests.add(test);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "assignPanelMembers",
                            "memberTestName '" + testName + "' not found — skipping");
                }
            }
        }

        // Granular Sync instead of panelItemService.updatePanelItems
        List<String> desiredTestIds = desiredTests.stream().map(Test::getId).toList();

        // Remove those not in payload
        for (PanelItem item : existingItems) {
            if (item.getTest() == null || !desiredTestIds.contains(item.getTest().getId())) {
                item.setSysUserId(currentUserId);
                panelItemService.delete(item);
            }
        }

        // Add those not in DB
        for (Test test : desiredTests) {
            boolean exists = existingItems.stream()
                    .anyMatch(item -> item.getTest() != null && item.getTest().getId().equals(test.getId()));
            if (!exists) {
                PanelItem newItem = new PanelItem();
                newItem.setPanel(panel);
                newItem.setTest(test);
                newItem.setSysUserId(currentUserId);
                panelItemService.insert(newItem);
            }
        }

        // Update active status based on membership
        String newActive = !desiredTests.isEmpty() ? "Y" : "N";
        if (!newActive.equals(panel.getIsActive())) {
            panel.setIsActive(newActive);
            panel.setSysUserId(currentUserId);
            panelService.update(panel);
        }
    }

    /** Rounds price to 2 decimal places (HALF_UP). Defaults to 1.00 if null. */
    private BigDecimal roundPrice(BigDecimal price) {
        return (price != null ? price : BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Converts age in years (decimal OK — e.g. 0.5 = 6 months) to days. OE stores
     * and compares all age limits in days internally. Infinity passes through
     * unchanged.
     */
    private double yearsToDays(double years) {
        if (years == Double.POSITIVE_INFINITY)
            return Double.POSITIVE_INFINITY;
        if (years == Double.NEGATIVE_INFINITY)
            return Double.NEGATIVE_INFINITY;
        return years * 365.25;
    }

    private SystemModule createSystemModule(String type, String name, String userId) {
        SystemModule sm = new SystemModule();
        // Hibernate limits: name=32, description=80
        String moduleName = type + ":" + name;
        sm.setSystemModuleName(moduleName.length() > 32 ? moduleName.substring(0, 32) : moduleName);

        String desc = type + "=>panel=>" + name;
        sm.setDescription(desc.length() > 80 ? desc.substring(0, 80) : desc);

        sm.setSysUserId(userId);
        sm.setHasAddFlag("Y");
        sm.setHasDeleteFlag("Y");
        sm.setHasSelectFlag("Y");
        sm.setHasUpdateFlag("Y");
        return sm;
    }

    private RoleModule createRoleModule(String userId, SystemModule sm, Role role) {
        RoleModule rm = new RoleModule();
        rm.setSystemModule(sm);
        rm.setRole(role);
        rm.setSysUserId(userId);
        rm.setHasAdd("Y");
        rm.setHasDelete("Y");
        rm.setHasSelect("Y");
        rm.setHasUpdate("Y");
        return rm;
    }
}
