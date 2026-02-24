package org.openelisglobal.qaevent.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.qaevent.valueholder.DeltaCheckAlert;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;

public class DeltaCheckEvaluationServiceImplTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DeltaCheckEvaluationService evaluationService;

    @Autowired
    private DeltaCheckAlertService alertService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/delta-check-alert.xml");
    }

    // --- calculatePercentageChange tests ---

    @Test
    public void calculatePercentageChange_normalValues_returnsCorrectPercent() {
        BigDecimal current = new BigDecimal("120");
        BigDecimal previous = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    public void calculatePercentageChange_decrease_returnsAbsolutePercent() {
        BigDecimal current = new BigDecimal("80");
        BigDecimal previous = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    public void calculatePercentageChange_zeroPrevious_returnsZero() {
        BigDecimal current = new BigDecimal("100");
        BigDecimal previous = BigDecimal.ZERO;

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void calculatePercentageChange_nullPrevious_returnsZero() {
        BigDecimal current = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(current, null);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void calculatePercentageChange_nullCurrent_returnsZero() {
        BigDecimal previous = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(null, previous);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void calculatePercentageChange_largeChange_returnsCorrectPercent() {
        BigDecimal current = new BigDecimal("300");
        BigDecimal previous = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    public void calculatePercentageChange_smallDecimalValues_returnsCorrectPercent() {
        BigDecimal current = new BigDecimal("1.5");
        BigDecimal previous = new BigDecimal("1.0");

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    public void calculatePercentageChange_identicalValues_returnsZero() {
        BigDecimal value = new BigDecimal("100");

        BigDecimal result = evaluationService.calculatePercentageChange(value, value);

        assertEquals(new BigDecimal("0.00"), result);
    }

    @Test
    public void calculatePercentageChange_negativePrevious_returnsCorrectPercent() {
        BigDecimal current = new BigDecimal("-80");
        BigDecimal previous = new BigDecimal("-100");

        BigDecimal result = evaluationService.calculatePercentageChange(current, previous);

        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    public void calculatePercentageChange_bothNull_returnsZero() {
        BigDecimal result = evaluationService.calculatePercentageChange(null, null);

        assertEquals(BigDecimal.ZERO, result);
    }

    // --- hasNumericValue tests ---

    @Test
    public void hasNumericValue_integerValue_returnsTrue() {
        Result result = createResultWithValue("42");
        assertTrue(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_decimalValue_returnsTrue() {
        Result result = createResultWithValue("3.14");
        assertTrue(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_scientificNotation_returnsTrue() {
        Result result = createResultWithValue("1.5e3");
        assertTrue(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_lessThanPrefix_returnsTrue() {
        Result result = createResultWithValue("<10");
        assertTrue(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_greaterThanPrefix_returnsTrue() {
        Result result = createResultWithValue(">1000");
        assertTrue(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_textValue_returnsFalse() {
        Result result = createResultWithValue("Positive");
        assertFalse(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_emptyValue_returnsFalse() {
        Result result = createResultWithValue("");
        assertFalse(evaluationService.hasNumericValue(result));
    }

    @Test
    public void hasNumericValue_nullResult_returnsFalse() {
        assertFalse(evaluationService.hasNumericValue(null));
    }

    @Test
    public void hasNumericValue_nullValue_returnsFalse() {
        Result result = new Result();
        result.setValue(null);
        assertFalse(evaluationService.hasNumericValue(result));
    }

    // --- extractNumericValue tests ---

    @Test
    public void extractNumericValue_integerValue_returnsCorrectBigDecimal() {
        Result result = createResultWithValue("42");
        assertEquals(new BigDecimal("42"), evaluationService.extractNumericValue(result));
    }

    @Test
    public void extractNumericValue_lessThanPrefix_stripsPrefix() {
        Result result = createResultWithValue("<10");
        assertEquals(new BigDecimal("10"), evaluationService.extractNumericValue(result));
    }

    @Test
    public void extractNumericValue_nonNumeric_returnsNull() {
        Result result = createResultWithValue("Positive");
        assertNull(evaluationService.extractNumericValue(result));
    }

    // --- shouldPerformDeltaCheck tests ---

    @Test
    public void shouldPerformDeltaCheck_nullResult_returnsFalse() {
        assertFalse(evaluationService.shouldPerformDeltaCheck(null));
    }

    @Test
    public void shouldPerformDeltaCheck_nonNumericResult_returnsFalse() {
        Result result = createResultWithValue("Positive");
        assertFalse(evaluationService.shouldPerformDeltaCheck(result));
    }

    @Test
    public void shouldPerformDeltaCheck_noAnalysis_returnsFalse() {
        Result result = createResultWithValue("42");
        result.setAnalysis(null);
        assertFalse(evaluationService.shouldPerformDeltaCheck(result));
    }

    @Test
    public void shouldPerformDeltaCheck_globallyEnabled_returnsTrue() {
        ConfigurationProperties.getInstance().setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED,
                "true");
        try {
            Result result = createResultWithAnalysis("42", "1", "3");
            assertTrue(evaluationService.shouldPerformDeltaCheck(result));
        } finally {
            ConfigurationProperties.getInstance()
                    .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED, "false");
        }
    }

    @Test
    public void shouldPerformDeltaCheck_globallyDisabled_returnsFalse() {
        // Default value is "false" — any numeric result with analysis should return
        // false
        Result result = createResultWithAnalysis("42", "1", "3");
        assertFalse(evaluationService.shouldPerformDeltaCheck(result));
    }

    // --- evaluateResultForDeltaCheck tests ---

    @Test
    public void evaluateResultForDeltaCheck_nullResult_returnsNull() {
        assertNull(evaluationService.evaluateResultForDeltaCheck(null));
    }

    @Test
    public void evaluateResultForDeltaCheck_nonNumericResult_returnsNull() {
        Result result = createResultWithValue("Positive");
        assertNull(evaluationService.evaluateResultForDeltaCheck(result));
    }

    // --- absolute threshold tests ---

    @Test
    public void evaluateResultForDeltaCheck_absoluteThresholdExceeded_createsAlertEvenWhenPercentBelowThreshold() {
        // current=110, previous=100: 10% change (below default 30% threshold)
        // absolute change=10, which exceeds absolute threshold of 5
        ConfigurationProperties.getInstance().setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED,
                "true");
        ConfigurationProperties.getInstance()
                .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ABSOLUTE_THRESHOLD, "5");
        try {
            Result currentResult = buildCurrentResultStub("101", "1", "3");
            currentResult.setValue("110.0"); // 10% above previous value of 100

            DeltaCheckAlert alert = evaluationService.evaluateResultForDeltaCheck(currentResult);

            assertNotNull("Alert should be created when absolute threshold is exceeded", alert);
        } finally {
            ConfigurationProperties.getInstance()
                    .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED, "false");
            ConfigurationProperties.getInstance()
                    .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ABSOLUTE_THRESHOLD, "");
        }
    }

    @Test
    public void evaluateResultForDeltaCheck_absoluteThresholdNotConfigured_usesPercentOnly() {
        // absolute threshold empty → disabled; 10% change below 30% percent threshold
        ConfigurationProperties.getInstance().setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED,
                "true");
        ConfigurationProperties.getInstance()
                .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ABSOLUTE_THRESHOLD, "");
        try {
            Result currentResult = buildCurrentResultStub("101", "1", "3");
            currentResult.setValue("88.0"); // ~10% drop from previous 80.0

            DeltaCheckAlert alert = evaluationService.evaluateResultForDeltaCheck(currentResult);

            assertNull("No alert should be created when only absolute threshold is disabled and percent not exceeded",
                    alert);
        } finally {
            ConfigurationProperties.getInstance()
                    .setPropertyValue(ConfigurationProperties.Property.NCE_DELTA_CHECK_ENABLED, "false");
        }
    }

    // --- reEvaluateAlert tests ---

    @Test
    public void reEvaluateAlert_recalculatesPercentageChange() {
        DeltaCheckAlert result = evaluationService.reEvaluateAlert(1);

        assertEquals(new BigDecimal("25.00"), result.getChangePercent());

        // Verify persistence — reload from DB
        DeltaCheckAlert reloaded = alertService.get(1);
        assertEquals(new BigDecimal("25.00"), reloaded.getChangePercent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reEvaluateAlert_notFound_throwsException() {
        evaluationService.reEvaluateAlert(999);
    }

    // --- findPreviousResultForComparison integration tests ---

    @Test
    public void findPreviousResultForComparison_returnsLastFinalizedResultFromDifferentSample() {
        // Dataset: patient 100 linked to sample 100 (previous, finalized analysis 100,
        // result 200 value="80.0") and sample 101 (current visit).
        // Stub the "current" result on sample 101, test 1, analyte 3.
        Result currentResult = buildCurrentResultStub("101", "1", "3");

        Result previous = evaluationService.findPreviousResultForComparison(currentResult);

        assertNotNull("Should find the previous finalized result", previous);
        assertEquals("200", previous.getId());
        assertEquals("80.0", previous.getValue());
    }

    @Test
    public void findPreviousResultForComparison_noPatientForSample_returnsNull() {
        // Sample 999 has no sample_human mapping — patient lookup returns null.
        Result currentResult = buildCurrentResultStub("999", "1", "3");

        Result previous = evaluationService.findPreviousResultForComparison(currentResult);

        assertNull("No patient means no previous result", previous);
    }

    @Test
    public void findPreviousResultForComparison_currentSampleExcludedFromResults_returnsNull() {
        // Sample 100 IS the sample with the only finalized result (result 200).
        // When sample 100 is the "current" sample it is excluded from the lookup,
        // and sample 101 has no finalized analysis — so no previous result is found.
        Result currentResult = buildCurrentResultStub("100", "1", "3");

        Result previous = evaluationService.findPreviousResultForComparison(currentResult);

        assertNull("Results on the current sample must be excluded", previous);
    }

    @Test
    public void findPreviousResultsForPatient_returnsAllFinalizedResultsForPatient() {
        Patient patient = new Patient();
        patient.setId("100");

        List<Result> results = evaluationService.findPreviousResultsForPatient(patient, 1, 3, 10);

        assertEquals(1, results.size());
        assertEquals("200", results.get(0).getId());
        assertEquals("80.0", results.get(0).getValue());
    }

    @Test
    public void findPreviousResultsForPatient_nullPatient_returnsEmpty() {
        List<Result> results = evaluationService.findPreviousResultsForPatient(null, 1, 3, 10);
        assertTrue(results.isEmpty());
    }

    @Test
    public void findPreviousResultForComparison_nullResult_returnsNull() {
        assertNull(evaluationService.findPreviousResultForComparison(null));
    }

    @Test
    public void findPreviousResultForComparison_nullAnalysis_returnsNull() {
        Result result = new Result();
        result.setValue("42.0");
        result.setAnalysis(null);
        assertNull(evaluationService.findPreviousResultForComparison(result));
    }

    // --- Helper methods ---

    private Result createResultWithValue(String value) {
        Result result = new Result();
        result.setValue(value);
        return result;
    }

    private Result createResultWithAnalysis(String value, String testId, String analyteId) {
        Result result = createResultWithValue(value);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);

        Analyte analyte = new Analyte();
        analyte.setId(analyteId);

        Analysis analysis = new Analysis();
        analysis.setTest(test);

        result.setAnalysis(analysis);
        result.setAnalyte(analyte);

        return result;
    }

    /**
     * Builds an in-memory Result stub whose analysis points to the given sampleId,
     * testId, and analyteId. The stub is NOT persisted — the service resolves the
     * patient via sampleHumanService and then queries the DB for previous results.
     */
    private Result buildCurrentResultStub(String sampleId, String testId, String analyteId) {
        Sample sample = new Sample();
        sample.setId(sampleId);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);

        Analysis analysis = new Analysis();
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);

        Analyte analyte = new Analyte();
        analyte.setId(analyteId);

        Result result = new Result();
        result.setValue("100.0");
        result.setAnalysis(analysis);
        result.setAnalyte(analyte);

        return result;
    }
}
