package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import javax.validation.Valid;
import org.hibernate.HibernateException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills;
import org.openelisglobal.testconfiguration.action.TestAddControllerUtills.TestAddParams;
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.testconfiguration.validator.TestAddFormValidator;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestAddRestController extends BaseRestController {

    @Autowired
    private TestAddFormValidator formValidator;
    @Autowired
    private TestAddService testAddService;
    @Autowired
    private TestService testService;
    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private TestResultService testResultService;

    @Autowired
    private TestAddControllerUtills testAddControllerUtills;

    @Autowired
    private DisplayListService displayListService;

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

        String currentUserId = getSysUserId(request);
        String jsonString = (form.getJsonWad());

        JSONParser parser = new JSONParser();

        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            LogEvent.logError(e.getMessage(), e);
        }
        TestAddParams testAddParams = testAddControllerUtills.extractTestAddParms(obj, parser);
        validateLoinc(testAddParams.loinc, result);
        List<TestSet> testSets = testAddControllerUtills.createTestSets(testAddParams);
        Localization nameLocalization = testAddControllerUtills.createNameLocalization(testAddParams);
        Localization reportingNameLocalization = testAddControllerUtills.createReportingNameLocalization(testAddParams);

        try {
            testAddService.addTests(testSets, nameLocalization, reportingNameLocalization, currentUserId);
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

    private List<List<IdValuePair>> createGroupedDictionaryList() {
        List<TestResult> testResults = testResultService.getAllSortedTestResults(); // getSortedTestResults();

        HashSet<String> dictionaryIdGroups = getDictionaryIdGroups(testResults);

        return getGroupedDictionaryPairs(dictionaryIdGroups);
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

}
