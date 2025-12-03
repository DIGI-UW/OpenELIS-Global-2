package org.openelisglobal.qc.service;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import org.openelisglobal.qc.dao.QCAlertDAO;
import org.openelisglobal.qc.valueholder.QCAlert;
import org.openelisglobal.qc.valueholder.QCRuleViolation;

/**
 * Unit tests for QCAlertService (T100)
 *
 * Tests alert creation, batching logic, and alert management.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCAlertServiceTest {

    @Mock
    private QCAlertDAO alertDAO;

    @InjectMocks
    private QCAlertServiceImpl alertService;

    private QCRuleViolation rejectionViolation;
    private QCRuleViolation warningViolation;

    @Before
    public void setUp() {
        // Create a REJECTION severity violation
        rejectionViolation = new QCRuleViolation();
        rejectionViolation.setId("V1");
        rejectionViolation.setRuleCode("1₃ₛ");
        rejectionViolation.setSeverity("REJECTION");
        rejectionViolation.setTestId(100);
        rejectionViolation.setInstrumentId(200);
        rejectionViolation.setViolationDateTime(Timestamp.from(Instant.now()));
        rejectionViolation.setTriggeringResultId("R1");

        // Create a WARNING severity violation
        warningViolation = new QCRuleViolation();
        warningViolation.setId("V2");
        warningViolation.setRuleCode("1₂ₛ");
        warningViolation.setSeverity("WARNING");
        warningViolation.setTestId(100);
        warningViolation.setInstrumentId(200);
        warningViolation.setViolationDateTime(Timestamp.from(Instant.now()));
        warningViolation.setTriggeringResultId("R2");
    }

    // ===================== createAlertForViolation tests =====================

    @Test
    public void testCreateAlertForViolation_Rejection_ShouldCreateImmediateAlert() {
        // REJECTION severity skips batching check entirely, no need to stub
        // findByViolation

        QCAlert alert = alertService.createAlertForViolation(rejectionViolation);

        assertNotNull("Alert should be created", alert);
        assertEquals("V1", alert.getViolationId());
        assertEquals("QC_RULE_VIOLATION", alert.getAlertType());
        assertFalse(alert.getReadStatus());
        assertTrue(alert.getMessageSubject().contains("URGENT"));
        assertTrue(alert.getMessageBody().contains("IMMEDIATE ACTION"));

        verify(alertDAO).insert(any(QCAlert.class));
    }

    @Test
    public void testCreateAlertForViolation_Warning_ShouldCreateAlert() {
        when(alertDAO.findByViolation("V2")).thenReturn(Collections.emptyList());

        QCAlert alert = alertService.createAlertForViolation(warningViolation);

        assertNotNull("Alert should be created", alert);
        assertEquals("V2", alert.getViolationId());
        assertFalse(alert.getMessageSubject().contains("URGENT"));
        assertTrue(alert.getMessageSubject().contains("Warning"));

        verify(alertDAO).insert(any(QCAlert.class));
    }

    @Test
    public void testCreateAlertForViolation_NullViolation_ShouldReturnNull() {
        QCAlert alert = alertService.createAlertForViolation(null);

        assertNull("Should return null for null violation", alert);
        verify(alertDAO, never()).insert(any(QCAlert.class));
    }

    @Test
    public void testCreateAlertForViolation_RejectionNeverBatched_EvenWithRecentAlerts() {
        // REJECTION severity skips batching check entirely - no need to set up recent
        // alerts
        // The service doesn't even call findByViolation for REJECTION

        QCAlert alert = alertService.createAlertForViolation(rejectionViolation);

        assertNotNull("REJECTION alerts should never be batched", alert);
        verify(alertDAO).insert(any(QCAlert.class));
        // Verify that findByViolation was never called for REJECTION
        verify(alertDAO, never()).findByViolation(anyString());
    }

    @Test
    public void testCreateAlertForViolation_Warning_ShouldBeBatchedIfRecentAlert() {
        // Recent alert within 15 minutes
        QCAlert recentAlert = new QCAlert();
        recentAlert.setSentDateTime(Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)));

        when(alertDAO.findByViolation("V2")).thenReturn(Arrays.asList(recentAlert));

        QCAlert alert = alertService.createAlertForViolation(warningViolation);

        assertNull("Warning alert should be batched if recent alert exists", alert);
        verify(alertDAO, never()).insert(any(QCAlert.class));
    }

    @Test
    public void testCreateAlertForViolation_Warning_ShouldNotBeBatchedIfOldAlert() {
        // Old alert outside 15 minute window
        QCAlert oldAlert = new QCAlert();
        oldAlert.setSentDateTime(Timestamp.from(Instant.now().minus(20, ChronoUnit.MINUTES)));

        when(alertDAO.findByViolation("V2")).thenReturn(Arrays.asList(oldAlert));

        QCAlert alert = alertService.createAlertForViolation(warningViolation);

        assertNotNull("Warning alert should be created if no recent alerts", alert);
        verify(alertDAO).insert(any(QCAlert.class));
    }

    @Test
    public void testCreateAlertForViolation_ShouldIncludeResolutionNotes() {
        rejectionViolation.setResolutionNotes("Detection: Result exceeds 3SD (z-score: 3.5)");
        // REJECTION severity skips batching check, no need to stub findByViolation

        QCAlert alert = alertService.createAlertForViolation(rejectionViolation);

        assertTrue(alert.getMessageBody().contains("Result exceeds 3SD"));
    }

    // ===================== getAlertsForUser tests =====================

    @Test
    public void testGetAlertsForUser_ShouldReturnUserAlerts() {
        QCAlert alert1 = new QCAlert();
        alert1.setId("A1");
        QCAlert alert2 = new QCAlert();
        alert2.setId("A2");

        when(alertDAO.findByRecipient(1)).thenReturn(Arrays.asList(alert1, alert2));

        List<QCAlert> alerts = alertService.getAlertsForUser(1);

        assertEquals(2, alerts.size());
    }

    // ===================== getUnreadAlertsForUser tests =====================

    @Test
    public void testGetUnreadAlertsForUser_ShouldReturnOnlyUnread() {
        QCAlert unreadAlert = new QCAlert();
        unreadAlert.setId("A1");
        unreadAlert.setReadStatus(false);

        when(alertDAO.findUnreadByRecipient(1)).thenReturn(Arrays.asList(unreadAlert));

        List<QCAlert> alerts = alertService.getUnreadAlertsForUser(1);

        assertEquals(1, alerts.size());
    }

    // ===================== markAsRead tests =====================

    @Test
    public void testMarkAsRead_ShouldUpdateReadStatus() {
        QCAlert alert = new QCAlert();
        alert.setId("A1");
        alert.setReadStatus(false);

        when(alertDAO.get("A1")).thenReturn(Optional.of(alert));

        QCAlert result = alertService.markAsRead("A1");

        assertTrue("Alert should be marked as read", result.getReadStatus());
        assertNotNull("Read time should be set", result.getReadDateTime());
        verify(alertDAO).update(alert);
    }

    @Test
    public void testMarkAsRead_AlreadyRead_ShouldNotUpdate() {
        QCAlert alert = new QCAlert();
        alert.setId("A1");
        alert.setReadStatus(true);
        alert.setReadDateTime(Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        when(alertDAO.get("A1")).thenReturn(Optional.of(alert));

        QCAlert result = alertService.markAsRead("A1");

        assertNotNull(result);
        verify(alertDAO, never()).update(any(QCAlert.class));
    }

    @Test
    public void testMarkAsRead_NotFound_ShouldReturnNull() {
        when(alertDAO.get("UNKNOWN")).thenReturn(Optional.empty());

        QCAlert result = alertService.markAsRead("UNKNOWN");

        assertNull("Should return null for unknown alert", result);
    }

    // ===================== markMultipleAsRead tests =====================

    @Test
    public void testMarkMultipleAsRead_ShouldMarkAll() {
        QCAlert alert1 = new QCAlert();
        alert1.setId("A1");
        alert1.setReadStatus(false);

        QCAlert alert2 = new QCAlert();
        alert2.setId("A2");
        alert2.setReadStatus(false);

        when(alertDAO.get("A1")).thenReturn(Optional.of(alert1));
        when(alertDAO.get("A2")).thenReturn(Optional.of(alert2));

        int count = alertService.markMultipleAsRead(Arrays.asList("A1", "A2"));

        assertEquals(2, count);
    }

    @Test
    public void testMarkMultipleAsRead_EmptyList_ShouldReturnZero() {
        int count = alertService.markMultipleAsRead(Collections.emptyList());

        assertEquals(0, count);
    }

    @Test
    public void testMarkMultipleAsRead_NullList_ShouldReturnZero() {
        int count = alertService.markMultipleAsRead(null);

        assertEquals(0, count);
    }

    // ===================== getUnreadAlertCount tests =====================

    @Test
    public void testGetUnreadAlertCount_ShouldReturnCount() {
        QCAlert alert1 = new QCAlert();
        QCAlert alert2 = new QCAlert();

        when(alertDAO.findUnreadByRecipient(1)).thenReturn(Arrays.asList(alert1, alert2));

        int count = alertService.getUnreadAlertCount(1);

        assertEquals(2, count);
    }

    @Test
    public void testGetUnreadAlertCount_NoAlerts_ShouldReturnZero() {
        when(alertDAO.findUnreadByRecipient(1)).thenReturn(Collections.emptyList());

        int count = alertService.getUnreadAlertCount(1);

        assertEquals(0, count);
    }

    // ===================== shouldBatchAlert tests =====================

    @Test
    public void testShouldBatchAlert_RejectionSeverity_ShouldNeverBatch() {
        // Even with recent alerts, REJECTION should not be batched
        assertFalse(alertService.shouldBatchAlert(rejectionViolation));
    }

    @Test
    public void testShouldBatchAlert_NullViolation_ShouldReturnFalse() {
        assertFalse(alertService.shouldBatchAlert(null));
    }

    @Test
    public void testShouldBatchAlert_Warning_NoRecentAlerts_ShouldNotBatch() {
        when(alertDAO.findByViolation("V2")).thenReturn(Collections.emptyList());

        assertFalse(alertService.shouldBatchAlert(warningViolation));
    }

    @Test
    public void testShouldBatchAlert_Warning_WithRecentAlert_ShouldBatch() {
        QCAlert recentAlert = new QCAlert();
        recentAlert.setSentDateTime(Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)));

        when(alertDAO.findByViolation("V2")).thenReturn(Arrays.asList(recentAlert));

        assertTrue(alertService.shouldBatchAlert(warningViolation));
    }
}
