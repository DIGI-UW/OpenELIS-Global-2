package org.openelisglobal.testconfiguration.controller.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.commons.lang.math.NumberUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestActivationBean;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testconfiguration.form.TestActivationForm;
import org.openelisglobal.testconfiguration.service.TestActivationService;
import org.openelisglobal.testconfiguration.validator.TestActivationFormValidator;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestActivationRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] { "jsonChangeList", "activeTestList*.sampleType.id",
            "inactiveTestList*.sampleType.id" };

    @Autowired
    private TestActivationFormValidator formValidator;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestActivationService testActivationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/TestActivation")
    public TestActivationForm showTestActivation(HttpServletRequest request) {
        TestActivationForm form = new TestActivationForm();

        setupDisplayItems(form);

        // return findForward(FWD_SUCCESS, form);
        return form;
    }

    private void setupDisplayItems(TestActivationForm form) {
        List<TestActivationBean> activeTestList = createTestList(true, false);
        List<TestActivationBean> inactiveTestList = createTestList(false, false);
        form.setActiveTestList(activeTestList);
        form.setInactiveTestList(inactiveTestList);
    }

    private List<TestActivationBean> createTestList(boolean active, boolean refresh) {
        ArrayList<TestActivationBean> testList = new ArrayList<>();

        if (refresh) {
            DisplayListService.getInstance().refreshList(active ? DisplayListService.ListType.SAMPLE_TYPE_ACTIVE
                    : DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
        }

        List<IdValuePair> sampleTypeList = DisplayListService.getInstance()
                .getList(active ? DisplayListService.ListType.SAMPLE_TYPE_ACTIVE
                        : DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);

        // if not active we use alphabetical ordering, the default is display order
        if (!active) {
            IdValuePair.sortByValue(sampleTypeList);
        }

        for (IdValuePair pair : sampleTypeList) {
            TestActivationBean bean = new TestActivationBean();

            List<Test> tests = typeOfSampleService.getAllTestsBySampleTypeId(pair.getId());
            List<IdValuePair> activeTests = new ArrayList<>();
            List<IdValuePair> inactiveTests = new ArrayList<>();

            // initial ordering will be by display order. Inactive tests will then be
            // re-ordered alphabetically
            Collections.sort(tests, new Comparator<Test>() {
                @Override
                public int compare(Test o1, Test o2) {
                    // compare sort order
                    if (NumberUtils.isNumber(o1.getSortOrder()) && NumberUtils.isNumber(o2.getSortOrder())) {
                        return Integer.parseInt(o1.getSortOrder()) - Integer.parseInt(o2.getSortOrder());
                        // if o2 has no sort order o1 does, o2 is assumed to be higher
                    } else if (NumberUtils.isNumber(o1.getSortOrder())) {
                        return -1;
                        // if o1 has no sort order o2 does, o1 is assumed to be higher
                    } else if (NumberUtils.isNumber(o2.getSortOrder())) {
                        return 1;
                        // else they are considered equal
                    } else {
                        return 0;
                    }
                }
            });

            for (Test test : tests) {
                if (test.isActive()) {
                    activeTests.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
                } else {
                    inactiveTests.add(new IdValuePair(test.getId(), TestServiceImpl.getUserLocalizedTestName(test)));
                }
            }

            IdValuePair.sortByValue(inactiveTests);

            bean.setActiveTests(activeTests);
            bean.setInactiveTests(inactiveTests);
            if (!activeTests.isEmpty() || !inactiveTests.isEmpty()) {
                bean.setSampleType(pair);
                testList.add(bean);
            }
        }

        return testList;
    }

    @PostMapping(value = "/TestActivation")
    public TestActivationForm postTestActivation(HttpServletRequest request,
            @RequestBody @Valid TestActivationForm form, BindingResult result) throws ParseException {
        formValidator.validate(form, result);
        if (result.hasErrors()) {
            saveErrors(result);
            setupDisplayItems(form);
            // return findForward(FWD_FAIL_INSERT, form);
            return form;
        }

        String changeList = form.getJsonChangeList();

        JSONParser parser = new JSONParser();

        JSONObject obj = (JSONObject) parser.parse(changeList);

        List<ActivateSet> activateSampleSets = getActivateSetForActions("activateSample", obj, parser);
        List<String> deactivateSampleIds = getIdsForActions("deactivateSample", obj, parser);
        List<ActivateSet> activateTestSets = getActivateSetForActions("activateTest", obj, parser);
        List<String> deactivateTestIds = getIdsForActions("deactivateTest", obj, parser);

        List<Test> deactivateTests = getDeactivatedTests(deactivateTestIds);
        List<Test> activateTests = getActivatedTests(activateTestSets);
        List<TypeOfSample> deactivateSampleTypes = getDeactivatedSampleTypes(deactivateSampleIds);
        List<TypeOfSample> activateSampleTypes = getActivatedSampleTypes(activateSampleSets);

        try {
            testActivationService.updateAll(deactivateTests, activateTests, deactivateSampleTypes, activateSampleTypes);
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }

        List<TestActivationBean> activeTestList = createTestList(true, true);
        List<TestActivationBean> inactiveTestList = createTestList(false, true);
        form.setActiveTestList(activeTestList);
        form.setInactiveTestList(inactiveTestList);

        // return findForward(FWD_SUCCESS_INSERT, form);
        return form;
    }

    private List<Test> getDeactivatedTests(List<String> testIds) {
        List<Test> tests = new ArrayList<>();

        for (String testId : testIds) {
            Test test = SpringContext.getBean(TestService.class).get(testId);

            test.setIsActive("N");
            test.setSysUserId(getSysUserId(request));
            tests.add(test);
        }

        return tests;
    }

    private List<Test> getActivatedTests(List<ActivateSet> testIds) {
        List<Test> tests = new ArrayList<>();

        for (ActivateSet set : testIds) {
            Test test = SpringContext.getBean(TestService.class).get(set.id);
            test.setIsActive("Y");
            test.setSortOrder(String.valueOf(set.sortOrder * 10));
            test.setSysUserId(getSysUserId(request));
            tests.add(test);
        }

        return tests;
    }

    private List<TypeOfSample> getDeactivatedSampleTypes(List<String> sampleTypeIds) {
        List<TypeOfSample> sampleTypes = new ArrayList<>();

        for (String id : sampleTypeIds) {
            TypeOfSample typeOfSample = SpringContext.getBean(TypeOfSampleService.class)
                    .getTransientTypeOfSampleById(id);
            typeOfSample.setActive(false);
            typeOfSample.setSysUserId(getSysUserId(request));
            sampleTypes.add(typeOfSample);
        }

        return sampleTypes;
    }

    private List<TypeOfSample> getActivatedSampleTypes(List<ActivateSet> sampleTypeSets) {
        List<TypeOfSample> sampleTypes = new ArrayList<>();

        for (ActivateSet set : sampleTypeSets) {
            TypeOfSample typeOfSample = SpringContext.getBean(TypeOfSampleService.class)
                    .getTransientTypeOfSampleById(set.id);
            typeOfSample.setActive(true);
            typeOfSample.setSortOrder(set.sortOrder * 10);
            typeOfSample.setSysUserId(getSysUserId(request));
            sampleTypes.add(typeOfSample);
        }

        return sampleTypes;
    }

    private List<String> getIdsForActions(String key, JSONObject root, JSONParser parser) {
        List<String> list = new ArrayList<>();

        String action = (String) root.get(key);

        try {
            JSONArray actionArray = (JSONArray) parser.parse(action);

            for (int i = 0; i < actionArray.size(); i++) {
                list.add((String) ((JSONObject) actionArray.get(i)).get("id"));
            }
        } catch (ParseException e) {
            LogEvent.logDebug(e);
        }

        return list;
    }

    private List<ActivateSet> getActivateSetForActions(String key, JSONObject root, JSONParser parser) {
        List<ActivateSet> list = new ArrayList<>();

        String action = (String) root.get(key);

        try {
            JSONArray actionArray = (JSONArray) parser.parse(action);

            for (int i = 0; i < actionArray.size(); i++) {
                ActivateSet set = new ActivateSet();
                set.id = String.valueOf(((JSONObject) actionArray.get(i)).get("id"));
                Long longSort = (Long) ((JSONObject) actionArray.get(i)).get("sortOrder");
                set.sortOrder = longSort.intValue();
                list.add(set);
            }
        } catch (ParseException e) {
            LogEvent.logDebug(e);
        }

        return list;
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testActivationDefinition";
        } else if (FWD_SUCCESS_INSERT.equals(forward)) {
            return "testActivationDefinition";
        } else if (FWD_FAIL_INSERT.equals(forward)) {
            return "testActivationDefinition";
        } else {
            return "PageNotFound";
        }
    }

    private class ActivateSet {
        public String id;
        public Integer sortOrder;
    }

    @Override
    protected String getPageTitleKey() {
        return null;
    }

    @Override
    protected String getPageSubtitleKey() {
        return null;
    }
}
