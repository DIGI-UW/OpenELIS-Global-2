package org.openelisglobal.qc.service;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.QcEvaluation;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * End-to-end integration test for QC evaluation through the result save
 * pipeline (OGC-554).
 *
 * Narrates the full workflow: lab tech enters 4 results (parent + blank +
 * duplicate + control) in one batch save → LogbookPersistServiceImpl persists
 * each result → QcEvaluationService automatically evaluates QC samples →
 * evaluation outcome is persisted on the Result entity.
 *
 * Fixture: testdata/qc-e2e-save-flow.xml has sample items, QC profiles,
 * analyses, and thresholds — but NO results. The test creates results through
 * the actual save flow.
 */
public class QcEvaluationE2ETest extends BaseWebContextSensitiveTest {

    @Autowired
    private LogbookResultsPersistService logbookPersistService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private SampleService sampleService;

    private Sample sample;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-e2e-save-flow.xml");
        sample = sampleService.get("1");
    }

    /**
     * Simulates a lab tech entering results for a full environmental QC batch:
     * parent sample (10.5 mg/L), blank (0.3), duplicate (11.5), control (48.0).
     *
     * All 4 results are submitted in a single persistDataSet() call — exactly as
     * the LogbookResultsRestController does. After save, QC evaluations should be
     * automatically populated on the 3 QC results.
     */
    @Test
    public void fullBatchSave_evaluatesAllQcSamplesAutomatically() {
        // ── Arrange: build 4 results as a lab tech would enter them ──

        Analysis parentAnalysis = analysisService.get("1");
        Analysis blankAnalysis = analysisService.get("2");
        Analysis duplicateAnalysis = analysisService.get("3");
        Analysis controlAnalysis = analysisService.get("4");

        Result parentResult = buildResult("10.5", parentAnalysis);
        Result blankResult = buildResult("0.3", blankAnalysis);
        Result duplicateResult = buildResult("11.5", duplicateAnalysis);
        Result controlResult = buildResult("48.0", controlAnalysis);

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("1");
        // Order matters: parent first, so its result exists when duplicate is evaluated
        dataSet.getNewResults().add(toResultSet(parentResult));
        dataSet.getNewResults().add(toResultSet(blankResult));
        dataSet.getNewResults().add(toResultSet(duplicateResult));
        dataSet.getNewResults().add(toResultSet(controlResult));

        // ── Act: save through the real persist service ──

        logbookPersistService.persistDataSet(dataSet, new ArrayList<>(), "1");

        // ── Assert: reload results from DB and verify evaluations ──

        // Parent (regular sample) — no QC evaluation
        Result savedParent = resultService.get(parentResult.getId());
        assertNull("Parent (regular sample) should have no QC evaluation", savedParent.getQcEvaluation());

        // Blank — result 0.3 ≤ threshold 0.5 → PASS
        Result savedBlank = resultService.get(blankResult.getId());
        assertEquals(QcEvaluation.PASS, savedBlank.getQcEvaluation());
        assertTrue("Blank detail should show result and threshold",
                savedBlank.getQcEvaluationDetail().contains("Result = 0.3"));
        assertTrue(savedBlank.getQcEvaluationDetail().contains("threshold 0.5"));

        // Duplicate — RPD = |10.5-11.5| / mean(10.5,11.5) × 100 = 9.1% ≤ 20% → PASS
        Result savedDuplicate = resultService.get(duplicateResult.getId());
        assertEquals(QcEvaluation.PASS, savedDuplicate.getQcEvaluation());
        assertTrue("Duplicate detail should show RPD", savedDuplicate.getQcEvaluationDetail().contains("RPD ="));
        assertTrue(savedDuplicate.getQcEvaluationDetail().contains("threshold 20"));

        // Control — recovery = 48/50 × 100 = 96% within [80%, 120%] → PASS
        Result savedControl = resultService.get(controlResult.getId());
        assertEquals(QcEvaluation.PASS, savedControl.getQcEvaluation());
        assertTrue("Control detail should show recovery", savedControl.getQcEvaluationDetail().contains("Recovery ="));
        assertTrue(savedControl.getQcEvaluationDetail().contains("window 80"));
    }

    /**
     * Same batch, but the blank is contaminated (1.2 > threshold 0.5) and the
     * duplicate has poor precision (RPD 35.3% > 20%). Verifies FAIL evaluations
     * propagate correctly.
     */
    @Test
    public void batchWithFailures_evaluationsReflectFailures() {
        Analysis parentAnalysis = analysisService.get("1");
        Analysis blankAnalysis = analysisService.get("2");
        Analysis duplicateAnalysis = analysisService.get("3");
        Analysis controlAnalysis = analysisService.get("4");

        Result parentResult = buildResult("10.5", parentAnalysis);
        Result blankResult = buildResult("1.2", blankAnalysis); // contaminated
        Result duplicateResult = buildResult("15.0", duplicateAnalysis); // poor precision
        Result controlResult = buildResult("48.0", controlAnalysis); // still within window

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("1");
        dataSet.getNewResults().add(toResultSet(parentResult));
        dataSet.getNewResults().add(toResultSet(blankResult));
        dataSet.getNewResults().add(toResultSet(duplicateResult));
        dataSet.getNewResults().add(toResultSet(controlResult));

        logbookPersistService.persistDataSet(dataSet, new ArrayList<>(), "1");

        // Blank — 1.2 > 0.5 → FAIL
        Result savedBlank = resultService.get(blankResult.getId());
        assertEquals(QcEvaluation.FAIL, savedBlank.getQcEvaluation());
        assertTrue(savedBlank.getQcEvaluationDetail().contains("1.2"));

        // Duplicate — RPD = |10.5-15.0| / mean(10.5,15.0) × 100 = 35.3% > 20% → FAIL
        Result savedDuplicate = resultService.get(duplicateResult.getId());
        assertEquals(QcEvaluation.FAIL, savedDuplicate.getQcEvaluation());
        assertTrue(savedDuplicate.getQcEvaluationDetail().contains("RPD ="));

        // Control — still PASS
        Result savedControl = resultService.get(controlResult.getId());
        assertEquals(QcEvaluation.PASS, savedControl.getQcEvaluation());
    }

    /**
     * Simulates a lab tech correcting a result: first save with a failing value,
     * then update with a passing value. Verifies evaluation is re-run on update.
     */
    @Test
    public void resultUpdate_reEvaluatesQc() {
        // First: save parent + blank with failing value
        Analysis parentAnalysis = analysisService.get("1");
        Analysis blankAnalysis = analysisService.get("2");

        Result parentResult = buildResult("10.5", parentAnalysis);
        Result blankResult = buildResult("0.8", blankAnalysis); // fails: 0.8 > 0.5

        ResultsUpdateDataSet insertDataSet = new ResultsUpdateDataSet("1");
        insertDataSet.getNewResults().add(toResultSet(parentResult));
        insertDataSet.getNewResults().add(toResultSet(blankResult));

        logbookPersistService.persistDataSet(insertDataSet, new ArrayList<>(), "1");

        Result savedBlank = resultService.get(blankResult.getId());
        assertEquals("Initial save should FAIL", QcEvaluation.FAIL, savedBlank.getQcEvaluation());

        // Second: lab tech corrects the blank value
        savedBlank.setValue("0.2"); // now passes: 0.2 ≤ 0.5
        savedBlank.setSysUserId("1");

        ResultsUpdateDataSet updateDataSet = new ResultsUpdateDataSet("1");
        updateDataSet.getModifiedResults().add(toResultSet(savedBlank));

        logbookPersistService.persistDataSet(updateDataSet, new ArrayList<>(), "1");

        Result updatedBlank = resultService.get(savedBlank.getId());
        assertEquals("After correction should PASS", QcEvaluation.PASS, updatedBlank.getQcEvaluation());
        assertTrue(updatedBlank.getQcEvaluationDetail().contains("0.2"));
    }

    /**
     * Verifies that evaluation uses the actual configured thresholds, not hardcoded
     * defaults. The fixture has rpd=20% and recovery=20%. This test changes them to
     * rpd=40% and recovery=5%, then saves results that would PASS at 20% but FAIL
     * at 5% (control), and results that would FAIL at 20% but PASS at 40%
     * (duplicate).
     */
    @Test
    public void customThresholds_evaluationUsesConfiguredValues() {
        // Change thresholds via SQL: loosen RPD to 40%, tighten recovery to 5%
        jdbcTemplate.update("UPDATE clinlims.test_qc_threshold SET rpd_threshold = 40, "
                + "recovery_window_pct = 5 WHERE id = 'tqc-lead-01'");

        Analysis parentAnalysis = analysisService.get("1");
        Analysis duplicateAnalysis = analysisService.get("3");
        Analysis controlAnalysis = analysisService.get("4");

        // RPD = |10.5-15.0| / mean(10.5,15.0) × 100 = 35.3%
        // At default 20% → FAIL, but at custom 40% → PASS
        Result parentResult = buildResult("10.5", parentAnalysis);
        Result duplicateResult = buildResult("15.0", duplicateAnalysis);

        // Recovery = 48/50 × 100 = 96%
        // At default ±20% (80-120%) → PASS, but at custom ±5% (95-105%) → PASS
        // Recovery = 44/50 × 100 = 88%
        // At default ±20% (80-120%) → PASS, but at custom ±5% (95-105%) → FAIL
        Result controlResult = buildResult("44.0", controlAnalysis);

        ResultsUpdateDataSet dataSet = new ResultsUpdateDataSet("1");
        dataSet.getNewResults().add(toResultSet(parentResult));
        dataSet.getNewResults().add(toResultSet(duplicateResult));
        dataSet.getNewResults().add(toResultSet(controlResult));

        logbookPersistService.persistDataSet(dataSet, new ArrayList<>(), "1");

        // Duplicate: RPD 35.3% ≤ 40% → PASS (would be FAIL with default 20%)
        Result savedDuplicate = resultService.get(duplicateResult.getId());
        assertEquals(QcEvaluation.PASS, savedDuplicate.getQcEvaluation());
        assertTrue("Detail should show custom threshold 40",
                savedDuplicate.getQcEvaluationDetail().contains("threshold 40"));

        // Control: recovery 88% outside [95%, 105%] → FAIL (would be PASS with default
        // ±20%)
        Result savedControl = resultService.get(controlResult.getId());
        assertEquals(QcEvaluation.FAIL, savedControl.getQcEvaluation());
        assertTrue("Detail should show custom window 95% - 105%",
                savedControl.getQcEvaluationDetail().contains("window 95"));
    }

    // ── helpers ──

    private Result buildResult(String value, Analysis analysis) {
        Result result = new Result();
        result.setAnalysis(analysis);
        result.setValue(value);
        result.setResultType("N");
        result.setIsReportable("Y");
        result.setSysUserId("1");
        return result;
    }

    private ResultSet toResultSet(Result result) {
        return new ResultSet(result, null, null, null, sample, new HashMap<>(), false);
    }
}
