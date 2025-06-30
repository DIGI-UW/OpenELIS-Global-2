package org.openelisglobal.testconfiguration.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.openelisglobal.common.controller.BaseController;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestCatalog;
import org.openelisglobal.testconfiguration.beans.ResultLimitBean;
import org.openelisglobal.testconfiguration.form.TestCatalogForm;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class TestCatalogRestController extends BaseController {

    private static final String[] ALLOWED_FIELDS = new String[] {};

    @Autowired
    private TestService testService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private LocalizationService localizationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setAllowedFields(ALLOWED_FIELDS);
    }

    @GetMapping(value = "/TestCatalog", produces = MediaType.APPLICATION_JSON_VALUE)
    public TestCatalogForm showTestCatalog(HttpServletRequest request) {
        TestCatalogForm form = new TestCatalogForm();

        List<TestCatalog> testCatalogList = createTestList();
        form.setTestCatalogList(testCatalogList);

        List<String> testSectionList = new ArrayList<>();
        for (TestCatalog testCatalog : testCatalogList) {
            if (!testSectionList.contains(testCatalog.getTestUnit())) {
                testSectionList.add(testCatalog.getTestUnit());
            }
        }
        form.setTestSectionList(testSectionList);

        return (form);
    }

    @Override
    protected String findLocalForward(String forward) {
        if (FWD_SUCCESS.equals(forward)) {
            return "testCatalogDefinition";
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

    private List<TestCatalog> createTestList() {
        List<TestCatalog> catalogList = new ArrayList<>();

        List<Test> testList = testService.getAllTests(false);

        for (Test test : testList) {

            TestCatalog catalog = new TestCatalog();
            String resultType = testService.getResultType(test);
            catalog.setId(test.getId());
            catalog.setLocalization(test.getLocalizedTestName());
            catalog.setReportLocalization(test.getLocalizedReportingName());
            if (NumberUtils.isNumber(test.getSortOrder())) {
                catalog.setTestSortOrder(Integer.parseInt(test.getSortOrder()));
            }
            catalog.setTestUnit(testService.getTestSectionName(test));
            catalog.setPanel(createPanelList(testService, test));
            catalog.setResultType(resultType);
            TypeOfSample typeOfSample = testService.getTypeOfSample(test);
            catalog.setSampleType(typeOfSample != null ? typeOfSample.getLocalizedName() : "n/a");
            catalog.setOrderable(test.getOrderable() ? "Orderable" : "Not orderable");
            catalog.setLoinc(test.getLoinc());
            catalog.setActive(test.isActive() ? "Active" : "Not active");
            catalog.setUom(testService.getUOM(test, false));
            if (TypeOfTestResultServiceImpl.ResultType.NUMERIC.matches(resultType)) {
                List<TestResult> testResults = testService.getPossibleTestResults(test);
                if (testResults.size() > 0) {
                    catalog.setSignificantDigits(
                            testService.getPossibleTestResults(test).get(0).getSignificantDigits());
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createTestList",
                            "test that doesn't have an active test result found. Possibly issue with data in"
                                    + " database");
                    catalog.setSignificantDigits("0");
                }
                catalog.setHasLimitValues(true);
                catalog.setResultLimits(getResultLimits(test, catalog.getSignificantDigits()));
            }
            catalog.setHasDictionaryValues(
                    TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(catalog.getResultType()));
            if (catalog.isHasDictionaryValues()) {
                catalog.setDictionaryValues(createDictionaryValues(testService, test));
                catalog.setReferenceValue(createReferenceValueForDictionaryType(test));
            }
            catalogList.add(catalog);
        }

        Collections.sort(catalogList, new Comparator<TestCatalog>() {
            @Override
            public int compare(TestCatalog o1, TestCatalog o2) {
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

        return catalogList;
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
}
