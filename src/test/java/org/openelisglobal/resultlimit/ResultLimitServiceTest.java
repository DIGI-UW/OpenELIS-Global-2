package org.openelisglobal.resultlimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimit.valueholder.ComplianceEvaluation;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultLimitServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TestService testService;

    private List<ResultLimit> resultLimitList;
    private static int NUMBER_OF_PAGES = 1;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/result-limit.xml");
    }

    @Test
    public void getAllResultLimits_ShouldReturnAllResultLimits() {
        resultLimitList = resultLimitService.getAllResultLimits();
        assertNotNull(resultLimitList);
        assertEquals(3, resultLimitList.size());
        assertEquals("7002", resultLimitList.get(1).getTestId());
    }

    @Test
    public void getPageOfResultLimits_ShouldReturnPageOfResultLimits() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        resultLimitList = resultLimitService.getPageOfResultLimits(1);
        assertTrue(NUMBER_OF_PAGES >= resultLimitList.size());
    }

    @Test
    public void getData_ShouldReturnDataForASpecificResultLimit() {
        ResultLimit resultLimit = new ResultLimit();
        resultLimit.setId("3");
        resultLimitService.getData(resultLimit);
        assertEquals(10.0, resultLimit.getHighReportingRange(), 0.0);
        assertTrue(resultLimit.isAlwaysValidate());
    }

    @Test
    public void getAllResultLimitsForTest_ShouldReturnAllResultLimitsWithASpecificTestId() {
        resultLimitList = resultLimitService.getAllResultLimitsForTest("7002");
        assertNotNull(resultLimitList);
        assertEquals(2, resultLimitList.size());
        assertFalse(resultLimitList.get(0).isAlwaysValidate());
    }

    @Test
    public void getResultLimitById_ShouldReturnResultLimitUsingAnId() {
        ResultLimit resultLimit = resultLimitService.getResultLimitById("2");
        assertNotNull(resultLimit);
        assertEquals(Timestamp.valueOf("2025-06-02 11:30:00"), resultLimit.getLastupdated());
        assertFalse(resultLimit.isAlwaysValidate());
    }

    @Test
    public void getDisplayAgeRange_ShouldReturnDisplayAgeRangeForASpecificResultLimit() {
        ResultLimit resultLimit = resultLimitService.get("2");
        String displayAgeRange = resultLimitService.getDisplayAgeRange(resultLimit, "/");
        assertNotNull(displayAgeRange);
        assertEquals("19D/0M/0Y/0D/2M/0Y", displayAgeRange);
    }

    @Test
    public void getDisplayValidRange_ShouldReturnDisplayValidRangeForASpecificResultLimit() {
        ResultLimit resultLimit = resultLimitService.get("2");
        String displayValidRange = resultLimitService.getDisplayValidRange(resultLimit, "2", "-");
        assertNotNull(displayValidRange);
        assertEquals("1.50-12.00", displayValidRange);
    }

    @Test
    public void getDisplayReportingRange_ShouldReturnDisplayReportingRangeForASpecificResultLimit() {
        ResultLimit resultLimit = resultLimitService.get("2");
        String reportingRange = resultLimitService.getDisplayReportingRange(resultLimit, "3", "-");
        assertNotNull(reportingRange);
        assertEquals("1.500-12.000", reportingRange);
    }

    @Test
    public void getDisplayCriticalRange_ShouldReturnDisplayCriticalRangeForASpecificResultLimit() {
        ResultLimit resultLimit = resultLimitService.get("2");
        String displayCriticalRange = resultLimitService.getDisplayCriticalRange(resultLimit, "3", "-");
        assertNotNull(displayCriticalRange);
        assertEquals("Any value", displayCriticalRange);

        // It returns anyValue because there's no column int the table that matches
        // high_critical and low_critical
    }

    @Test
    public void getDisplayReferenceRange_ShouldReturnDisplayReferenceRangeForASpecificResultLimit() {
        ResultLimit resultLimit = resultLimitService.get("2");
        String displayReferenceRange = resultLimitService.getDisplayReferenceRange(resultLimit, "2", ":");
        assertNotNull(displayReferenceRange);
        assertEquals("3.50:9.50", displayReferenceRange);
    }

    @Test
    public void getDisplayNormalRange_ShouldReturnDisplayNormalRangeForASpecificResultLimit() {
        String displayNormalRange = resultLimitService.getDisplayNormalRange(4.0, 10.0, "3", ":");
        assertNotNull(displayNormalRange);
        assertEquals("4.000:10.000", displayNormalRange);
    }

    // @Test
    public void getResultLimitForTestAndPatient_ShouldReturnAResultLimitsWithASpecificTestIdAndPatient() {
        Patient patient = new Patient();
        patient.setId("1");
        patient.setBirthDate(Timestamp.valueOf("2025-06-02 11:30:00"));
        patient.setGender("M");
        ResultLimit resultLimit = resultLimitService.getResultLimitForTestAndPatient("7002", patient);
        assertNotNull(resultLimit);
        assertEquals(Timestamp.valueOf("2025-06-02 11:30:00.0"), resultLimit.getLastupdated());
        assertTrue(resultLimit.isAlwaysValidate());
    }

    // @Test
    public void getResultLimitForTestAndPatient_ShouldReturnAResultLimitsWithATestAndPatient() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("7002");
        Patient patient = new Patient();
        patient.setId("1");
        patient.setBirthDate(Timestamp.valueOf("2025-06-02 11:30:00"));
        patient.setGender("M");
        ResultLimit resultLimit = resultLimitService.getResultLimitForTestAndPatient(test, patient);
        assertNotNull(resultLimit);
        assertEquals(Timestamp.valueOf("2025-06-02 11:30:00"), resultLimit.getLastupdated());
        assertTrue(resultLimit.isAlwaysValidate());
    }

    // @Test
    public void getPredefinedAgeRanges() {
        List<IdValuePair> predefinedAgeRanges = resultLimitService.getPredefinedAgeRanges();
        assertNotNull(predefinedAgeRanges);
        assertEquals(5, predefinedAgeRanges.size());
        assertEquals("Newborn", predefinedAgeRanges.get(0).getValue());
    }

    private Analysis buildAnalysis(String sampleItemId, String testId) {
        SampleItem sampleItem = sampleItemService.get(sampleItemId);
        org.openelisglobal.test.valueholder.Test test = testService.get(testId);
        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        return analysis;
    }

    // T14: sample with one compliance standard and a matching threshold — result
    // within range returns one PASS evaluation
    @Test
    public void getComplianceResultsForAnalysis_withinRange_returnsPass() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "7.0");

        assertEquals(1, results.size());
        assertEquals("PP 22/2021", results.get(0).standardName());
        assertTrue(results.get(0).pass());
    }

    // T14 (fail path): result outside range returns one FAIL evaluation
    @Test
    public void getComplianceResultsForAnalysis_outsideRange_returnsFail() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "5.0");

        assertEquals(1, results.size());
        assertFalse(results.get(0).pass());
    }

    // T15: sample with two standards both having matching thresholds — returns
    // two evaluations with independent PASS/FAIL verdicts.
    // Value 9.0: within PP 22/2021 [6–9] but outside WHO-DWG-4 [6.5–8.5]
    @Test
    public void getComplianceResultsForAnalysis_twoStandards_returnsTwoEvaluations() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        jdbcTemplate.update("INSERT INTO clinlims.sample_compliance_standards "
                + "(id, sample_id, compliance_standard_id, priority, created_date, last_updated) "
                + "VALUES (9002, 9001, 9002, 1, NOW(), NOW())");

        Analysis analysis = buildAnalysis("9001", "9001");
        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "9.0");

        assertEquals(2, results.size());
        ComplianceEvaluation pp = results.stream().filter(e -> "PP 22/2021".equals(e.standardName())).findFirst()
                .orElse(null);
        ComplianceEvaluation who = results.stream().filter(e -> "WHO-DWG-4".equals(e.standardName())).findFirst()
                .orElse(null);
        assertNotNull(pp);
        assertNotNull(who);
        assertTrue(pp.pass());
        assertFalse(who.pass());
    }

    // T16: sample has a compliance standard but no threshold is linked to the
    // test — complianceStatuses is empty, no exception
    @Test
    public void getComplianceResultsForAnalysis_noMatchingThreshold_returnsEmpty() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        jdbcTemplate.update("UPDATE clinlims.compliance_threshold SET test_id = NULL WHERE id = 9001");

        Analysis analysis = buildAnalysis("9001", "9001");
        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "7.0");

        assertTrue(results.isEmpty());
    }

    // T17: sample has no compliance standards linked — returns empty list
    @Test
    public void getComplianceResultsForAnalysis_noStandardsOnSample_returnsEmpty() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        jdbcTemplate.update("DELETE FROM clinlims.sample_compliance_standards WHERE sample_id = 9001");

        Analysis analysis = buildAnalysis("9001", "9001");
        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "7.0");

        assertTrue(results.isEmpty());
    }

    // T18: blank result value — returns empty list without evaluating thresholds
    @Test
    public void getComplianceResultsForAnalysis_blankResultValue_returnsEmpty() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        assertTrue(resultLimitService.getComplianceResultsForAnalysis(analysis, "").isEmpty());
        assertTrue(resultLimitService.getComplianceResultsForAnalysis(analysis, null).isEmpty());
    }

    // T19: result with < prefix stripped correctly — "<5.0" is outside [6-9], FAIL
    @Test
    public void getComplianceResultsForAnalysis_lessThanPrefix_evaluatesStrippedValue() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "<5.0");

        assertEquals(1, results.size());
        assertFalse(results.get(0).pass());
    }

    // T20: result with > prefix stripped correctly — ">7.0" is within [6-9], PASS
    @Test
    public void getComplianceResultsForAnalysis_greaterThanPrefix_evaluatesStrippedValue() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, ">7.0");

        assertEquals(1, results.size());
        assertTrue(results.get(0).pass());
    }

    // T21: non-numeric result value against numeric threshold — skips evaluation,
    // returns PASS (no threshold can be violated when value is non-numeric)
    @Test
    public void getComplianceResultsForAnalysis_nonNumericResult_returnsPass() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "Positive");

        assertEquals(1, results.size());
        assertTrue(results.get(0).pass());
    }

    // T22: DESCRIPTIVE threshold type is skipped (requiresManualReview) —
    // no auto-evaluation, evaluation returns PASS for the standard
    @Test
    public void getComplianceResultsForAnalysis_descriptiveThreshold_isSkipped() throws Exception {
        executeDataSetWithStateManagement("testdata/compliance-evaluation.xml");
        jdbcTemplate.update("UPDATE clinlims.compliance_threshold SET threshold_type = 'DESCRIPTIVE' WHERE id = 9001");
        Analysis analysis = buildAnalysis("9001", "9001");

        List<ComplianceEvaluation> results = resultLimitService.getComplianceResultsForAnalysis(analysis, "5.0");

        assertEquals(1, results.size());
        assertTrue(results.get(0).pass());
    }

    @Test
    public void getResultLimits_ShouldReturnAResultLimitsWithASpecificTestId() {
        resultLimitList = resultLimitService.getResultLimits("7002");
        assertNotNull(resultLimitList);
        assertEquals(2, resultLimitList.size());
        assertEquals(Timestamp.valueOf("2025-06-03 09:15:00"), resultLimitList.get(1).getLastupdated());
        assertTrue(resultLimitList.get(1).isAlwaysValidate());
    }

    @Test
    public void getResultLimits_ShouldReturnAResultLimitsWithASpecificTest() {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId("7001");
        resultLimitList = resultLimitService.getResultLimits(test);
        assertNotNull(resultLimitList);
        assertEquals(1, resultLimitList.size());
        assertEquals(Timestamp.valueOf("2025-06-01 10:00:00"), resultLimitList.get(0).getLastupdated());
        assertTrue(resultLimitList.get(0).isAlwaysValidate());
    }
}
