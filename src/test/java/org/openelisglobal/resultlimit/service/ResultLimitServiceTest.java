package org.openelisglobal.resultlimit.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultLimitServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultLimitService resultLimitService;

    @Autowired
    private TypeOfTestResultService typeOfTestResultService;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/result-limit.xml");
        ensureReferenceTables("result_limits", "test");
    }

    @Test
    public void getAllResultLimits_shouldReturnAllFixtureRows() {
        List<ResultLimit> limits = resultLimitService.getAllResultLimits();
        Assert.assertEquals(3, limits.size());
    }

    @Test
    public void getResultLimitById_shouldReturnMatchingRowDetails() {
        ResultLimit limit = resultLimitService.getResultLimitById("1");
        Assert.assertEquals("1", limit.getId());
        Assert.assertEquals("7001", limit.getTestId());
        Assert.assertEquals("M", limit.getGender());
        Assert.assertEquals(0.0, limit.getMinAge(), 0.001);
        Assert.assertEquals(18.0, limit.getMaxAge(), 0.001);
        Assert.assertEquals(4.0, limit.getLowNormal(), 0.001);
        Assert.assertEquals(10.0, limit.getHighNormal(), 0.001);
        Assert.assertEquals(2.0, limit.getLowValid(), 0.001);
        Assert.assertEquals(15.0, limit.getHighValid(), 0.001);
        Assert.assertEquals(true, limit.isAlwaysValidate());
    }

    @Test
    public void getResultLimits_byTestId_shouldReturnMatchingLimits() {
        List<ResultLimit> limits7002 = resultLimitService.getResultLimits("7002");
        Assert.assertEquals(2, limits7002.size());
        Assert.assertEquals("2", limits7002.get(0).getId());
        Assert.assertEquals("3", limits7002.get(1).getId());
    }

    @Test
    public void getAllResultLimitsForTest_shouldReturnLimitsForSingleTest() {
        List<ResultLimit> limits7001 = resultLimitService.getAllResultLimitsForTest("7001");
        Assert.assertEquals(1, limits7001.size());
        Assert.assertEquals("1", limits7001.get(0).getId());
        Assert.assertEquals("7001", limits7001.get(0).getTestId());
    }

    @Test
    public void getPredefinedAgeRanges_shouldReturnAgeRangeList() {
        List<IdValuePair> ageRanges = resultLimitService.getPredefinedAgeRanges();
        Assert.assertFalse("Age ranges list should not be empty", ageRanges.isEmpty());
        Assert.assertTrue("Age ranges count should be at least 4", ageRanges.size() >= 4);
    }

    @Test
    public void getDisplayNormalRange_shouldFormatRangeWithSignificantDigits() {
        String formattedRange = resultLimitService.getDisplayNormalRange(4.0, 10.0, "2", " - ");
        Assert.assertEquals("4.00 - 10.00", formattedRange);
    }

    @Test
    public void getDisplayAgeRange_shouldFormatAgeRangeCorrectly() {
        ResultLimit limit = resultLimitService.getResultLimitById("1");
        String ageRangeDisplay = resultLimitService.getDisplayAgeRange(limit, " to ");
        Assert.assertTrue("Display should contain min age 0", ageRangeDisplay.contains("0"));
        Assert.assertTrue("Display should contain max age 18", ageRangeDisplay.contains("18"));
    }

    @Test
    public void saveRangesForTest_shouldInsertNewLimitForTest() {
        String numericTypeId = typeOfTestResultService.getTypeOfTestResultByType("N").getId();

        ResultLimit newLimit = new ResultLimit();
        newLimit.setTestId("7001");
        newLimit.setResultTypeId(numericTypeId);
        newLimit.setMinAge(19.0);
        newLimit.setMaxAge(65.0);
        newLimit.setGender("M");
        newLimit.setLowNormal(5.0);
        newLimit.setHighNormal(11.0);
        newLimit.setLowValid(2.5);
        newLimit.setHighValid(16.0);
        newLimit.setSysUserId("1");

        List<ResultLimit> existing = resultLimitService.getAllResultLimitsForTest("7001");
        List<ResultLimit> desired = new ArrayList<>(existing);
        desired.add(newLimit);

        resultLimitService.saveRangesForTest("7001", desired, "1");

        List<ResultLimit> savedLimits = resultLimitService.getAllResultLimitsForTest("7001");
        Assert.assertEquals(2, savedLimits.size());
    }
}
