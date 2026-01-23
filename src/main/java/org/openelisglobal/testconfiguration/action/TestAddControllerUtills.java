package org.openelisglobal.testconfiguration.action;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestAddController;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestAddControllerUtills {

    @Autowired
    private PanelService panelService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private TestAddController testAddController;

    public Localization createNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testNameEnglish,
                testAddParams.testNameFrench, LocalizationServiceImpl.LocalizationType.TEST_NAME);
    }

    public Localization createReportingNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testReportNameEnglish,
                testAddParams.testReportNameFrench, LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
    }

    public List<TestSet> createTestSets(TestAddParams testAddParams) {
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
            TestSet testSet = testAddController.new TestSet();
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

    public TestAddParams extractTestAddParms(JSONObject obj, JSONParser parser) {
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
                testAddParams.lowValid = obj.get("lowValid").toString();
                testAddParams.highValid = obj.get("highValid").toString();
                testAddParams.lowReportingRange = obj.get("lowReportingRange").toString();
                testAddParams.highReportingRange = obj.get("highReportingRange").toString();
                testAddParams.lowCritical = obj.get("lowCritical").toString();
                testAddParams.highCritical = obj.get("highCritical").toString();
                testAddParams.significantDigits = obj.get("significantDigits").toString();
                extractLimits(obj, parser, testAddParams);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(testAddParams.resultTypeId)) {
                JSONArray dictionaryArray = (JSONArray) obj.get("dictionary");
                for (int i = 0; i < dictionaryArray.size(); i++) {
                    DictionaryParams params = new DictionaryParams();
                    params.dictionaryId = (String) ((JSONObject) dictionaryArray.get(i)).get("id");
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
        JSONArray limitArray = (JSONArray) obj.get("resultLimits");
        String globalLowCritical = obj.get("lowCritical").toString();
        String globalHighCritical = obj.get("highCritical").toString();
        String globalLowReporting = obj.get("lowReportingRange").toString();
        String globalHighReporting = obj.get("highReportingRange").toString();
        String globalReportingRange = globalLowReporting + " - " + globalHighReporting;
        for (int i = 0; i < limitArray.size(); i++) {
            ResultLimitParams params = new ResultLimitParams();
            Boolean gender = (Boolean) ((JSONObject) limitArray.get(i)).get("gender");
            if (gender) {
                params.gender = "M";
            }
            String highAge = (((JSONObject) limitArray.get(i)).get("highAgeRange")).toString();
            // params.displayRange = (((JSONObject)
            // limitArray.get(i)).get("reportingRange")).toString();
            params.displayRange = globalReportingRange;
            params.lowNormalLimit = (((JSONObject) limitArray.get(i)).get("lowNormal")).toString();
            params.highNormalLimit = (((JSONObject) limitArray.get(i)).get("highNormal")).toString();
            // params.lowCritical = (((JSONObject)
            // limitArray.get(i)).get("lowCritical")).toString();
            // params.highCritical = (((JSONObject)
            // limitArray.get(i)).get("highCritical")).toString();
            params.lowCritical = globalLowCritical;
            params.highCritical = globalHighCritical;
            params.lowAge = lowAge;
            params.highAge = highAge;
            testAddParams.limits.add(params);

            if (gender) {
                params = new ResultLimitParams();
                params.gender = "F";
                params.lowNormalLimit = (((JSONObject) limitArray.get(i)).get("lowNormalFemale")).toString();
                params.highNormalLimit = (((JSONObject) limitArray.get(i)).get("highNormalFemale")).toString();
                params.lowAge = lowAge;
                params.highAge = highAge;
                testAddParams.limits.add(params);
            }

            lowAge = highAge;
        }
    }

    private void extractPanels(JSONObject obj, JSONParser parser, TestAddParams testAddParams) throws ParseException {
        JSONArray panelArray = (JSONArray) obj.get("panels");

        for (int i = 0; i < panelArray.size(); i++) {
            testAddParams.panelList.add((String) (((JSONObject) panelArray.get(i)).get("id")));
        }
    }

    private void extractSampleTypes(JSONObject obj, JSONParser parser, TestAddParams testAddParams)
            throws ParseException {
        JSONArray sampleTypeArray = (JSONArray) obj.get("sampleTypes");

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

    public class TestAddParams {
        String testId;
        String testNameEnglish;
        String testNameFrench;
        String testReportNameEnglish;
        String testReportNameFrench;
        String testSectionId;
        ArrayList<String> panelList = new ArrayList<>();
        String uomId;
        public String loinc;
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
        String significantDigits;
        String dictionaryReferenceId;
        ArrayList<ResultLimitParams> limits = new ArrayList<>();
        ArrayList<DictionaryParams> dictionaryParamList = new ArrayList<>();
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
