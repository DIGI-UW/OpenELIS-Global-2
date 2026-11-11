package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.result.action.util.ResultUtil;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1226 (FR-7) — automated validation obeys the Clear lane's rule: a result
 * saved at the bench is finalized without a validator only when the queue would
 * have held it clear. Fixture: {@code testdata/validation-bulk-release.xml}
 * (numeric test with reference range 5.0 - 20.0; analysis 104's result carries
 * a recorded QC failure).
 */
public class ResultUtilAutoValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private org.openelisglobal.typeoftestresult.service.TypeOfTestResultService typeOfTestResultService;

    @Autowired
    private org.openelisglobal.resultlimit.service.ResultLimitService resultLimitService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/validation-bulk-release.xml");
        org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult numeric = typeOfTestResultService
                .getTypeOfTestResultByType("N");
        if (numeric == null) {
            jdbcTemplate.update("INSERT INTO clinlims.type_of_test_result (id, description, test_result_type,"
                    + " hl7_value, lastupdated) VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM"
                    + " clinlims.type_of_test_result), 'Numeric', 'N', 'NM', NOW())");
            numeric = typeOfTestResultService.getTypeOfTestResultByType("N");
        }
        jdbcTemplate.update(
                "UPDATE clinlims.result_limits SET max_age = 'Infinity', test_result_type_id = ? WHERE id = 1",
                Integer.valueOf(numeric.getId()));
        ((org.openelisglobal.resultlimit.service.ResultLimitServiceImpl) org.springframework.test.util.AopTestUtils
                .getTargetObject(resultLimitService)).initializeGlobalVariables();
        statusService.refreshCache();
    }

    private TestResultItem entered(String analysisId, String resultType, String value) {
        TestResultItem item = new TestResultItem();
        item.setAnalysisId(analysisId);
        item.setResultType(resultType);
        item.setResultValue(value);
        item.setShadowResultValue(value);
        item.setResultLimitId("1");
        item.setValid(true);
        return item;
    }

    private String status(AnalysisStatus status) {
        return statusService.getStatusID(status);
    }

    @Test
    public void finalizesOnlyWhatTheClearLaneWouldRelease() {
        Analysis firstSave = analysisService.get("100");

        assertEquals("in range, no signals: released at entry", status(AnalysisStatus.Finalized),
                ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false, firstSave));
        assertEquals("abnormal: waits for a validator", status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "N", "25.0"), false, firstSave));
        assertEquals("no range to judge against: waits for a validator", status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "A", "negative"), false, firstSave));
    }

    @Test
    public void aLabThatValidatesEverythingStillSeesEveryResult() {
        assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), true, analysisService.get("100")));
    }

    @Test
    public void aRecordedQcFailureOrAnEarlierSaveHoldsTheResultForAValidator() {
        TestResultItem resaved = entered("104", "N", "12.0");
        resaved.setResultId("104");
        assertEquals("the result being re-saved carries a QC failure", status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(resaved, false, analysisService.get("104")));

        Analysis savedBefore = analysisService.get("100");
        savedBefore.setRevision("1");
        assertEquals("a second save is a modification", status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false, savedBefore));
    }

    @Test
    public void everyEntryPathSharesTheRule() {
        assertEquals(status(AnalysisStatus.Finalized),
                ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false));
    }

    @Test
    public void blockingSelfValidationAlsoBlocksAutomatedFinalization() {
        var configuration = org.openelisglobal.common.util.ConfigurationProperties.getInstance();
        var property = org.openelisglobal.common.util.ConfigurationProperties.Property.BLOCK_SELF_VALIDATION;
        String previous = configuration.getPropertyValue(property);
        try {
            configuration.setPropertyValue(property, "true");
            assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                    ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false, analysisService.get("100")));
        } finally {
            configuration.setPropertyValue(property, previous);
        }
    }

    @Test
    public void noReviewReleasesAValidResultAtEntryEvenWhenAbnormal() {
        underPolicy("NONE", true, () -> {
            assertEquals(status(AnalysisStatus.Finalized),
                    ResultUtil.getStatusForTestResult(entered("100", "N", "25.0"), true, analysisService.get("100")));
            TestResultItem invalid = entered("100", "N", "25.0");
            invalid.setValid(false);
            assertEquals("an entry the page marked invalid still waits", status(AnalysisStatus.TechnicalAcceptance),
                    ResultUtil.getStatusForTestResult(invalid, false, analysisService.get("100")));
        });
    }

    @Test
    public void allResultsWaitsForReviewWhateverValidateAllResultsSays() {
        underPolicy("ALL", true, () -> assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false, analysisService.get("100"))));
    }

    @Test
    public void onlyResultsThatNeedReviewAppliesTheClearanceRuleWhateverValidateAllResultsSays() {
        underPolicy("NEEDS_REVIEW", true, () -> {
            assertEquals(status(AnalysisStatus.Finalized),
                    ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), true, analysisService.get("100")));
            assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                    ResultUtil.getStatusForTestResult(entered("100", "N", "25.0"), true, analysisService.get("100")));
        });
    }

    @Test
    public void withTheFlagOffThePolicyIsIgnored() {
        underPolicy("NONE", false, () -> assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                ResultUtil.getStatusForTestResult(entered("100", "N", "25.0"), false, analysisService.get("100"))));
    }

    @Test
    public void blockedSelfValidationKeepsNoReviewResultsWaiting() {
        var configuration = org.openelisglobal.common.util.ConfigurationProperties.getInstance();
        var property = org.openelisglobal.common.util.ConfigurationProperties.Property.BLOCK_SELF_VALIDATION;
        String previous = configuration.getPropertyValue(property);
        try {
            configuration.setPropertyValue(property, "true");
            underPolicy("NONE", true, () -> assertEquals(status(AnalysisStatus.TechnicalAcceptance),
                    ResultUtil.getStatusForTestResult(entered("100", "N", "10.5"), false, analysisService.get("100"))));
        } finally {
            configuration.setPropertyValue(property, previous);
        }
    }

    private void underPolicy(String trigger, boolean flagOn, Runnable check) {
        var configuration = org.openelisglobal.common.util.ConfigurationProperties.getInstance();
        var flag = org.openelisglobal.common.util.ConfigurationProperties.Property.USE_MULTI_LEVEL_VALIDATION;
        String previousFlag = configuration.getPropertyValue(flag);
        java.util.List<java.util.Map<String, Object>> before = jdbcTemplate.queryForList(
                "SELECT review_trigger, review_levels FROM clinlims.validation_policy WHERE test_section_id IS NULL");
        int levels = "NONE".equals(trigger) ? 0 : 1;
        try {
            configuration.setPropertyValue(flag, Boolean.toString(flagOn));
            if (before.isEmpty()) {
                jdbcTemplate.update("INSERT INTO clinlims.validation_policy (id, test_section_id, review_trigger,"
                        + " review_levels, is_active) VALUES (nextval('clinlims.validation_policy_seq'), NULL, ?, ?,"
                        + " true)", trigger, levels);
            } else {
                jdbcTemplate.update("UPDATE clinlims.validation_policy SET review_trigger = ?, review_levels = ?"
                        + " WHERE test_section_id IS NULL", trigger, levels);
            }
            check.run();
        } finally {
            if (before.isEmpty()) {
                jdbcTemplate.update("DELETE FROM clinlims.validation_policy WHERE test_section_id IS NULL");
            } else {
                jdbcTemplate.update(
                        "UPDATE clinlims.validation_policy SET review_trigger = ?, review_levels = ?"
                                + " WHERE test_section_id IS NULL",
                        before.get(0).get("review_trigger"), before.get(0).get("review_levels"));
            }
            configuration.setPropertyValue(flag, previousFlag);
        }
    }

}
