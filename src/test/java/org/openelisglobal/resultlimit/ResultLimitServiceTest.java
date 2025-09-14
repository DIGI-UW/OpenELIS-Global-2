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
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultLimitServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultLimitService resultLimitService;

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
