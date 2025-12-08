package org.openelisglobal.test.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.LocaleChangeListener;
import org.openelisglobal.internationalization.GlobalLocaleResolver;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.beanItems.TestResultItem.ResultDisplayType;
import org.openelisglobal.test.dao.TestDAO;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.LocaleResolver;

@Service
@DependsOn({ "springContext" })
public class TestServiceImpl extends AuditableBaseObjectServiceImpl<Test, String>
        implements TestService, LocaleChangeListener {

    public enum Entity {
        TEST_NAME, TEST_AUGMENTED_NAME, TEST_REPORTING_NAME
    }

    public static final String HIV_TYPE = "HIV_TEST_KIT";
    public static final String SYPHILIS_TYPE = "SYPHILIS_TEST_KIT";
    private static String VARIABLE_TYPE_OF_SAMPLE_ID;
    // private static String LANGUAGE_LOCALE = ConfigurationProperties.getInstance()
    // .getPropertyValue(ConfigurationProperties.Property.DEFAULT_LANG_LOCALE);
    private static Map<Entity, Map<String, String>> entityToMap;

    protected static TestDAO baseObjectDAO = SpringContext.getBean(TestDAO.class);

    private static TestResultService testResultService = SpringContext.getBean(TestResultService.class);
    private static TypeOfSampleTestService typeOfSampleTestService = SpringContext
            .getBean(TypeOfSampleTestService.class);
    private static TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    private PanelItemService panelItemService = SpringContext.getBean(PanelItemService.class);
    private PanelService panelService = SpringContext.getBean(PanelService.class);
    private TestAnalyteService testAnalyteService = SpringContext.getBean(TestAnalyteService.class);
    private TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
    private LocalizationService localizationService = SpringContext.getBean(LocalizationService.class);

    @PostConstruct
    private void initialize() {
        LocaleResolver localeResolver = SpringContext.getBean(LocaleResolver.class);
        if (localeResolver instanceof GlobalLocaleResolver) {
            ((GlobalLocaleResolver) localeResolver).addLocalChangeListener(this);
        }
    }

    synchronized void initializeGlobalVariables() {
        TypeOfSample variableTypeOfSample = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain("Variable", "H");
        VARIABLE_TYPE_OF_SAMPLE_ID = variableTypeOfSample == null ? "-1" : variableTypeOfSample.getId();

        if (entityToMap == null) {
            createEntityMap();
        }
    }

    private synchronized void createEntityMap() {
        entityToMap = new HashMap<>();
        entityToMap.put(Entity.TEST_NAME, createTestIdToNameMap());
        entityToMap.put(Entity.TEST_AUGMENTED_NAME, createTestIdToAugmentedNameMap());
        entityToMap.put(Entity.TEST_REPORTING_NAME, createTestIdToReportingNameMap());
    }

    public TestServiceImpl() {
        super(Test.class);
        initializeGlobalVariables();
    }

    @Override
    protected TestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    public static List<Test> getTestsInTestSectionById(String testSectionId) {
        return baseObjectDAO.getTestsByTestSectionId(testSectionId);
    }

    @Override
    public void localeChanged(String locale) {
        // LANGUAGE_LOCALE = locale;
        refreshTestNames();
    }

    @Override
    public void refreshTestNames() {
        entityToMap.put(Entity.TEST_NAME, createTestIdToNameMap());
        entityToMap.put(Entity.TEST_AUGMENTED_NAME, createTestIdToAugmentedNameMap());
        entityToMap.put(Entity.TEST_REPORTING_NAME, createTestIdToReportingNameMap());
    }

    @Override
    public String getTestMethodName(Test test) {
        return (test != null && test.getMethod() != null) ? test.getMethod().getMethodName() : null;
    }

    public boolean isActive(Test test) {
        return test == null ? false : test.isActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResult> getPossibleTestResults(Test test) {
        return testResultService.getAllActiveTestResultsPerTest(test);
    }

    @Override
    public String getUOM(Test test, boolean isCD4Conclusion) {
        if (!isCD4Conclusion) {
            if (test != null && test.getUnitOfMeasure() != null) {
                return test.getUnitOfMeasure().getName();
            }
        }

        return "";
    }

    @Override
    public boolean isReportable(Test test) {
        return test != null && "Y".equals(test.getIsReportable());
    }

    @Override
    public String getSortOrder(Test test) {
        return test == null ? "0" : test.getSortOrder();
    }

    @Override
    public ResultDisplayType getDisplayTypeForTestMethod(Test test) {
        String methodName = getTestMethodName(test);

        if (HIV_TYPE.equals(methodName)) {
            return ResultDisplayType.HIV;
        } else if (SYPHILIS_TYPE.equals(methodName)) {
            return ResultDisplayType.SYPHILIS;
        }

        return TestResultItem.ResultDisplayType.TEXT;
    }

    @Override
    @Transactional(readOnly = true)
    public String getResultType(Test test) {
        String testResultType = TypeOfTestResultServiceImpl.ResultType.NUMERIC.getCharacterValue();
        List<TestResult> testResults = getPossibleTestResults(test);

        if (testResults != null && !testResults.isEmpty()) {
            testResultType = testResults.get(0).getTestResultType();
        }

        return testResultType;
    }

    @Override
    @Transactional(readOnly = true)
    public TypeOfSample getTypeOfSample(Test test) {
        if (test == null) {
            return null;
        }

        List<TypeOfSampleTest> typeOfSampleTests = typeOfSampleTestService.getTypeOfSampleTestsForTest(test.getId());

        if (typeOfSampleTests == null || typeOfSampleTests.isEmpty()) {
            return null;
        }

        String typeOfSampleId = typeOfSampleTests.get(0).getTypeOfSampleId();

        return typeOfSampleService.getTypeOfSampleById(typeOfSampleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Panel> getPanels(Test test) {
        List<Panel> panelList = new ArrayList<>();
        if (test != null) {
            List<PanelItem> panelItemList = panelItemService.getPanelItemByTestId(test.getId());
            for (PanelItem panelItem : panelItemList) {
                panelList.add(panelItem.getPanel());
            }
        }

        return panelList;
    }

    @Transactional(readOnly = true)
    public List<Panel> getAllPanels() {
        return panelService.getAllPanels();
    }

    @Transactional(readOnly = true)
    public TestSection getTestSection(Test test) {
        return test == null ? null : test.getTestSection();
    }

    @Override
    @Transactional(readOnly = true)
    public String getTestSectionName(Test test) {
        return testSectionService.getUserLocalizedTesSectionName(getTestSection(test));
    }

    public static Map<String, String> getMap(Entity entiy) {
        return entityToMap.get(entiy);
    }

    public static String getUserLocalizedTestName(Test test) {
        if (test == null) {
            return "";
        }

        return getUserLocalizedTestName(test.getId());
    }

    public static String getUserLocalizedReportingTestName(String testId) {
        ensureEntityMapInitialized();
        if (entityToMap == null || entityToMap.get(Entity.TEST_REPORTING_NAME) == null) {
            return "";
        }
        String name = entityToMap.get(Entity.TEST_REPORTING_NAME).get(testId);
        return name == null ? "" : name;
    }

    private static synchronized void ensureEntityMapInitialized() {
        if (entityToMap == null) {
            TestServiceImpl instance = SpringContext.getBean(TestServiceImpl.class);
            if (instance != null) {
                instance.initializeGlobalVariables();
            }
        }
    }

    public static String getUserLocalizedReportingTestName(Test test) {
        if (test == null) {
            return "";
        }

        return getUserLocalizedReportingTestName(test.getId());
    }

    public static String getUserLocalizedTestName(String testId) {
        String name = entityToMap.get(Entity.TEST_NAME).get(testId);
        return name == null ? "" : name;
    }

    /**
     * Returns the test name augmented with the sample type IF
     * ConfigurationProperties.Property.TEST_NAME_AUGMENTED is true. If it is not
     * true just the test name will be returned. The test name will be correct for
     * the current locale
     *
     * @param test The test for which we want the name
     * @return The test name or the augmented test name
     */
    public static String getLocalizedTestNameWithType(Test test) {
        if (test == null) {
            return "";
        }

        return getLocalizedTestNameWithType(test.getId());
    }

    /**
     * Returns the test name augmented with the sample type IF
     * ConfigurationProperties.Property.TEST_NAME_AUGMENTED is true. If it is not
     * true just the test name will be returned. The test name will be correct for
     * the current locale
     *
     * @param testId The test id of the test for which we want the name
     * @return The test name or the augmented test name
     */
    public static String getLocalizedTestNameWithType(String testId) {
        ensureEntityMapInitialized();
        if (entityToMap == null || entityToMap.get(Entity.TEST_AUGMENTED_NAME) == null) {
            return "";
        }
        String description = entityToMap.get(Entity.TEST_AUGMENTED_NAME).get(testId);
        return description == null ? "" : description;
    }

    private static Map<String, String> createTestIdToNameMap() {
        Map<String, String> testIdToNameMap = new HashMap<>();

        List<Test> tests = baseObjectDAO.getAllTests(false);

        for (Test test : tests) {
            testIdToNameMap.put(test.getId(), buildTestName(test).replace("\n", " "));
        }

        return testIdToNameMap;
    }

    private static String buildTestName(Test test) {
        Localization localization = test.getLocalizedTestName();

        try {
            return localization.getLocalizedValue();
            // if
            // (LANGUAGE_LOCALE.equals(ConfigurationProperties.LOCALE.FRENCH.getRepresentation()))
            // {
            // return localization.getFrench();
            // } else {
            // return localization.getEnglish();
            // }
        } catch (RuntimeException e) {
            LogEvent.logInfo("TestServiceImpl", "buildTestName", "buildTestName caught LAZY");
            return "ts:btn:284:name";
        }
    }

    @Transactional
    private Map<String, String> createTestIdToAugmentedNameMap() {
        Map<String, String> testIdToNameMap = new HashMap<>();

        List<Test> tests = baseObjectDAO.getAllTests(false);

        for (Test test : tests) {
            testIdToNameMap.put(test.getId(), buildAugmentedTestName(test).replace("\n", " "));
        }

        return testIdToNameMap;
    }

    @Transactional
    private static Map<String, String> createTestIdToReportingNameMap() {
        Map<String, String> testIdToNameMap = new HashMap<>();

        List<Test> tests = baseObjectDAO.getAllActiveTests(false);

        for (Test test : tests) {
            testIdToNameMap.put(test.getId(), buildReportingTestName(test));
        }

        return testIdToNameMap;
    }

    private static String buildReportingTestName(Test test) {
        Localization localization = test.getLocalizedReportingName();

        try {
            return localization.getLocalizedValue();
            // if
            // (LANGUAGE_LOCALE.equals(ConfigurationProperties.LOCALE.FRENCH.getRepresentation()))
            // {
            // return localization.getFrench();
            // } else {
            // return localization.getEnglish();
            // }
        } catch (RuntimeException e) {
            LogEvent.logInfo("TestServiceImpl", "buildReportingTestName", "reporting caught LAZY");
            return "ts:brtn:322:name";
        }
    }

    private String buildAugmentedTestName(Test test) {
        Localization localization = test.getLocalizedTestName();

        String sampleName = "";

        if (ConfigurationProperties.getInstance()
                .isPropertyValueEqual(ConfigurationProperties.Property.TEST_NAME_AUGMENTED, "true")) {
            TypeOfSample typeOfSample = getTypeOfSample(test);
            if (typeOfSample != null && !typeOfSample.getId().equals(VARIABLE_TYPE_OF_SAMPLE_ID)) {
                sampleName = "(" + typeOfSample.getLocalizedName() + ")";
            }
        }

        try {
            return localization.getLocalizedValue() + sampleName;
            // if
            // (LANGUAGE_LOCALE.equals(ConfigurationProperties.LOCALE.FRENCH.getRepresentation()))
            // {
            // return localization.getFrench() + sampleName;
            // } else {
            // return localization.getEnglish() + sampleName;
            // // return "ts:batn:342:name:" + test.getDescription();
            // }
        } catch (RuntimeException e) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "buildAugmentedTestName", "augmented caught LAZY");
            return "ts:batn:345:name:" + test.getDescription();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getData(Test test) {
        getBaseObjectDAO().getData(test);
    }

    @Override
    @Transactional(readOnly = true)
    public Test getActiveTestById(Integer id) {
        return getBaseObjectDAO().getActiveTestById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByLocalizedName(String testName, Locale locale) {
        return getBaseObjectDAO().getTestByLocalizedName(testName, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalTestCount() {
        return getBaseObjectDAO().getTotalTestCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllActiveTests(boolean onlyTestsFullySetup) {
        List<Test> tests = getBaseObjectDAO().getAllActiveTests(onlyTestsFullySetup);
        return filterOnlyFullSetup(onlyTestsFullySetup, tests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSectionAndMethod(String filter, String filter2) {
        return getBaseObjectDAO().getTestsByTestSectionAndMethod(filter, filter2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSectionId(String id) {
        return getBaseObjectDAO().getTestsByTestSectionId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedTestCount(String searchString) {
        return getBaseObjectDAO().getTotalSearchedTestCount(searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByTestSection(String filter) {
        return getBaseObjectDAO().getTestsByTestSection(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfSearchedTests(int startingRecNo, String searchString) {
        return getBaseObjectDAO().getPageOfSearchedTests(startingRecNo, searchString);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Method> getMethodsByTestSection(String filter) {
        return getBaseObjectDAO().getMethodsByTestSection(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getActiveTestsByLoinc(String loincCode) {
        return getBaseObjectDAO().getActiveTestsByLoinc(loincCode);
    }

    @Override
    public List<Test> getActiveTestsByLoinc(String[] loincCodes) {
        return getBaseObjectDAO().getActiveTestsByLoinc(loincCodes);
    }

    @Override
    public Optional<Test> getActiveTestByLoincCodeAndSampleType(String loincCode, String sampleTypeId) {
        List<Test> tests = getBaseObjectDAO().getActiveTestsByLoinc(loincCode);
        for (Test test : tests) {
            for (TypeOfSampleTest typeOfSampleTest : typeOfSampleTestService
                    .getTypeOfSampleTestsForTest(test.getId())) {
                if (typeOfSampleTest.getTypeOfSampleId().equals(sampleTypeId)) {
                    return Optional.of(test);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllActiveOrderableTests() {
        return getBaseObjectDAO().getAllActiveOrderableTests();
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByDescription(String description) {
        return getBaseObjectDAO().getTestByDescription(description);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByLoincCode(String loincCode) {
        return getBaseObjectDAO().getTestsByLoincCode(loincCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllOrderBy(String columnName) {
        return getBaseObjectDAO().getAllOrderBy(columnName);
    }

    @Override
    public boolean isTestFullySetup(Test test) {
        try {
            List<TestAnalyte> testAnalytesByTest = testAnalyteService.getAllTestAnalytesPerTest(test);
            boolean result = true;
            if (testAnalytesByTest == null || testAnalytesByTest.size() == 0) {
                result = false;
            } else {
                // bugzilla 2291 make sure none of the components has a null
                // result group
                boolean atLeastOneResultGroupFound = false;
                for (int j = 0; j < testAnalytesByTest.size(); j++) {
                    TestAnalyte testAnalyte = testAnalytesByTest.get(j);
                    if (testAnalyte.getResultGroup() == null) {
                        atLeastOneResultGroupFound = true;
                        break;
                    }
                }
                if (atLeastOneResultGroupFound) {
                    result = false;
                }
            }
            return result;
        } catch (RuntimeException e) {
            // bugzilla 2154
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in isTestFullySetup()", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestById(Test test) {
        return getBaseObjectDAO().getTestById(test);
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestById(String testId) {
        return getBaseObjectDAO().getTestById(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTestsByMethod(String filter) {
        return getBaseObjectDAO().getTestsByMethod(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfTests(int startingRecNo) {
        return getBaseObjectDAO().getPageOfTests(startingRecNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getTests(String filter, boolean onlyTestsFullySetup) {
        List<Test> tests = getBaseObjectDAO().getTests(filter, onlyTestsFullySetup);
        return filterOnlyFullSetup(onlyTestsFullySetup, tests);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTests(boolean onlyTestsFullySetup) {
        List<Test> tests = getBaseObjectDAO().getAllTests(onlyTestsFullySetup);
        return filterOnlyFullSetup(onlyTestsFullySetup, tests);
    }

    @Override
    @Transactional(readOnly = true)
    public Test getTestByGUID(String guid) {
        return getBaseObjectDAO().getTestByGUID(guid);
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getNextAvailableSortOrderByTestSection(Test test) {
        return getBaseObjectDAO().getNextAvailableSortOrderByTestSection(test);
    }

    @Override
    public void delete(Test test) {
        Test oldTest = get(test.getId());
        oldTest.setIsActive(IActionConstants.NO);
        oldTest.setSysUserId(test.getSysUserId());
        updateDelete(oldTest);
    }

    @Override
    public String insert(Test test) {
        if (test.getIsActive().equals(IActionConstants.YES) && getBaseObjectDAO().duplicateTestExists(test)) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for " + test.getDescription());
        }

        return super.insert(test);
    }

    private List<Test> filterOnlyFullSetup(boolean onlyTestsFullySetup, List<Test> list) {
        if (onlyTestsFullySetup && list != null && list.size() > 0) {
            Iterator<Test> testIterator = list.iterator();
            list = new Vector<>();
            while (testIterator.hasNext()) {
                Test test = testIterator.next();
                if (isTestFullySetup(test)) {
                    list.add(test);
                }
            }
        }
        return list;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTestsByDictionaryResult() {
        return getBaseObjectDAO().getAllTestsByDictionaryResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfTestsBySysUserId(int startingRecNo, int sysUserId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getAllSearchedTotalTestCount(HttpServletRequest request, String searchString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getAllTestsBySysUserId(int sysUserId, boolean onlyTestsFullySetup) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getTotalSearchedTestCountBySysUserId(int sysUserId, String searchString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Test> getPageOfSearchedTestsBySysUserId(int startingRecNo, int sysUserId, String searchString) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException {
        return getBaseObjectDAO().getActiveTestsByName(testName);
    }

    @Override
    public Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException {
        return getBaseObjectDAO().getActiveTestByLocalizedName(testName, locale);
    }

    @Override
    public List<Test> getTestsByName(String testName) throws LIMSRuntimeException {
        return getBaseObjectDAO().getTestsByName(testName);
    }

    @Override
    public Test getTestByLocalizedName(String testName) {
        return getBaseObjectDAO().getTestByLocalizedName(testName, localizationService.getCurrentLocale());
    }

    @Override
    public Test getTestByName(String testName) {
        return getTestByLocalizedName(testName);
    }

    @Override
    public List<Test> getActiveTestByName(String testName) {
        return getBaseObjectDAO().getActiveTestsByName(testName);
    }

    @Override
    public List<Test> getActiveTestsByPanel(String panelName) {
        return getBaseObjectDAO().getActiveTestsByPanelName(panelName);
    }

    @Override
    @Transactional
    public void deactivateAllTests() {
        for (Test test : getBaseObjectDAO().getAll()) {
            test.setIsActive("N");
        }
    }

    @Override
    @Transactional
    public void activateTests(List<String> testNames) {
        for (Test test : getBaseObjectDAO().getAll()) {
            if (testNames.contains(test.getLocalizedTestName().getEnglish())
                    || testNames.contains(test.getLocalizedTestName().getFrench())) {
                test.setIsActive("Y");
            }
        }
    }

    @Override
    @Transactional
    public void activateTestsAndDeactivateOthers(List<String> testNames) {
        deactivateAllTests();
        activateTests(testNames);
    }

    @Override
    public List<Test> getTestsByTestSectionIds(List<Integer> ids) {
        return getBaseObjectDAO().getTestsByTestSectionIds(ids);
    }

    @Override
    public List<Test> getTbTestByMethod(String method) {
        return getBaseObjectDAO().getTbTestByMethod(method);
    }

    @Override
    public List<Test> getTbTest() {
        return getBaseObjectDAO().getTbTest();
    }

    @Override
    public List<Panel> getTbPanelsByMethod(String method) {
        return getBaseObjectDAO().getTbPanelsByMethod(method);
    }

    @Override
    public List<Test> getTriggeringAntimicrobialResistanceTests() {
        return getAllMatching("antimicrobialResistance", Boolean.TRUE).stream()
                .filter(e -> TestReflexUtil.isTriggeringReflexTestId(e.getId())).collect(Collectors.toList());
    }
}
