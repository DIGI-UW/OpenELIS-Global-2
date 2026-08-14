package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.qc.form.BenchQCCaptureForm;
import org.openelisglobal.qc.valueholder.QCQualitativeOutcome;
import org.openelisglobal.qc.valueholder.QCSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-1147 FR-C3 — the hold-or-warn policy, which is the difference between a
 * warning a technician can read and a control that actually stops a result
 * being released.
 *
 * <p>
 * Reuses {@code bench-qc-fail-signal.xml}: a failing manual control in lab unit
 * 701 holds analyses 9101 and 9102, while 9103 predates it and 9104 is in
 * another lab unit.
 */
public class QcHoldServiceTest extends BaseWebContextSensitiveTest {

    private static final List<String> CANDIDATES = List.of("9101", "9102", "9103", "9104");
    private static final String TEST_ID = "6601";
    private static final String LAB_UNIT = "701";
    private static final int TECHNICIAN = 7701;

    @Autowired
    private QcHoldService qcHoldService;
    @Autowired
    private QCResultService qcResultService;

    private String originalFlag;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/bench-qc-fail-signal.xml");
        originalFlag = ConfigurationProperties.getInstance().getPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION);
    }

    @After
    public void restoreFlag() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION,
                originalFlag == null ? "false" : originalFlag);
    }

    @Test
    public void heldAnalysesAreReportedRegardlessOfThePolicy() {
        recordFailingControl();

        // A lab that only warns still has to see which results are affected, so the
        // annotation must not depend on the blocking flag.
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "false");
        assertEquals(Set.of("9101", "9102"), qcHoldService.heldAnalysisIds(CANDIDATES));

        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "true");
        assertEquals(Set.of("9101", "9102"), qcHoldService.heldAnalysisIds(CANDIDATES));
    }

    @Test
    public void warnOnlyLabBlocksNothing() {
        recordFailingControl();
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "false");

        assertFalse(qcHoldService.blocksRelease());
        assertTrue("a warn-only lab must still be able to release",
                qcHoldService.analysisIdsBlockedFromRelease(CANDIDATES).isEmpty());
    }

    @Test
    public void blockingLabWithholdsExactlyTheHeldResults() {
        recordFailingControl();
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "true");

        assertTrue(qcHoldService.blocksRelease());
        // Only the covered results are withheld — a QC failure in one lab unit must not
        // freeze the whole validation queue.
        assertEquals(Set.of("9101", "9102"), qcHoldService.analysisIdsBlockedFromRelease(CANDIDATES));
    }

    @Test
    public void nothingIsBlockedWhenNoControlHasFailed() {
        ConfigurationProperties.getInstance().setPropertyValue(Property.QC_FAIL_BLOCKS_VALIDATION, "true");

        assertTrue(qcHoldService.heldAnalysisIds(CANDIDATES).isEmpty());
        assertTrue(qcHoldService.analysisIdsBlockedFromRelease(CANDIDATES).isEmpty());
    }

    @Test
    public void nonNumericAndEmptyInputAreHandledWithoutQuerying() {
        assertTrue(qcHoldService.heldAnalysisIds(List.of()).isEmpty());
        assertTrue(qcHoldService.heldAnalysisIds(List.of("not-a-number")).isEmpty());
        assertTrue(qcHoldService.analysisIdsBlockedFromRelease(List.of()).isEmpty());
    }

    /** A manual control that fails at 12:00, after an in-control one at 11:30. */
    private void recordFailingControl() {
        qcResultService.createBenchQCResult(
                capture(QCQualitativeOutcome.PASS, new BigDecimal("100.00000"), "2025-06-10 11:30:00"), TECHNICIAN);
        qcResultService.createBenchQCResult(
                capture(QCQualitativeOutcome.FAIL, new BigDecimal("140.00000"), "2025-06-10 12:00:00"), TECHNICIAN);
    }

    private BenchQCCaptureForm capture(QCQualitativeOutcome outcome, BigDecimal value, String runAt) {
        BenchQCCaptureForm form = new BenchQCCaptureForm();
        form.setSource(QCSource.MANUAL);
        form.setTestId(TEST_ID);
        form.setTestSectionId(LAB_UNIT);
        form.setControlLotId("bench-lot-a");
        form.setQualitativeOutcome(outcome);
        form.setResultValue(value);
        form.setExpectedValue(new BigDecimal("100.00000"));
        form.setUncertainty(new BigDecimal("5.00000"));
        form.setRunDateTime(Timestamp.valueOf(runAt).toLocalDateTime());
        return form;
    }
}
