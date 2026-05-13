package org.openelisglobal.qc.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.result.service.QcEvaluationService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.QcEvaluation;
import org.openelisglobal.result.valueholder.Result;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for QcEvaluationService (OGC-554 S-08).
 *
 * Test data in testdata/qc-evaluation.xml: - test id=1: Lead (Pb), thresholds:
 * blank=0.5, rpd=20%, recovery=20% - test id=2: pH, no QC thresholds configured
 * - sample_item id=1: regular parent (result=10.5) - sample_item id=2: BLANK
 * (result=0.3, threshold=0.5 → PASS) - sample_item id=3: DUPLICATE of parent
 * (result=11.5, RPD=9.1% → PASS) - sample_item id=4: CONTROL (result=48.0,
 * expected=50.0, recovery=96% → PASS)
 */
public class QcEvaluationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private QcEvaluationService qcEvaluationService;

    @Autowired
    private ResultService resultService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-evaluation.xml");
    }

    // ==================== BLANK tests ====================

    @Test
    public void evaluateQc_blankPass_resultBelowThreshold() {
        Result result = resultService.get("2");
        assertNull("QC evaluation should not be set before evaluation", result.getQcEvaluation());

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.PASS, result.getQcEvaluation());
        assertTrue("Detail should contain result and threshold",
                result.getQcEvaluationDetail().contains("Result = 0.3"));
        assertTrue("Detail should contain threshold", result.getQcEvaluationDetail().contains("threshold 0.5"));
    }

    @Test
    public void evaluateQc_blankFail_resultAboveThreshold() {
        Result result = resultService.get("2");
        result.setValue("0.8");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.FAIL, result.getQcEvaluation());
        assertTrue("Detail should mention result value", result.getQcEvaluationDetail().contains("0.8"));
    }

    @Test
    public void evaluateQc_blankBoundary_resultEqualsThreshold() {
        Result result = resultService.get("2");
        result.setValue("0.5");

        qcEvaluationService.evaluateQc(result);

        assertEquals("Result exactly at threshold should PASS", QcEvaluation.PASS, result.getQcEvaluation());
    }

    // ==================== DUPLICATE tests ====================

    @Test
    public void evaluateQc_duplicatePass_rpdBelowThreshold() {
        // Parent=10.5, Dup=11.5 → RPD = |10.5-11.5| / mean(10.5,11.5) * 100
        // = 1.0 / 11.0 * 100 = 9.1% (below 20%)
        Result result = resultService.get("3");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.PASS, result.getQcEvaluation());
        assertTrue("Detail should contain RPD", result.getQcEvaluationDetail().contains("RPD ="));
        assertTrue("Detail should contain threshold", result.getQcEvaluationDetail().contains("threshold 20"));
    }

    @Test
    public void evaluateQc_duplicateFail_rpdAboveThreshold() {
        // Change dup value to 15.0: RPD = |10.5-15.0| / mean(10.5,15.0) * 100
        // = 4.5 / 12.75 * 100 = 35.3% (above 20%)
        Result result = resultService.get("3");
        result.setValue("15.0");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.FAIL, result.getQcEvaluation());
        assertTrue("Detail should contain RPD percentage", result.getQcEvaluationDetail().contains("RPD ="));
    }

    @Test
    public void evaluateQc_duplicateBothZero_passWithZeroRpd() {
        // Both parent and dup are 0 → RPD = 0 → PASS
        Result parentResult = resultService.get("1");
        parentResult.setValue("0");
        resultService.update(parentResult);

        Result result = resultService.get("3");
        result.setValue("0");

        qcEvaluationService.evaluateQc(result);

        assertEquals("Both zero should PASS", QcEvaluation.PASS, result.getQcEvaluation());
    }

    // ==================== CONTROL tests ====================

    @Test
    public void evaluateQc_controlPass_recoveryWithinWindow() {
        // Result=48.0, Expected=50.0 → recovery = 48/50*100 = 96%
        // Window: 80% - 120% → PASS
        Result result = resultService.get("4");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.PASS, result.getQcEvaluation());
        assertTrue("Detail should contain recovery", result.getQcEvaluationDetail().contains("Recovery ="));
        assertTrue("Detail should contain window", result.getQcEvaluationDetail().contains("window"));
    }

    @Test
    public void evaluateQc_controlFail_recoveryOutsideWindow() {
        // Set result to 35.0 → recovery = 35/50*100 = 70% (below 80%)
        Result result = resultService.get("4");
        result.setValue("35.0");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.FAIL, result.getQcEvaluation());
    }

    @Test
    public void evaluateQc_controlBoundaryLow_recoveryAtLowerBound() {
        // recovery = 80% → result = 50 * 0.80 = 40.0
        Result result = resultService.get("4");
        result.setValue("40.0");

        qcEvaluationService.evaluateQc(result);

        assertEquals("Recovery at lower bound should PASS", QcEvaluation.PASS, result.getQcEvaluation());
    }

    // ==================== N_A / edge case tests ====================

    @Test
    public void evaluateQc_nonNumericResult_returnsNA() {
        Result result = resultService.get("2");
        result.setValue("not-a-number");

        qcEvaluationService.evaluateQc(result);

        assertEquals(QcEvaluation.N_A, result.getQcEvaluation());
        assertEquals("Non-numeric result", result.getQcEvaluationDetail());
    }

    @Test
    public void evaluateQc_regularSample_noEvaluation() {
        // Result id=1 is from the parent sample item (no QC profile)
        Result result = resultService.get("1");

        qcEvaluationService.evaluateQc(result);

        assertNull("Regular sample should have no QC evaluation", result.getQcEvaluation());
    }

    @Test
    public void evaluateQc_nullResult_noException() {
        qcEvaluationService.evaluateQc(null);
        // Should not throw
    }
}
