package org.openelisglobal.test.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.test.beanItems.TestResultItem.ResultDisplayType;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;

public interface TestService extends BaseObjectService<Test, String> {

    void getData(Test test);

    Test getActiveTestById(Integer id);

    Integer getTotalTestCount();

    List<Test> getAllActiveTests(boolean onlyTestsFullySetup);

    List<Test> getTestsByTestSectionAndMethod(String filter, String filter2);

    List<Test> getTestsByTestSectionId(String id);

    List<Test> getTestsByTestSectionIds(List<Integer> ids);

    List<Test> getPageOfTestsBySysUserId(int startingRecNo, int sysUserId);

    Integer getTotalSearchedTestCount(String searchString);

    Integer getAllSearchedTotalTestCount(HttpServletRequest request, String searchString);

    List<Test> getTestsByTestSection(String filter);

    List<Test> getPageOfSearchedTests(int startingRecNo, String searchString);

    List<Test> getAllTestsBySysUserId(int sysUserId, boolean onlyTestsFullySetup);

    List<Method> getMethodsByTestSection(String filter);

    List<Test> getActiveTestsByLoinc(String loincCode);

    List<Test> getAllActiveOrderableTests();

    Test getTestByDescription(String description);

    List<Test> getTestsByLoincCode(String loincCode);

    List<Test> getActiveTestsByLoinc(String[] loincCodes);

    List<Test> getAllOrderBy(String columnName);

    boolean isTestFullySetup(Test test);

    Test getTestById(Test test);

    Test getTestById(String testId);

    List<Test> getTestsByMethod(String filter);

    List<Test> getPageOfTests(int startingRecNo);

    List<Test> getTests(String filter, boolean onlyTestsFullySetup);

    List<Test> getAllTests(boolean onlyTestsFullySetup);

    Test getTestByGUID(String guid);

    Integer getTotalSearchedTestCountBySysUserId(int sysUserId, String searchString);

    Integer getNextAvailableSortOrderByTestSection(Test test);

    List<Test> getPageOfSearchedTestsBySysUserId(int startingRecNo, int sysUserId, String searchString);

    void localeChanged(String locale);

    void refreshTestNames();

    String getTestMethodName(Test test);

    List<TestResult> getPossibleTestResults(Test test);

    String getUOM(Test test, boolean isCD4Conclusion);

    boolean isReportable(Test test);

    String getSortOrder(Test test);

    TypeOfSample getTypeOfSample(Test test);

    List<Panel> getPanels(Test test);

    String getTestSectionName(Test test);

    ResultDisplayType getDisplayTypeForTestMethod(Test test);

    String getResultType(Test test);

    List<Test> getAllTestsByDictionaryResult();

    Test getTestByLocalizedName(String testName, Locale locale);

    List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException;

    List<Test> getActiveTestsByPanel(String panelName);

    Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException;

    List<Test> getTestsByName(String testName) throws LIMSRuntimeException;

    Test getTestByLocalizedName(String testName);

    Test getTestByName(String testName);

    List<Test> getActiveTestByName(String testName);

    List<Test> getTbTestByMethod(String method);

    List<Test> getTbTest();

    List<Panel> getTbPanelsByMethod(String method);

    Optional<Test> getActiveTestByLoincCodeAndSampleType(String loincCode, String sampleTypeId);

    void deactivateAllTests();

    void activateTests(List<String> testNames);

    void activateTestsAndDeactivateOthers(List<String> asList);

    List<Test> getTriggeringAntimicrobialResistanceTests();
}
