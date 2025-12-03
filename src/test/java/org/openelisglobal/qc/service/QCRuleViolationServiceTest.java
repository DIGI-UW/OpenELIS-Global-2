package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCRuleViolationDAO;
import org.openelisglobal.qc.service.evaluator.RuleEvaluationResult;
import org.openelisglobal.qc.valueholder.QCResult;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Unit tests for QCRuleViolationService (T101)
 *
 * Tests violation creation, resolution, and alert triggering.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCRuleViolationServiceTest {

    @Mock
    private QCRuleViolationDAO violationDAO;

    @Mock
    private QCAlertService alertService;

    @InjectMocks
    private QCRuleViolationServiceImpl violationService;

    private QCResult testQCResult;
    private RuleEvaluationResult testEvalResult;

    @Before
    public void setUp() {
        testQCResult = new QCResult();
        testQCResult.setId("R1");
        testQCResult.setControlLotId("LOT1");
        testQCResult.setTestId(100);
        testQCResult.setInstrumentId(200);
        testQCResult.setResultValue(new BigDecimal("115.00"));
        testQCResult.setRunDateTime(new Timestamp(System.currentTimeMillis()));

        testEvalResult = RuleEvaluationResult.violation("1₃ₛ", "REJECTION", Arrays.asList("R1"),
                "Result exceeds 3SD (z-score: 3.0)");
    }

    // ===================== createViolation tests =====================

    @Test
    public void testCreateViolation_ShouldCreateAndPersistViolation() {
        QCRuleViolation violation = violationService.createViolation(testEvalResult, testQCResult);

        assertNotNull("Violation should be created", violation);
        assertNotNull("Violation ID should be set", violation.getId());
        assertEquals("R1", violation.getTriggeringResultId());
        assertEquals("1₃ₛ", violation.getRuleCode());
        assertEquals("REJECTION", violation.getSeverity());
        assertEquals(Integer.valueOf(100), violation.getTestId());
        assertEquals(Integer.valueOf(200), violation.getInstrumentId());
        assertEquals("UNRESOLVED", violation.getResolutionStatus());
        assertNotNull(violation.getViolationDateTime());
        assertTrue(violation.getResolutionNotes().contains("Result exceeds 3SD"));

        verify(violationDAO).insert(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_ShouldTriggerAlert() {
        violationService.createViolation(testEvalResult, testQCResult);

        verify(alertService).createAlertForViolation(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_NullEvalResult_ShouldReturnNull() {
        QCRuleViolation violation = violationService.createViolation(null, testQCResult);

        assertNull("Should return null for null evalResult", violation);
        verify(violationDAO, never()).insert(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_NullQCResult_ShouldReturnNull() {
        QCRuleViolation violation = violationService.createViolation(testEvalResult, null);

        assertNull("Should return null for null qcResult", violation);
        verify(violationDAO, never()).insert(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_NotViolated_ShouldReturnNull() {
        RuleEvaluationResult noViolation = RuleEvaluationResult.noViolation("1₃ₛ");

        QCRuleViolation violation = violationService.createViolation(noViolation, testQCResult);

        assertNull("Should return null for non-violation", violation);
        verify(violationDAO, never()).insert(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_AlertFailure_ShouldStillCreateViolation() {
        doThrow(new RuntimeException("Alert error")).when(alertService).createAlertForViolation(any());

        QCRuleViolation violation = violationService.createViolation(testEvalResult, testQCResult);

        assertNotNull("Violation should still be created", violation);
        verify(violationDAO).insert(any(QCRuleViolation.class));
    }

    @Test
    public void testCreateViolation_WarningViolation_ShouldSetWarningSeverity() {
        RuleEvaluationResult warningResult = RuleEvaluationResult.violation("1₂ₛ", "WARNING", Arrays.asList("R1"),
                "Result exceeds 2SD");

        QCRuleViolation violation = violationService.createViolation(warningResult, testQCResult);

        assertEquals("WARNING", violation.getSeverity());
    }

    // ===================== getById tests =====================

    @Test
    public void testGetById_Found_ShouldReturnViolation() {
        QCRuleViolation expected = new QCRuleViolation();
        expected.setId("V1");
        when(violationDAO.get("V1")).thenReturn(Optional.of(expected));

        QCRuleViolation result = violationService.getById("V1");

        assertNotNull(result);
        assertEquals("V1", result.getId());
    }

    @Test
    public void testGetById_NotFound_ShouldReturnNull() {
        when(violationDAO.get("UNKNOWN")).thenReturn(Optional.empty());

        QCRuleViolation result = violationService.getById("UNKNOWN");

        assertNull(result);
    }

    // ===================== findByInstrument tests =====================

    @Test
    public void testFindByInstrument_ShouldReturnViolations() {
        QCRuleViolation v1 = new QCRuleViolation();
        v1.setId("V1");
        QCRuleViolation v2 = new QCRuleViolation();
        v2.setId("V2");

        when(violationDAO.findByInstrument(200)).thenReturn(Arrays.asList(v1, v2));

        List<QCRuleViolation> results = violationService.findByInstrument(200);

        assertEquals(2, results.size());
    }

    // ===================== findUnresolved tests =====================

    @Test
    public void testFindUnresolved_ShouldReturnUnresolvedViolations() {
        QCRuleViolation v1 = new QCRuleViolation();
        v1.setId("V1");
        v1.setResolutionStatus("UNRESOLVED");

        when(violationDAO.findUnresolved()).thenReturn(Arrays.asList(v1));

        List<QCRuleViolation> results = violationService.findUnresolved();

        assertEquals(1, results.size());
    }

    // ===================== resolveViolation tests =====================

    @Test
    public void testResolveViolation_ShouldUpdateStatus() {
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId("V1");
        violation.setResolutionStatus("UNRESOLVED");
        violation.setResolutionNotes("Detection: Result exceeds 3SD");

        when(violationDAO.get("V1")).thenReturn(Optional.of(violation));

        QCRuleViolation result = violationService.resolveViolation("V1", 1, "Recalibrated analyzer");

        assertEquals("RESOLVED", result.getResolutionStatus());
        assertEquals(Integer.valueOf(1), result.getResolvedByUserId());
        assertNotNull(result.getResolvedDateTime());
        assertTrue(result.getResolutionNotes().contains("Recalibrated analyzer"));
        assertTrue(result.getResolutionNotes().contains("Detection: Result exceeds 3SD"));

        verify(violationDAO).update(violation);
    }

    @Test
    public void testResolveViolation_NotFound_ShouldReturnNull() {
        when(violationDAO.get("UNKNOWN")).thenReturn(Optional.empty());

        QCRuleViolation result = violationService.resolveViolation("UNKNOWN", 1, "Notes");

        assertNull(result);
        verify(violationDAO, never()).update(any());
    }

    // ===================== acknowledgeViolation tests =====================

    @Test
    public void testAcknowledgeViolation_ShouldUpdateStatus() {
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId("V1");
        violation.setResolutionStatus("UNRESOLVED");

        when(violationDAO.get("V1")).thenReturn(Optional.of(violation));

        QCRuleViolation result = violationService.acknowledgeViolation("V1", 1);

        assertEquals("ACKNOWLEDGED", result.getResolutionStatus());
        assertEquals(Integer.valueOf(1), result.getResolvedByUserId());
        assertTrue(result.getResolutionNotes().contains("Acknowledged by user 1"));

        verify(violationDAO).update(violation);
    }

    @Test
    public void testAcknowledgeViolation_AlreadyResolved_ShouldNotChange() {
        QCRuleViolation violation = new QCRuleViolation();
        violation.setId("V1");
        violation.setResolutionStatus("RESOLVED");

        when(violationDAO.get("V1")).thenReturn(Optional.of(violation));

        QCRuleViolation result = violationService.acknowledgeViolation("V1", 1);

        assertEquals("RESOLVED", result.getResolutionStatus());
        verify(violationDAO, never()).update(any());
    }

    // ===================== getUnresolvedCountBySeverity tests
    // =====================

    @Test
    public void testGetUnresolvedCountBySeverity_ShouldReturnCount() {
        QCRuleViolation v1 = new QCRuleViolation();
        v1.setResolutionStatus("UNRESOLVED");
        QCRuleViolation v2 = new QCRuleViolation();
        v2.setResolutionStatus("UNRESOLVED");
        QCRuleViolation v3 = new QCRuleViolation();
        v3.setResolutionStatus("RESOLVED");

        when(violationDAO.findBySeverity("REJECTION")).thenReturn(Arrays.asList(v1, v2, v3));

        int count = violationService.getUnresolvedCountBySeverity("REJECTION");

        assertEquals(2, count);
    }

    @Test
    public void testGetUnresolvedCountBySeverity_NoViolations_ShouldReturnZero() {
        when(violationDAO.findBySeverity("REJECTION")).thenReturn(Collections.emptyList());

        int count = violationService.getUnresolvedCountBySeverity("REJECTION");

        assertEquals(0, count);
    }
}
