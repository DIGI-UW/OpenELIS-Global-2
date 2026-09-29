package org.openelisglobal.test.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.security.CrudPrivileges;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.method.valueholder.Method;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.qc.valueholder.TestQcThreshold;
import org.openelisglobal.test.beanItems.TestResultItem.ResultDisplayType;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.security.access.prepost.PreAuthorize;

@CrudPrivileges(write = "PRIV_TEST_CONFIGURE")
public interface TestService extends BaseObjectService<Test, String> {

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    void getData(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getActiveTestById(Integer id);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalTestCount();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getAllActiveTests(boolean onlyTestsFullySetup);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByTestSectionAndMethod(String filter, String filter2);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByTestSectionId(String id);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByTestSectionIds(List<String> ids);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfTestsBySysUserId(int startingRecNo, int sysUserId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getTotalSearchedTestCount(String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    Integer getAllSearchedTotalTestCount(HttpServletRequest request, String searchString);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByTestSection(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfSearchedTests(int startingRecNo, String searchString);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getAllTestsBySysUserId(int sysUserId, boolean onlyTestsFullySetup);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Method> getMethodsByTestSection(String filter);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getActiveTestsByLoinc(String loincCode);

    @PreAuthorize("hasAuthority('PRIV_ORDER_VIEW')")
    List<Test> getAllActiveOrderableTests();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByDescription(String description);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByNormalizedDescription(String description);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByLocalCode(String localCode);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByNormalizedDescriptionPrefix(String plainName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByLoincCode(String loincCode);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getActiveTestsByLoinc(String[] loincCodes);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getAllOrderBy(String columnName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean isTestFullySetup(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestById(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestById(String testId);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByMethod(String filter);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    List<Test> getPageOfTests(int startingRecNo);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTests(String filter, boolean onlyTestsFullySetup);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getAllTests(boolean onlyTestsFullySetup);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
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

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    String getTestMethodName(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<TestResult> getPossibleTestResults(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    String getUOM(Test test, boolean isCD4Conclusion);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    boolean isReportable(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    String getSortOrder(Test test);

    /**
     * Primary (first-linked) sample type only — a test may associate several
     * (OGC-1145); use {@link #getTypeOfSamples(Test)} or the specimen-aware lookups
     * when a specimen is in context.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    TypeOfSample getTypeOfSample(Test test);

    /** All sample types associated with the test (OGC-1145 m:n model). */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<TypeOfSample> getTypeOfSamples(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Panel> getPanels(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    String getTestSectionName(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    ResultDisplayType getDisplayTypeForTestMethod(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    String getResultType(Test test);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getAllTestsByDictionaryResult();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByLocalizedName(String testName, Locale locale);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getActiveTestsByName(String testName) throws LIMSRuntimeException;

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getActiveTestsByPanel(String panelName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getActiveTestByLocalizedName(String testName, Locale locale) throws LIMSRuntimeException;

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTestsByName(String testName) throws LIMSRuntimeException;

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByLocalizedName(String testName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Test getTestByName(String testName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getActiveTestByName(String testName);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTbTestByMethod(String method);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTbTest();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Panel> getTbPanelsByMethod(String method);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Optional<Test> getActiveTestByLoincCodeAndSampleType(String loincCode, String sampleTypeId);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void deactivateAllTests();

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void activateTests(List<String> testNames);

    @PreAuthorize("hasAuthority('PRIV_TEST_CONFIGURE')")
    void activateTestsAndDeactivateOthers(List<String> asList);

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    List<Test> getTriggeringAntimicrobialResistanceTests();

    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Optional<TestQcThreshold> getQcThreshold(String testId);

    /**
     * Resolves the {@code localization} ids backing a test's localizable name
     * fields, so the editor can read/write per-locale values through the existing
     * {@code /rest/localizations/{id}} endpoints (no per-test translation store).
     * Keys are field names ("name", "reportingName"); a field is omitted when the
     * test has no localization link for it.
     */
    @PreAuthorize("hasAnyAuthority('PRIV_RESULT_VIEW','PRIV_CATALOGUE_VIEW')")
    Map<String, String> getNameLocalizationIds(String testId);
}
