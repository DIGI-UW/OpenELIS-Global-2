package org.openelisglobal.qc.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.qc.dao.QCAlertDAO;
import org.openelisglobal.qc.dao.QCResultDAO;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the full QC Alert pipeline (T100-T102).
 *
 * Tests the end-to-end flow: QCResult creation → async event → Westgard rule
 * evaluation → Violation creation → Alert broadcasting.
 *
 * Test data loaded via DBUnit from testdata/qc-alert-flow.xml: - system_user
 * id=1 (active, alert recipient) - test id=1 ("Glucose") - qc_control_lot
 * "lot-alert-001" (ACTIVE, instrument_id=1) - qc_statistics: mean=100.0, SD=5.0
 * - westgard_rule_config: 1₃ₛ (REJECTION) and 1₂ₛ (WARNING) enabled
 */
public class QCAlertServiceIntegrationTest extends BaseWebContextSensitiveTest {

    // Async event processing requires time: result commit → event listener → rule
    // evaluation → violation + alert creation (3 async hops with REQUIRES_NEW)
    private static final int ASYNC_WAIT_MS = 1500;

    @Autowired
    private QCResultService resultService;

    @Autowired
    private QCResultDAO resultDAO;

    @Autowired
    private QCRuleViolationDAO violationDAO;

    @Autowired
    private QCAlertDAO alertDAO;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/qc-alert-flow.xml");
    }

    /**
     * Value=120 → z-score=4.0 (exceeds both ±3SD and ±2SD). Expects: 1₃ₛ REJECTION
     * violation + 1₂ₛ WARNING violation, with alerts for each violation sent to the
     * active user.
     */
    @Test
    public void fullFlow_resultExceeding3SD_createsViolationsAndAlerts() throws InterruptedException {
        // Arrange & Act: value=120, mean=100, SD=5 → z-score = (120-100)/5 = 4.0
        QCResult result = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("120.0"),
                "mg/dL", LocalDateTime.now());

        assertNotNull("QC result should be created", result);
        assertEquals("Z-score should be 4.0000", 0, new BigDecimal("4.0000").compareTo(result.getZScore()));

        // Wait for async event processing
        Thread.sleep(ASYNC_WAIT_MS);

        // Verify violations in DB
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        assertEquals("Should have 2 violations (1₃ₛ and 1₂ₛ)", 2, violations.size());

        // Verify REJECTION violation (1₃ₛ)
        List<QCRuleViolation> rejections = violations.stream().filter(v -> "REJECTION".equals(v.getSeverity()))
                .collect(Collectors.toList());
        assertEquals("Should have exactly 1 REJECTION violation", 1, rejections.size());
        QCRuleViolation rejection = rejections.get(0);
        assertEquals("REJECTION rule code should be 1₃ₛ", "1₃ₛ", rejection.getRuleCode());
        assertEquals("REJECTION status should be UNRESOLVED", "UNRESOLVED", rejection.getResolutionStatus());
        assertEquals("REJECTION triggering result should match", result.getId(), rejection.getTriggeringResultId());
        assertEquals("1", rejection.getInstrumentId());
        assertEquals("1", rejection.getTestId());
        Timestamp tenSecondsAgo = Timestamp.from(Instant.now().minusSeconds(10));
        assertTrue("REJECTION violation datetime should be recent (within 10s)",
                rejection.getViolationDateTime().after(tenSecondsAgo));

        // Verify WARNING violation (1₂ₛ)
        List<QCRuleViolation> warnings = violations.stream().filter(v -> "WARNING".equals(v.getSeverity()))
                .collect(Collectors.toList());
        assertEquals("Should have exactly 1 WARNING violation", 1, warnings.size());
        QCRuleViolation warning = warnings.get(0);
        assertEquals("WARNING rule code should be 1₂ₛ", "1₂ₛ", warning.getRuleCode());
        assertEquals("WARNING status should be UNRESOLVED", "UNRESOLVED", warning.getResolutionStatus());
        assertEquals("WARNING triggering result should match", result.getId(), warning.getTriggeringResultId());

        // Verify alerts for REJECTION violation
        List<QCAlert> rejectionAlerts = alertDAO.findByViolation(rejection.getId());
        assertEquals("Should have 1 alert for REJECTION violation (1 active user)", 1, rejectionAlerts.size());
        QCAlert rejectionAlert = rejectionAlerts.get(0);
        assertEquals("REJECTION alert recipient should be user 1", Integer.valueOf(1),
                rejectionAlert.getRecipientUserId());
        assertEquals("REJECTION alert violation ID should match", rejection.getId(), rejectionAlert.getViolationId());
        assertEquals("REJECTION alert type should be QC_RULE_VIOLATION", "QC_RULE_VIOLATION",
                rejectionAlert.getAlertType());
        assertFalse("REJECTION alert should be unread", rejectionAlert.getReadStatus());
        assertTrue("REJECTION alert sent time should be recent (within 10s)",
                rejectionAlert.getSentDateTime().after(tenSecondsAgo));
        assertTrue("REJECTION alert subject should contain URGENT",
                rejectionAlert.getMessageSubject().contains("URGENT"));
        assertTrue("REJECTION alert body should contain IMMEDIATE ACTION REQUIRED",
                rejectionAlert.getMessageBody().contains("IMMEDIATE ACTION REQUIRED"));

        // Verify alerts for WARNING violation
        List<QCAlert> warningAlerts = alertDAO.findByViolation(warning.getId());
        assertEquals("Should have 1 alert for WARNING violation (1 active user)", 1, warningAlerts.size());
        QCAlert warningAlert = warningAlerts.get(0);
        assertEquals("WARNING alert recipient should be user 1", Integer.valueOf(1), warningAlert.getRecipientUserId());
        assertEquals("WARNING alert violation ID should match", warning.getId(), warningAlert.getViolationId());
        assertFalse("WARNING alert should be unread", warningAlert.getReadStatus());
        assertTrue("WARNING alert subject should contain Warning",
                warningAlert.getMessageSubject().contains("Warning"));
        assertFalse("WARNING alert subject should NOT contain URGENT",
                warningAlert.getMessageSubject().contains("URGENT"));

        // Verify result_status updated from PENDING to REJECTED (REJECTION-severity
        // violation)
        QCResult updatedResult = resultDAO.get(result.getId()).orElseThrow();
        assertEquals("Result status should be REJECTED after REJECTION-severity violation", "REJECTED",
                updatedResult.getResultStatus());
    }

    /**
     * Value=112 → z-score=2.4 (exceeds ±2SD but not ±3SD). Expects: only 1₂ₛ
     * WARNING violation with alert.
     */
    @Test
    public void fullFlow_resultExceeding2SDOnly_createsWarningViolationAndAlert() throws InterruptedException {
        // Arrange & Act: value=112, mean=100, SD=5 → z-score = (112-100)/5 = 2.4
        QCResult result = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("112.0"),
                "mg/dL", LocalDateTime.now());

        assertEquals("Z-score should be 2.4000", 0, new BigDecimal("2.4000").compareTo(result.getZScore()));

        Thread.sleep(ASYNC_WAIT_MS);

        // Verify: only WARNING violation, no REJECTION
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        assertEquals("Should have exactly 1 violation (1₂ₛ only)", 1, violations.size());

        QCRuleViolation violation = violations.get(0);
        assertEquals("Violation rule should be 1₂ₛ", "1₂ₛ", violation.getRuleCode());
        assertEquals("Violation severity should be WARNING", "WARNING", violation.getSeverity());
        assertEquals("Violation status should be UNRESOLVED", "UNRESOLVED", violation.getResolutionStatus());
        assertEquals("Violation triggering result should match", result.getId(), violation.getTriggeringResultId());

        // Verify alert
        List<QCAlert> alerts = alertDAO.findByViolation(violation.getId());
        assertEquals("Should have 1 alert for WARNING violation", 1, alerts.size());

        QCAlert alert = alerts.get(0);
        assertEquals("Alert recipient should be user 1", Integer.valueOf(1), alert.getRecipientUserId());
        assertEquals("Alert violation ID should match", violation.getId(), alert.getViolationId());
        assertTrue("Alert subject should contain Warning", alert.getMessageSubject().contains("Warning"));
        assertFalse("Alert subject should NOT contain URGENT", alert.getMessageSubject().contains("URGENT"));
        assertFalse("Alert body should NOT contain IMMEDIATE ACTION REQUIRED",
                alert.getMessageBody().contains("IMMEDIATE ACTION REQUIRED"));

        // Verify result_status is ACCEPTED (WARNING violations don't reject a result)
        QCResult updatedResult = resultDAO.get(result.getId()).orElseThrow();
        assertEquals("Result status should be ACCEPTED (warnings don't reject)", "ACCEPTED",
                updatedResult.getResultStatus());
    }

    /**
     * Proves the pipeline correctly discriminates: an abnormal result triggers
     * violations, then a normal result (z=0.4) adds NO additional violations.
     *
     * Without the abnormal precondition, asserting "0 violations" would be a false
     * positive — it could pass if the pipeline were completely broken.
     */
    @Test
    public void fullFlow_normalResult_createsNoViolationsOrAlerts() throws InterruptedException {
        // Precondition: create an abnormal result to prove the pipeline is functional
        QCResult abnormal = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("112.0"),
                "mg/dL", LocalDateTime.now());

        Thread.sleep(ASYNC_WAIT_MS);

        List<QCRuleViolation> preconditionViolations = violationDAO.findByInstrument("1");
        assertEquals("Precondition: pipeline must produce 1 WARNING violation", 1, preconditionViolations.size());
        assertEquals("Precondition: violation should reference the abnormal result", abnormal.getId(),
                preconditionViolations.get(0).getTriggeringResultId());

        // Act: create a normal result (value=102, z-score=0.4, within ±2SD)
        QCResult normal = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("102.0"),
                "mg/dL", LocalDateTime.now());

        assertEquals("Z-score should be 0.4000", 0, new BigDecimal("0.4000").compareTo(normal.getZScore()));

        Thread.sleep(ASYNC_WAIT_MS);

        // Verify: still only 1 violation (from the precondition abnormal result)
        List<QCRuleViolation> allViolations = violationDAO.findByInstrument("1");
        assertEquals("Should still have only 1 violation (the precondition one)", 1, allViolations.size());
        assertEquals("The only violation should still be from the abnormal result", abnormal.getId(),
                allViolations.get(0).getTriggeringResultId());

        // Verify: no violations reference the normal result
        List<QCRuleViolation> normalResultViolations = violationDAO.findByTriggeringResultId(normal.getId());
        assertEquals("Normal result should have triggered 0 violations", 0, normalResultViolations.size());

        // Verify result_status updated to ACCEPTED (no violations)
        QCResult updatedNormal = resultDAO.get(normal.getId()).orElseThrow();
        assertEquals("Normal result status should be ACCEPTED", "ACCEPTED", updatedNormal.getResultStatus());

        // Verify abnormal result (warning-only) is also ACCEPTED
        QCResult updatedAbnormal = resultDAO.get(abnormal.getId()).orElseThrow();
        assertEquals("Warning-only result status should be ACCEPTED", "ACCEPTED", updatedAbnormal.getResultStatus());
    }

    /**
     * Two separate QC results in rapid succession should each produce their own
     * independent violation and alert. Each violation gets a unique UUID, so they
     * are treated as independent events regardless of timing.
     */
    @Test
    public void fullFlow_twoSeparateResults_createIndependentViolationsAndAlerts() throws InterruptedException {
        // Act: Create two results both exceeding 2SD but not 3SD
        QCResult result1 = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("112.0"),
                "mg/dL", LocalDateTime.now());
        QCResult result2 = resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("113.0"),
                "mg/dL", LocalDateTime.now());

        assertNotEquals("Results should have different IDs", result1.getId(), result2.getId());

        Thread.sleep(ASYNC_WAIT_MS);

        // Verify: 2 violations (one per result), each WARNING
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        assertEquals("Should have 2 violations (one per result)", 2, violations.size());

        for (QCRuleViolation v : violations) {
            assertEquals("Each violation severity should be WARNING", "WARNING", v.getSeverity());
            assertEquals("Each violation rule should be 1₂ₛ", "1₂ₛ", v.getRuleCode());
        }

        // Verify each violation has its own triggering result
        List<String> triggeringIds = violations.stream().map(QCRuleViolation::getTriggeringResultId).sorted()
                .collect(Collectors.toList());
        assertEquals("Should have 2 distinct triggering result IDs", 2, triggeringIds.stream().distinct().count());

        // Verify alerts exist for each violation independently
        for (QCRuleViolation v : violations) {
            List<QCAlert> alerts = alertDAO.findByViolation(v.getId());
            assertEquals("Each violation should produce 1 alert", 1, alerts.size());
            assertEquals("Alert recipient should be user 1", Integer.valueOf(1), alerts.get(0).getRecipientUserId());
            assertEquals("Alert violation ID should match", v.getId(), alerts.get(0).getViolationId());
        }

        // Verify total alert count
        List<QCAlert> allAlerts = alertDAO.findByRecipient(1);
        assertEquals("Total alerts for user 1 should be 2", 2, allAlerts.size());
    }

    /**
     * Verifies the alert message formatting for REJECTION severity. Subject should
     * contain "URGENT" and rule code. Body should contain violation details and
     * action required text.
     */
    @Test
    public void fullFlow_rejectionAlert_hasCorrectMessageFormatting() throws InterruptedException {
        // Act: value=120 → z=4.0 → triggers 1₃ₛ REJECTION
        resultService.createQCResult("1", "1", "lot-alert-001", "NORMAL", new BigDecimal("120.0"), "mg/dL",
                LocalDateTime.now());

        Thread.sleep(ASYNC_WAIT_MS);

        // Find the REJECTION violation
        List<QCRuleViolation> violations = violationDAO.findByInstrument("1");
        QCRuleViolation rejection = violations.stream().filter(v -> "REJECTION".equals(v.getSeverity())).findFirst()
                .orElseThrow(() -> new AssertionError("REJECTION violation not found"));

        List<QCAlert> alerts = alertDAO.findByViolation(rejection.getId());
        assertEquals("Should have 1 alert for REJECTION", 1, alerts.size());

        QCAlert alert = alerts.get(0);

        // Verify subject format: "URGENT: QC Rule 1₃ₛ Violation - Immediate Action
        // Required"
        assertEquals("REJECTION alert subject should match expected format",
                "URGENT: QC Rule 1₃ₛ Violation - Immediate Action Required", alert.getMessageSubject());

        // Verify body contains key elements with specific format strings
        // (not just "1" which would trivially match rule codes, timestamps, etc.)
        String body = alert.getMessageBody();
        assertTrue("Body should contain rule line", body.contains("Rule: 1₃ₛ"));
        assertTrue("Body should contain severity line", body.contains("Severity: REJECTION"));
        assertTrue("Body should contain instrument ID line", body.contains("Instrument ID: 1"));
        assertTrue("Body should contain test ID line", body.contains("Test ID: 1"));
        assertTrue("Body should contain action required text", body.contains("IMMEDIATE ACTION REQUIRED"));
        assertTrue("Body should contain corrective action instruction",
                body.contains("take corrective action before continuing patient testing"));
    }
}
