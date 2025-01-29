package org.openelisglobal.testconfiguration.controller.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
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
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.testconfiguration.validator.TestAddFormValidator;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestAddRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "jsonWad", "loinc" };

    @Autowired
    private TestAddFormValidator formValidator;
    @Autowired
    private DisplayListService displayListService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestAddService testAddService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private TestService testService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/TestAdd")
    public TestAddForm showTestAdd(HttpServletRequest request) {
        LogEvent.logTrace(this.getClass().getSimpleName(), "showTestAdd",
                "Hibernate Version: " + org.hibernate.Version.getVersionString());

        TestAddForm form = new TestAddForm();
        Test test = new Test();

        List<IdValuePair> allSampleTypesList = new ArrayList<>();
        allSampleTypesList.addAll(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_ACTIVE));
        allSampleTypesList.addAll(DisplayListService.getInstance().getList(ListType.SAMPLE_TYPE_INACTIVE));

        form.setSampleTypeList(allSampleTypesList);
        form.setPanelList(DisplayListService.getInstance().getList(ListType.PANELS));
        form.setResultTypeList(DisplayListService.getInstance().getList(ListType.RESULT_TYPE_LOCALIZED));
        form.setUomList(DisplayListService.getInstance().getList(ListType.UNIT_OF_MEASURE));

        List<IdValuePair> allLabUnitsList = new ArrayList<>();
        allLabUnitsList.addAll(DisplayListService.getInstance().getList(ListType.TEST_SECTION_ACTIVE));
        allLabUnitsList.addAll(DisplayListService.getInstance().getList(ListType.TEST_SECTION_INACTIVE));
        form.setLabUnitList(allLabUnitsList);
        form.setAgeRangeList(SpringContext.getBean(ResultLimitService.class).getPredefinedAgeRanges());
        form.setDictionaryList(DisplayListService.getInstance().getList(ListType.DICTIONARY_TEST_RESULTS));
        form.setGroupedDictionaryList(createGroupedDictionaryList());
        form.setLoinc(test.getLoinc());

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    @PostMapping(value = "/TestAdd")
    public TestAddForm postTestAdd(HttpServletRequest request, @RequestBody @Valid TestAddForm form,
            BindingResult result) {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String currentUserId = getSysUserId(request);
        String jsonString = (form.getJsonWad());

        JSONParser parser = new JSONParser();

        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            LogEvent.logError(e.getMessage(), e);
        }
        TestAddParams testAddParams = extractTestAddParms(obj, parser);
        validateLoinc(testAddParams.loinc, result);
        List<TestSet> testSets = createTestSets(testAddParams);
        Localization nameLocalization = createNameLocalization(testAddParams);
        Localization reportingNameLocalization = createReportingNameLocalization(testAddParams);

        try {
            testAddService.addTestsRest(testSets, nameLocalization, reportingNameLocalization, currentUserId);
        } catch (HibernateException e) {
            LogEvent.logDebug(e);
        }

        testService.refreshTestNames();
        displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
        displayListService.refreshList(DisplayListService.ListType.PANELS);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
        displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
        SpringContext.getBean(TypeOfSampleService.class).clearCache();

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private Errors validateLoinc(String loincCode, Errors errors) {
        List<Test> tests = testService.getTestsByLoincCode(loincCode);
        for (Test test : tests) {
            if (test.getLoinc().equals(loincCode)) {
                errors.reject("entry.invalid.loinc.number.used", "entry.invalid.loinc.number.used");
            }
        }
        return errors;
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
        if (!GenericValidator.isBlankOrNull(testAddParams.uomId) || "0".equals(testAddParams.uomId)) {
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
            } else {
                typeOfSample.setActive("Y".equals(testAddParams.active));
            }
            TestSet testSet = new TestSet();
            testSet.typeOfSample = typeOfSample;
            Test test = new Test();
            test.setUnitOfMeasure(uom);
            test.setLoinc(testAddParams.loinc);
            test.setDescription(testAddParams.testNameEnglish + "(" + typeOfSample.getDescription() + ")");
            // TODO remove test name if possible. Tests should be identified by LOINC and
            // use a localization
            test.setName(testAddParams.testNameEnglish);
            test.setLocalCode(testAddParams.testNameEnglish);
            test.setIsActive(testAddParams.active);
            test.setOrderable("Y".equals(testAddParams.orderable));
            test.setNotifyResults("Y".equals(testAddParams.notifyResults));
            test.setInLabOnly("Y".equals(testAddParams.inLabOnly));
            test.setAntimicrobialResistance("Y".equals(testAddParams.antimicrobialResistance));
            test.setIsReportable("N");
            test.setTestSection(testSection);
            test.setGuid(String.valueOf(UUID.randomUUID()));
            ArrayList<String> orderedTests = testAddParams.sampleList.get(i).orderedTests;
            for (int j = 0; j < orderedTests.size(); j++) {
                if ("0".equals(orderedTests.get(j))) {
                    test.setSortOrder(String.valueOf(j));
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
        ArrayList<ResultLimit> resultLimits = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(testAddParams.dictionaryReferenceId)) {
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
            if (lowCritical != null && highCritical != null) {
                limit.setLowReportingRange(lowReportingRange);
                limit.setHighReportingRange(highReportingRange);
                limit.setLowCritical(lowCritical);
                limit.setHighCritical(highCritical);
            }
            resultLimits.add(limit);
        }

        return resultLimits;
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

    private TestAddParams extractTestAddParms(JSONObject obj, JSONParser parser) {
        TestAddParams testAddParams = new TestAddParams();
        try {

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

    private List<List<IdValuePair>> createGroupedDictionaryList() {
        List<TestResult> testResults = testResultService.getAllSortedTestResults(); // getSortedTestResults();

        HashSet<String> dictionaryIdGroups = getDictionaryIdGroups(testResults);

        return getGroupedDictionaryPairs(dictionaryIdGroups);
    }

    /*
     * @SuppressWarnings("unchecked") private List<TestResult>
     * getSortedTestResults() { List<TestResult> testResults =
     * testResultService.getAllTestResults();
     *
     * Collections.sort(testResults, new Comparator<TestResult>() {
     *
     * @Override public int compare(TestResult o1, TestResult o2) { int result =
     * o1.getTest().getId().compareTo(o2.getTest().getId());
     *
     * if (result != 0) { return result; }
     *
     * return GenericValidator.isBlankOrNull(o1.getSortOrder()) ||
     * GenericValidator.isBlankOrNull(o2.getSortOrder()) ? 0 :
     * Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder()); }
     * }); return testResults; }
     */

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

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testAddDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "redirect:/TestAdd";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testAddDefinition";
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
        String testId;
        public String testNameEnglish;
        public String testNameFrench;
        public String testReportNameEnglish;
        public String testReportNameFrench;
        String testSectionId;
        ArrayList<String> panelList = new ArrayList<>();
        String uomId;
        String loinc;
        String resultTypeId;
        ArrayList<SampleTypeListAndTestOrder> sampleList = new ArrayList<>();
        String active;
        String orderable;
        String notifyResults;
        String inLabOnly;
        String antimicrobialResistance;
        String lowValid;
        String highValid;
        String lowReportingRange;
        String highReportingRange;
        String lowCritical;
        String highCritical;
        public String significantDigits;
        String dictionaryReferenceId;
        ArrayList<ResultLimitParams> limits = new ArrayList<>();
        public ArrayList<DictionaryParams> dictionaryParamList = new ArrayList<>();
    }

    public class TestSet {
        public Test test;
        public TypeOfSampleTest sampleTypeTest;
        public TypeOfSample typeOfSample;
        public ArrayList<Test> sortedTests = new ArrayList<>();
        public ArrayList<PanelItem> panelItems = new ArrayList<>();
        public ArrayList<TestResult> testResults = new ArrayList<>();
        public ArrayList<ResultLimit> resultLimits = new ArrayList<>();
    }

    public class SampleTypeListAndTestOrder {
        String sampleTypeId;
        ArrayList<String> orderedTests = new ArrayList<>();
    }

    public class ResultLimitParams {
        String gender;
        String lowAge;
        String highAge;
        String lowNormalLimit;
        String highNormalLimit;
        String displayRange;
        String lowCritical;
        String highCritical;
    }

    public class DictionaryParams {
        public boolean isDefault;
        public String dictionaryId;
        public boolean isQuantifiable = false;
    }
}
