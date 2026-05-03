package org.openelisglobal.test.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.qc.valueholder.TestQcThreshold;
import org.openelisglobal.test.beanItems.TestResultItem.ResultDisplayType;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TestService extends BaseObjectService<Test, String> {

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    void getData(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getActiveTestById(Integer id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalTestCount();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getAllActiveTests(boolean onlyTestsFullySetup);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByTestSectionAndMethod(String filter, String filter2);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByTestSectionId(String id);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByTestSectionIds(List<String> ids);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfTestsBySysUserId(int startingRecNo, int sysUserId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalSearchedTestCount(String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getAllSearchedTotalTestCount(HttpServletRequest request, String searchString);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByTestSection(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfSearchedTests(int startingRecNo, String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getAllTestsBySysUserId(int sysUserId, boolean onlyTestsFullySetup);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Method> getMethodsByTestSection(String filter);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getActiveTestsByLoinc(String loincCode);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<Test> getAllActiveOrderableTests();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByDescription(String description);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByNormalizedDescription(String description);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByLoincCode(String loincCode);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getActiveTestsByLoinc(String[] loincCodes);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getAllOrderBy(String columnName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean isTestFullySetup(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestById(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestById(String testId);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByMethod(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfTests(int startingRecNo);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTests(String filter, boolean onlyTestsFullySetup);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getAllTests(boolean onlyTestsFullySetup);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByGUID(String guid);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalSearchedTestCountBySysUserId(int sysUserId, String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getNextAvailableSortOrderByTestSection(Test test);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfSearchedTestsBySysUserId(int startingRecNo, int sysUserId, String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void localeChanged(String locale);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void refreshTestNames();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getTestMethodName(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TestResult> getPossibleTestResults(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getUOM(Test test, boolean isCD4Conclusion);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    boolean isReportable(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getSortOrder(Test test);

    /**
     * Primary (first-linked) sample type only — a test may associate several
     * (OGC-1145); use {@link #getTypeOfSamples(Test)} or the specimen-aware lookups
     * when a specimen is in context.
     */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    TypeOfSample getTypeOfSample(Test test);

    /** All sample types associated with the test (OGC-1145 m:n model). */
    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<TypeOfSample> getTypeOfSamples(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Panel> getPanels(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getTestSectionName(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    ResultDisplayType getDisplayTypeForTestMethod(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    String getResultType(Test test);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getAllTestsByDictionaryResult();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByLocalizedName(String testName, Locale locale);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getActiveTestsByPanel(String panelName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTestsByName(String testName) throws LIMSRuntimeException;

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByLocalizedName(String testName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Test getTestByName(String testName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getActiveTestByName(String testName);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTbTestByMethod(String method);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTbTest();

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Panel> getTbPanelsByMethod(String method);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    Optional<Test> getActiveTestByLoincCodeAndSampleType(String loincCode, String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void deactivateAllTests();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void activateTests(List<String> testNames);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void activateTestsAndDeactivateOthers(List<String> asList);

    @PreAuthorize("hasAuthority('PRIV_RESULT_VIEW')")
    List<Test> getTriggeringAntimicrobialResistanceTests();

    Optional<TestQcThreshold> getQcThreshold(String testId);

    /**
     * Resolves the {@code localization} ids backing a test's localizable name
     * fields, so the editor can read/write per-locale values through the existing
     * {@code /rest/localizations/{id}} endpoints (no per-test translation store).
     * Keys are field names ("name", "reportingName"); a field is omitted when the
     * test has no localization link for it.
     */
    Map<String, String> getNameLocalizationIds(String testId);
}
