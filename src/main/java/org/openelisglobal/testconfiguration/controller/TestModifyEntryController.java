package org.openelisglobal.testconfiguration.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.beans.ResultLimitBean;
import org.openelisglobal.testconfiguration.beans.TestCatalogBean;
import org.openelisglobal.testconfiguration.form.TestModifyEntryForm;
import org.openelisglobal.testconfiguration.service.TestModifyService;
import org.openelisglobal.testconfiguration.validator.TestModifyEntryFormValidator;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TestModifyEntryController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "jsonWad", "testId", "loinc" };

    @Autowired
    private TestModifyEntryFormValidator formValidator;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestModifyService testModifyService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private TestSectionService testSectionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @RequestMapping(value = "/TestModifyEntry", method = RequestMethod.GET)
    public ModelAndView showTestModifyEntry(HttpServletRequest request) {

        TestModifyEntryForm form = new TestModifyEntryForm();
        setupDisplayItems(form);

        return findForward(FWD_SUCCESS, form);
    }

    private void setupDisplayItems(TestModifyEntryForm form) {

        List<IdValuePair> allSampleTypesList = new ArrayList<>();
        allSampleTypesList.addAll(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));
        allSampleTypesList.addAll(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_INACTIVE));

        form.setSampleTypeList(allSampleTypesList);
        form.setPanelList(DisplayListService.getInstance().getList(ListType.PANELS));
        form.setResultTypeList(DisplayListService.getInstance().getList(ListType.RESULT_TYPE_LOCALIZED));
        form.setUomList(DisplayListService.getInstance().getList(ListType.UNIT_OF_MEASURE));
        form.setLabUnitList(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        form.setAgeRangeList(SpringContext.getBean(ResultLimitService.class).getPredefinedAgeRanges());
        form.setDictionaryList(DisplayListService.getInstance().getList(ListType.DICTIONARY_TEST_RESULTS));
        form.setGroupedDictionaryList(createGroupedDictionaryList());
        form.setTestList(DisplayListService.getInstance().getFreshList(DisplayListService.ListType.ALL_TESTS));

        // gnr: ALL_TESTS calls getActiveTests, this could be a way to enable
        // maintenance of inactive tests
        // form.setTestListInactive( DisplayListService.getInstance().getList(
        // DisplayListService.ListType.ALL_TESTS_INACTIVE )
        // );

        List<TestCatalogBean> testCatBeanList = createTestCatBeanList();
        form.setTestCatBeanList(testCatBeanList);
    }

    private List<TestCatalogBean> createTestCatBeanList() {
        List<TestCatalogBean> beanList = new ArrayList<>();

        List<Test> testList = testService.getAllTests(false);

        for (Test test : testList) {

            TestCatalogBean bean = new TestCatalogBean();
            TestService testService = SpringContext.getBean(TestService.class);
            String resultType = testService.getResultType(test);
            bean.setId(test.getId());
            bean.setLocalization(test.getLocalizedTestName());
            bean.setReportLocalization(test.getLocalizedReportingName());
            if (test.getSortOrder() != null) {
                bean.setTestSortOrder(Integer.parseInt(test.getSortOrder()));
            }

            bean.setTestUnit(testService.getTestSectionName(test));
            bean.setPanel(createPanelList(testService, test));
            bean.setResultType(resultType);
            TypeOfSample typeOfSample = testService.getTypeOfSample(test);
            bean.setSampleType(typeOfSample != null ? typeOfSample.getLocalizedName() : "n/a");
            bean.setOrderable(test.getOrderable() ? "Orderable" : "Not orderable");
            bean.setNotifyResults(test.isNotifyResults());
            bean.setInLabOnly(test.isInLabOnly());
            bean.setAntimicrobialResistance(test.getAntimicrobialResistance());
            bean.setLoinc(test.getLoinc());
            bean.setActive(test.isActive() ? "Active" : "Not active");
            bean.setUom(testService.getUOM(test, false));
            if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(resultType)
                    && testResultService.getAllActiveTestResultsPerTest(test).size() != 0) {
                bean.setSignificantDigits(
                        testResultService.getAllActiveTestResultsPerTest(test).get(0).getSignificantDigits());
                bean.setHasLimitValues(true);
                bean.setResultLimits(getResultLimits(test, bean.getSignificantDigits()));
            }
            bean.setHasDictionaryValues(
                    TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(bean.getResultType()));
            if (bean.isHasDictionaryValues()) {
                bean.setDictionaryValues(createDictionaryValues(testService, test));
                bean.setReferenceValue(createReferenceValueForDictionaryType(test));
                bean.setDictionaryIds(createDictionaryIds(testService, test));
                bean.setReferenceId(createReferenceIdForDictionaryType(test));
                bean.setReferenceId(getDictionaryIdByDictEntry(bean.getReferenceValue(), bean.getDictionaryIds(),
                        bean.getDictionaryValues()));
            }
            beanList.add(bean);
        }

        Collections.sort(beanList, new Comparator<TestCatalogBean>() {
            @Override
            public int compare(TestCatalogBean o1, TestCatalogBean o2) {
                // sort by test section, sample type, panel, sort order
                int comparison = o1.getTestUnit().compareTo(o2.getTestUnit());
                if (comparison != 0) {
                    return comparison;
                }

                comparison = o1.getSampleType().compareTo(o2.getSampleType());
                if (comparison != 0) {
                    return comparison;
                }

                comparison = o1.getPanel().compareTo(o2.getPanel());
                if (comparison != 0) {
                    return comparison;
                }

                return o1.getTestSortOrder() - o2.getTestSortOrder();
            }
        });

        return beanList;
    }

    private List<ResultLimitBean> getResultLimits(Test test, String significantDigits) {
        List<ResultLimitBean> limitBeans = new ArrayList<>();

        List<ResultLimit> resultLimitList = SpringContext.getBean(ResultLimitService.class).getResultLimits(test);

        Collections.sort(resultLimitList, new Comparator<ResultLimit>() {
            @Override
            public int compare(ResultLimit o1, ResultLimit o2) {
                return (int) (o1.getMinAge() - o2.getMinAge());
            }
        });

        for (ResultLimit limit : resultLimitList) {
            ResultLimitBean bean = new ResultLimitBean();
            bean.setNormalRange(SpringContext.getBean(ResultLimitService.class).getDisplayReferenceRange(limit,
                    significantDigits, "-"));
            bean.setValidRange(SpringContext.getBean(ResultLimitService.class).getDisplayValidRange(limit,
                    significantDigits, "-"));
            bean.setReportingRange(SpringContext.getBean(ResultLimitService.class).getDisplayReportingRange(limit,
                    significantDigits, "-"));
            bean.setCriticalRange(SpringContext.getBean(ResultLimitService.class).getDisplayCriticalRange(limit,
                    significantDigits, "-"));
            bean.setGender(limit.getGender());
            bean.setAgeRange(SpringContext.getBean(ResultLimitService.class).getDisplayAgeRange(limit, "-"));
            limitBeans.add(bean);
        }
        return limitBeans;
    }

    private String createReferenceValueForDictionaryType(Test test) {
        List<ResultLimit> resultLimits = SpringContext.getBean(ResultLimitService.class).getResultLimits(test);

        if (resultLimits.isEmpty()) {
            return "n/a";
        }

        return SpringContext.getBean(ResultLimitService.class).getDisplayReferenceRange(resultLimits.get(0), null,
                null);
    }

    private List<String> createDictionaryValues(TestService testService, Test test) {
        List<String> dictionaryList = new ArrayList<>();
        List<TestResult> testResultList = testService.getPossibleTestResults(test);
        for (TestResult testResult : testResultList) {
            CollectionUtils.addIgnoreNull(dictionaryList, getDictionaryValue(testResult));
        }

        return dictionaryList;
    }

    private String getDictionaryValue(TestResult testResult) {

        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
            Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());
            String displayValue = dictionary.getLocalizedName();

            if ("unknown".equals(displayValue)) {
                displayValue = !org.apache.commons.validator.GenericValidator.isBlankOrNull(dictionary.getDictEntry())
                        ? dictionary.getDictEntry()
                        : dictionary.getLocalAbbreviation();
            }

            if (testResult.getIsQuantifiable()) {
                displayValue += " Qualifiable";
            }
            return displayValue;
        }

        return null;
    }

    private String createReferenceIdForDictionaryType(Test test) {
        List<ResultLimit> resultLimits = SpringContext.getBean(ResultLimitService.class).getResultLimits(test);

        if (resultLimits.isEmpty()) {
            return "n/a";
        }

        return SpringContext.getBean(ResultLimitService.class).getDisplayReferenceRange(resultLimits.get(0), null,
                null);
    }

    private List<String> createDictionaryIds(TestService testService, Test test) {
        List<String> dictionaryList = new ArrayList<>();
        List<TestResult> testResultList = testService.getPossibleTestResults(test);
        for (TestResult testResult : testResultList) {
            CollectionUtils.addIgnoreNull(dictionaryList, getDictionaryId(testResult));
        }

        return dictionaryList;
    }

    private String getDictionaryIdByDictEntry(String dict_entry, List<String> ids, List<String> values) {

        if ("n/a".equals(dict_entry)) {
            return null;
        }

        for (int i = 0; i < ids.size(); i++) {
            if (values.get(i).equals(dict_entry)) {
                return ids.get(i);
            }
        }

        return null;
    }

    private String getDictionaryId(TestResult testResult) {

        if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
            Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());
            String displayId = dictionary.getId();

            if ("unknown".equals(displayId)) {
                displayId = !org.apache.commons.validator.GenericValidator.isBlankOrNull(dictionary.getDictEntry())
                        ? dictionary.getDictEntry()
                        : dictionary.getLocalAbbreviation();
            }

            if (testResult.getIsQuantifiable()) {
                displayId += " Qualifiable";
            }
            return displayId;
        }

        return null;
    }

    private String createPanelList(TestService testService, Test test) {
        StringBuilder builder = new StringBuilder();

        List<Panel> panelList = testService.getPanels(test);
        for (Panel panel : panelList) {
            builder.append(localizationService.getLocalizedValueById(panel.getLocalization().getId()));
            builder.append(", ");
        }

        String panelString = builder.toString();
        if (panelString.isEmpty()) {
            panelString = "None";
        } else {
            panelString = panelString.substring(0, panelString.length() - 2);
        }

        return panelString;
    }

    private List<List<IdValuePair>> createGroupedDictionaryList() {
        List<TestResult> testResults = getSortedTestResults();

        HashSet<String> dictionaryIdGroups = getDictionaryIdGroups(testResults);

        return getGroupedDictionaryPairs(dictionaryIdGroups);
    }

    @SuppressWarnings("unchecked")
    private List<TestResult> getSortedTestResults() {
        List<TestResult> testResults = testResultService.getAllTestResults();

        Collections.sort(testResults, new Comparator<TestResult>() {
            @Override
            public int compare(TestResult o1, TestResult o2) {
                int result = o1.getTest().getId().compareTo(o2.getTest().getId());

                if (result != 0) {
                    return result;
                }

                return (GenericValidator.isBlankOrNull(o1.getSortOrder())
                        || GenericValidator.isBlankOrNull(o2.getSortOrder())) ? 0
                                : Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
            }
        });
        return testResults;
    }

    private HashSet<String> getDictionaryIdGroups(List<TestResult> testResults) {
        HashSet<String> dictionaryIdGroups = new HashSet<>();
        String currentTestId = null;
        String dictionaryIdGroup = null;
        for (TestResult testResult : testResults) {
            if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(testResult.getTestResultType())) {
                if (testResult.getTest().getId().equals(currentTestId)) {
                    dictionaryIdGroup += "," + testResult.getValue();
                } else {
                    currentTestId = testResult.getTest().getId();
                    if (dictionaryIdGroup != null) {
                        dictionaryIdGroups.add(dictionaryIdGroup);
                    }

                    dictionaryIdGroup = testResult.getValue();
                }
            }
        }

        if (dictionaryIdGroup != null) {
            dictionaryIdGroups.add(dictionaryIdGroup);
        }

        return dictionaryIdGroups;
    }

    private List<List<IdValuePair>> getGroupedDictionaryPairs(HashSet<String> dictionaryIdGroups) {
        List<List<IdValuePair>> groups = new ArrayList<>();
        for (String group : dictionaryIdGroups) {
            List<IdValuePair> dictionaryPairs = new ArrayList<>();
            for (String id : group.split(",")) {
                Dictionary dictionary = dictionaryService.getDictionaryById(id);
                if (dictionary != null) {
                    dictionaryPairs.add(new IdValuePair(id, dictionary.getLocalizedName()));
                }
            }
            groups.add(dictionaryPairs);
        }

        Collections.sort(groups, new Comparator<List<IdValuePair>>() {
            @Override
            public int compare(List<IdValuePair> o1, List<IdValuePair> o2) {
                return o1.size() - o2.size();
            }
        });
        return groups;
    }

    @RequestMapping(value = "/TestModifyEntry", method = RequestMethod.POST)
    public ModelAndView postTestModifyEntry(HttpServletRequest request,
            @ModelAttribute("form") @Valid TestModifyEntryForm form, BindingResult result) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }
        String currentUserId = getSysUserId(request);
        String changeList = form.getJsonWad();

        JSONParser parser = new JSONParser();
        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(changeList);
        } catch (ParseException e) {
            LogEvent.logError(e.getMessage(), e);
        }

        TestAddParams testAddParams = extractTestAddParms(obj, parser);

        Localization nameLocalization = createNameLocalization(testAddParams);
        Localization reportingNameLocalization = createReportingNameLocalization(testAddParams);

        List<TestSet> testSets = createTestSets(testAddParams);

        try {
            testModifyService.updateTestSets(testSets, testAddParams, nameLocalization, reportingNameLocalization,
                    currentUserId);
        } catch (HibernateException e) {
            LogEvent.logDebug(e);
            result.reject("error.hibernate.exception");
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        } catch (Exception e) {
            LogEvent.logDebug(e);
            result.reject("error.exception");
            setupDisplayItems(form);
            return findForward(FWD_FAIL_INSERT, form);
        }

        testService.refreshTestNames();
        SpringContext.getBean(TypeOfSampleService.class).clearCache();

        return findForward(FWD_SUCCESS_INSERT, form);
    }

    private void createPanelItems(ArrayList<PanelItem> panelItems, TestAddParams testAddParams) {
        for (String panelId : testAddParams.panelList) {
            PanelItem panelItem = new PanelItem();
            panelItem.setPanel(panelService.getPanelById(panelId));
            panelItems.add(panelItem);
        }
    }

    private void createTestResults(ArrayList<TestResult> testResults, String significantDigits,
            TestAddParams testAddParams) {
        TypeOfTestResultServiceImpl.ResultType type = SpringContext.getBean(TypeOfTestResultService.class)
                .getResultTypeById(testAddParams.resultTypeId);

        if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(type)
                || TypeOfTestResultServiceImpl.ResultType.isNumeric(type)) {
            TestResult testResult = new TestResult();
            testResult.setTestResultType(type.getCharacterValue());
            testResult.setSortOrder("1");
            testResult.setIsActive(true);
            testResult.setSignificantDigits(significantDigits);
            testResults.add(testResult);
        } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type.getCharacterValue())) {
            int sortOrder = 10;
            for (DictionaryParams params : testAddParams.dictionaryParamList) {
                TestResult testResult = new TestResult();
                testResult.setTestResultType(type.getCharacterValue());
                testResult.setSortOrder(String.valueOf(sortOrder));
                sortOrder += 10;
                testResult.setIsActive(true);
                testResult.setValue(params.dictionaryId);
                testResult.setDefault(params.isDefault);
                testResult.setIsQuantifiable(params.isQuantifiable);
                testResults.add(testResult);
            }
        }
    }

    private Localization createNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testNameEnglish,
                testAddParams.testNameFrench, LocalizationServiceImpl.LocalizationType.TEST_NAME);
    }

    private Localization createReportingNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testReportNameEnglish,
                testAddParams.testReportNameFrench, LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
    }

    private List<TestSet> createTestSets(TestAddParams testAddParams) {
        Double lowValid = null;
        Double highValid = null;
        Double lowReportingRange = null;
        Double highReportingRange = null;
        Double lowCritical = null;
        Double highCritical = null;
        String significantDigits = testAddParams.significantDigits;
        boolean numericResults = TypeOfTestResultServiceImpl.ResultType.isNumericById(testAddParams.resultTypeId);
        boolean dictionaryResults = TypeOfTestResultServiceImpl.ResultType
                .isDictionaryVarientById(testAddParams.resultTypeId);
        List<TestSet> testSets = new ArrayList<>();
        UnitOfMeasure uom = null;
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(testAddParams.uomId)
                || "0".equals(testAddParams.uomId)) {
            uom = unitOfMeasureService.getUnitOfMeasureById(testAddParams.uomId);
        }
        TestSection testSection = testSectionService.get(testAddParams.testSectionId);

        if (numericResults) {
            lowValid = StringUtil.doubleWithInfinity(testAddParams.lowValid);
            highValid = StringUtil.doubleWithInfinity(testAddParams.highValid);
            lowReportingRange = StringUtil.doubleWithInfinity(testAddParams.lowReportingRange);
            highReportingRange = StringUtil.doubleWithInfinity(testAddParams.highReportingRange);
            lowCritical = StringUtil.doubleWithInfinity(testAddParams.lowCritical);
            highCritical = StringUtil.doubleWithInfinity(testAddParams.highCritical);
        }
        // The number of test sets depend on the number of sampleTypes
        for (int i = 0; i < testAddParams.sampleList.size(); i++) {
            TypeOfSample typeOfSample = typeOfSampleService
                    .getTypeOfSampleById(testAddParams.sampleList.get(i).sampleTypeId);
            if (typeOfSample == null) {
                continue;
            }
            TestSet testSet = new TestSet();
            Test test = new Test();
            test.setId(testAddParams.testId);

            test.setUnitOfMeasure(uom);
            test.setDescription(testAddParams.testNameEnglish + "(" + typeOfSample.getDescription() + ")");
            test.setLocalCode(testAddParams.testNameEnglish);
            test.setIsActive(testAddParams.active);
            test.setOrderable("Y".equals(testAddParams.orderable));
            test.setNotifyResults("Y".equals(testAddParams.notifyResults));
            test.setInLabOnly("Y".equals(testAddParams.inLabOnly));
            test.setAntimicrobialResistance("Y".equals(testAddParams.antimicrobialResistance));
            test.setIsReportable("N");
            test.setTestSection(testSection);
            if (GenericValidator.isBlankOrNull(test.getGuid())) {
                test.setGuid(String.valueOf(UUID.randomUUID()));
            }
            ArrayList<String> orderedTests = testAddParams.sampleList.get(i).orderedTests;
            for (int j = 0; j < orderedTests.size(); j++) {
                if ("0".equals(orderedTests.get(j))) {
                    test.setSortOrder(String.valueOf(j));
                    testSet.sortedTests.add(test);
                } else {
                    Test orderedTest = SpringContext.getBean(TestService.class).get(orderedTests.get(j));
                    orderedTest.setSortOrder(String.valueOf(j));
                    testSet.sortedTests.add(orderedTest);
                }
            }

            testSet.test = test;

            TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
            typeOfSampleTest.setTypeOfSampleId(typeOfSample.getId());
            testSet.sampleTypeTest = typeOfSampleTest;

            createPanelItems(testSet.panelItems, testAddParams);
            createTestResults(testSet.testResults, significantDigits, testAddParams);
            if (numericResults) {
                testSet.resultLimits = createResultLimits(lowValid, highValid, lowReportingRange, highReportingRange,
                        testAddParams, highCritical, lowCritical);
            } else if (dictionaryResults) {
                testSet.resultLimits = createDictionaryResultLimit(testAddParams);
            }

            testSets.add(testSet);
        }

        return testSets;
    }

    private ArrayList<ResultLimit> createDictionaryResultLimit(TestAddParams testAddParams) {

        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(testAddParams.testId);
        for (int i = 0; i < testResults.size(); i++) {
            testResults.get(i).setIsActive(false);
        }
        testResultService.updateAll(testResults);

        ArrayList<ResultLimit> resultLimits = new ArrayList<>();
        if (!org.apache.commons.validator.GenericValidator.isBlankOrNull(testAddParams.dictionaryReferenceId)) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setDictionaryNormalId(testAddParams.dictionaryReferenceId);
            resultLimits.add(limit);
        }

        return resultLimits;
    }

    private ArrayList<ResultLimit> createResultLimits(Double lowValid, Double highValid, Double lowReportingRange,
            Double highReportingRange, TestAddParams testAddParams, Double highCritical, Double lowCritical) {
        ArrayList<ResultLimit> resultLimits = new ArrayList<>();
        for (ResultLimitParams params : testAddParams.limits) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setGender(params.gender);
            limit.setMinAge(StringUtil.doubleWithInfinity(params.lowAge));
            limit.setMaxAge(StringUtil.doubleWithInfinity(params.highAge));
            limit.setLowNormal(StringUtil.doubleWithInfinity(params.lowNormalLimit));
            limit.setHighNormal(StringUtil.doubleWithInfinity(params.highNormalLimit));
            limit.setLowValid(lowValid);
            limit.setHighValid(highValid);
            if (lowReportingRange != null && highReportingRange != null && lowCritical != null
                    && highCritical != null) {
                limit.setLowReportingRange(lowReportingRange);
                limit.setHighReportingRange(highReportingRange);
                limit.setLowCritical(lowCritical);
                limit.setHighCritical(highCritical);
            }
            resultLimits.add(limit);
        }

        return resultLimits;
    }

    private TestAddParams extractTestAddParms(JSONObject obj, JSONParser parser) {
        TestAddParams testAddParams = new TestAddParams();
        try {

            testAddParams.testId = (String) obj.get("testId");
            testAddParams.testNameEnglish = (String) obj.get("testNameEnglish");
            testAddParams.testNameFrench = (String) obj.get("testNameFrench");
            testAddParams.testReportNameEnglish = (String) obj.get("testReportNameEnglish");
            testAddParams.testReportNameFrench = (String) obj.get("testReportNameFrench");
            testAddParams.testSectionId = (String) obj.get("testSection");
            testAddParams.dictionaryReferenceId = (String) obj.get("dictionaryReference");
            extractPanels(obj, parser, testAddParams);
            testAddParams.uomId = (String) obj.get("uom");
            testAddParams.loinc = (String) obj.get("loinc");
            testAddParams.resultTypeId = (String) obj.get("resultType");
            extractSampleTypes(obj, parser, testAddParams);
            testAddParams.active = (String) obj.get("active");
            testAddParams.orderable = (String) obj.get("orderable");
            testAddParams.notifyResults = (String) obj.get("notifyResults");
            testAddParams.inLabOnly = (String) obj.get("inLabOnly");
            testAddParams.antimicrobialResistance = (String) obj.get("antimicrobialResistance");
            if (TypeOfTestResultServiceImpl.ResultType.isNumericById(testAddParams.resultTypeId)) {
                testAddParams.lowValid = (String) obj.get("lowValid");
                testAddParams.highValid = (String) obj.get("highValid");
                testAddParams.lowReportingRange = (String) obj.get("lowReportingRange");
                testAddParams.highReportingRange = (String) obj.get("highReportingRange");
                testAddParams.lowCritical = (String) obj.get("lowCritical");
                testAddParams.highCritical = (String) obj.get("highCritical");
                testAddParams.significantDigits = (String) obj.get("significantDigits");
                extractLimits(obj, parser, testAddParams);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(testAddParams.resultTypeId)) {
                String dictionary = (String) obj.get("dictionary");
                JSONArray dictionaryArray = (JSONArray) parser.parse(dictionary);
                for (int i = 0; i < dictionaryArray.size(); i++) {
                    DictionaryParams params = new DictionaryParams();
                    params.dictionaryId = (String) ((JSONObject) dictionaryArray.get(i)).get("value");
                    params.isQuantifiable = "Y".equals(((JSONObject) dictionaryArray.get(i)).get("qualified"));
                    params.isDefault = params.dictionaryId.equals(obj.get("defaultTestResult"));
                    testAddParams.dictionaryParamList.add(params);
                }
            }

        } catch (ParseException e) {
            LogEvent.logDebug(e);
        }

        return testAddParams;
    }

    private void extractLimits(JSONObject obj, JSONParser parser, TestAddParams testAddParams) throws ParseException {
        String lowAge = "0";
        if (obj.containsKey("resultLimits")) {
            String limits = (String) obj.get("resultLimits");
            JSONArray limitArray = (JSONArray) parser.parse(limits);
            for (int i = 0; i < limitArray.size(); i++) {
                ResultLimitParams params = new ResultLimitParams();
                Boolean gender = (Boolean) ((JSONObject) limitArray.get(i)).get("gender");
                if (gender) {
                    params.gender = "M";
                }
                String highAge = (String) (((JSONObject) limitArray.get(i)).get("highAgeRange"));
                params.displayRange = (String) (((JSONObject) limitArray.get(i)).get("reportingRange"));
                params.lowNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("lowNormal"));
                params.highNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("highNormal"));
                params.lowCritical = (String) (((JSONObject) limitArray.get(i)).get("lowCritical"));
                params.highCritical = (String) (((JSONObject) limitArray.get(i)).get("highCritical"));
                params.lowAge = lowAge;
                params.highAge = highAge;
                testAddParams.limits.add(params);

                if (gender) {
                    params = new ResultLimitParams();
                    params.gender = "F";
                    params.lowNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("lowNormalFemale"));
                    params.highNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("highNormalFemale"));
                    params.lowAge = lowAge;
                    params.highAge = highAge;
                    testAddParams.limits.add(params);
                }

                lowAge = highAge;
            }
        }
    }

    private void extractPanels(JSONObject obj, JSONParser parser, TestAddParams testAddParams) throws ParseException {
        String panels = (String) obj.get("panels");
        JSONArray panelArray = (JSONArray) parser.parse(panels);

        for (int i = 0; i < panelArray.size(); i++) {
            testAddParams.panelList.add((String) (((JSONObject) panelArray.get(i)).get("id")));
        }
    }

    private void extractSampleTypes(JSONObject obj, JSONParser parser, TestAddParams testAddParams)
            throws ParseException {
        String sampleTypes = (String) obj.get("sampleTypes");
        JSONArray sampleTypeArray = (JSONArray) parser.parse(sampleTypes);

        for (int i = 0; i < sampleTypeArray.size(); i++) {
            SampleTypeListAndTestOrder sampleTypeTests = new SampleTypeListAndTestOrder();
            sampleTypeTests.sampleTypeId = (String) (((JSONObject) sampleTypeArray.get(i)).get("typeId"));

            JSONArray testArray = (JSONArray) (((JSONObject) sampleTypeArray.get(i)).get("tests"));
            for (int j = 0; j < testArray.size(); j++) {
                sampleTypeTests.orderedTests.add(String.valueOf(((JSONObject) testArray.get(j)).get("id")));
            }
            testAddParams.sampleList.add(sampleTypeTests);
        }
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testModifyDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testModifyDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestModifyEntry";
        } else {
            return "PageNotFound";
        }
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }

    public class TestAddParams {
        public String testId;
        public String testNameEnglish;
        public String testNameFrench;
        public String testReportNameEnglish;
        public String testReportNameFrench;
        public String testSectionId;
        public ArrayList<String> panelList = new ArrayList<>();
        public String uomId;
        public String loinc;
        public String resultTypeId;
        public ArrayList<SampleTypeListAndTestOrder> sampleList = new ArrayList<>();
        public String active;
        public String orderable;
        public String notifyResults;
        public String inLabOnly;
        public String antimicrobialResistance;
        public String lowValid;
        public String highValid;
        public String lowReportingRange;
        public String highReportingRange;
        public String lowCritical;
        public String highCritical;
        public String significantDigits;
        public String dictionaryReferenceId;
        public ArrayList<ResultLimitParams> limits = new ArrayList<>();
        public ArrayList<DictionaryParams> dictionaryParamList = new ArrayList<>();
    }

    public class SampleTypeListAndTestOrder {
        public String sampleTypeId;
        public ArrayList<String> orderedTests = new ArrayList<>();
    }

    public class ResultLimitParams {
        public String gender;
        public String lowAge;
        public String highAge;
        public String lowNormalLimit;
        public String highNormalLimit;
        public String displayRange;
        public String lowCritical;
        public String highCritical;
    }

    public class TestSet {
        public Test test;
        public TypeOfSampleTest sampleTypeTest;
        public ArrayList<Test> sortedTests = new ArrayList<>();
        public ArrayList<PanelItem> panelItems = new ArrayList<>();
        public ArrayList<TestResult> testResults = new ArrayList<>();
        public ArrayList<ResultLimit> resultLimits = new ArrayList<>();
    }

    public class DictionaryParams {
        public Boolean isDefault;
        public String dictionaryId;
        public boolean isQuantifiable = false;
    }
}
