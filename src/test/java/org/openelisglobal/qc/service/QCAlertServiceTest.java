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
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Unit tests for QCAlertService (T100)
 *
 * Tests alert creation, batching logic, and alert management.
 */
@RunWith(MockitoJUnitRunner.class)
public class QCAlertServiceTest {

    @Mock
    private QCAlertDAO alertDAO;

    @Mock
    private SystemUserService systemUserService;

    @InjectMocks
    private QCAlertServiceImpl alertService;

    private QCRuleViolation rejectionViolation;
    private QCRuleViolation warningViolation;

    @Before
    public void setUp() {
        rejectionViolation = new QCRuleViolation();
        rejectionViolation.setId("V1");
        rejectionViolation.setRuleCode("1₃ₛ");
        rejectionViolation.setSeverity("REJECTION");
        rejectionViolation.setTestId("100");
        rejectionViolation.setInstrumentId("200");
        rejectionViolation.setViolationDateTime(Timestamp.from(Instant.now()));
        rejectionViolation.setTriggeringResultId("R1");

        warningViolation = new QCRuleViolation();
        warningViolation.setId("V2");
        warningViolation.setRuleCode("1₂ₛ");
        warningViolation.setSeverity("WARNING");
        warningViolation.setTestId("100");
        warningViolation.setInstrumentId("200");
        warningViolation.setViolationDateTime(Timestamp.from(Instant.now()));
        warningViolation.setTriggeringResultId("R2");
    }

    private SystemUser createActiveUser(String id) {
        SystemUser user = new SystemUser();
        user.setId(id);
        user.setIsActive("Y");
        return user;
    }

    private SystemUser createInactiveUser(String id) {
        SystemUser user = new SystemUser();
        user.setId(id);
        user.setIsActive("N");
        return user;
    }

    // ===================== createAlertsForViolation tests =====================

    @Test
    public void createAlertsForViolation_rejection_createsOneAlertPerActiveUser() {
        SystemUser user1 = createActiveUser("10");
        SystemUser user2 = createActiveUser("20");
        SystemUser inactive = createInactiveUser("30");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(user1, user2, inactive));

        List<QCAlert> alerts = alertService.createAlertsForViolation(rejectionViolation);

        assertEquals(2, alerts.size());
        assertEquals("V1", alerts.get(0).getViolationId());
        assertEquals("V1", alerts.get(1).getViolationId());
        assertEquals(Integer.valueOf(10), alerts.get(0).getRecipientUserId());
        assertEquals(Integer.valueOf(20), alerts.get(1).getRecipientUserId());
        assertFalse(alerts.get(0).getReadStatus());
        assertTrue(alerts.get(0).getMessageSubject().contains("URGENT"));
        verify(alertDAO, times(2)).insert(any(QCAlert.class));
    }

    @Test
    public void createAlertsForViolation_warning_createsAlertsWhenNotBatched() {
        when(alertDAO.findByViolation("V2")).thenReturn(Collections.emptyList());
        SystemUser user = createActiveUser("10");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(user));

        List<QCAlert> alerts = alertService.createAlertsForViolation(warningViolation);

        assertEquals(1, alerts.size());
        assertEquals("V2", alerts.get(0).getViolationId());
        assertFalse(alerts.get(0).getMessageSubject().contains("URGENT"));
        assertTrue(alerts.get(0).getMessageSubject().contains("Warning"));
        verify(alertDAO).insert(any(QCAlert.class));
    }

    @Test
    public void createAlertsForViolation_nullViolation_returnsEmptyList() {
        List<QCAlert> alerts = alertService.createAlertsForViolation(null);

        assertTrue(alerts.isEmpty());
        verify(alertDAO, never()).insert(any(QCAlert.class));
    }

    @Test
    public void createAlertsForViolation_rejectionNeverBatched() {
        SystemUser user = createActiveUser("10");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(user));

        List<QCAlert> alerts = alertService.createAlertsForViolation(rejectionViolation);

        assertFalse("REJECTION alerts should never be batched", alerts.isEmpty());
        verify(alertDAO).insert(any(QCAlert.class));
        verify(alertDAO, never()).findByViolation(anyString());
    }

    @Test
    public void createAlertsForViolation_warningBatchedIfRecentAlert() {
        QCAlert recentAlert = new QCAlert();
        recentAlert.setSentDateTime(Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
        when(alertDAO.findByViolation("V2")).thenReturn(Arrays.asList(recentAlert));

        List<QCAlert> alerts = alertService.createAlertsForViolation(warningViolation);

        assertTrue("Warning alert should be batched if recent alert exists", alerts.isEmpty());
        verify(alertDAO, never()).insert(any(QCAlert.class));
    }

    @Test
    public void createAlertsForViolation_warningNotBatchedIfOldAlert() {
        QCAlert oldAlert = new QCAlert();
        oldAlert.setSentDateTime(Timestamp.from(Instant.now().minus(20, ChronoUnit.MINUTES)));
        when(alertDAO.findByViolation("V2")).thenReturn(Arrays.asList(oldAlert));
        SystemUser user = createActiveUser("10");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(user));

        List<QCAlert> alerts = alertService.createAlertsForViolation(warningViolation);

        assertFalse("Warning alert should be created if no recent alerts", alerts.isEmpty());
        verify(alertDAO).insert(any(QCAlert.class));
    }

    @Test
    public void createAlertsForViolation_includesResolutionNotes() {
        rejectionViolation.setResolutionNotes("Detection: Result exceeds 3SD (z-score: 3.5)");
        SystemUser user = createActiveUser("10");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(user));

        List<QCAlert> alerts = alertService.createAlertsForViolation(rejectionViolation);

        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).getMessageBody().contains("Result exceeds 3SD"));
    }

    @Test
    public void createAlertsForViolation_noActiveUsers_returnsEmptyList() {
        SystemUser inactive = createInactiveUser("10");
        when(systemUserService.getAllSystemUsers()).thenReturn(Arrays.asList(inactive));

        List<QCAlert> alerts = alertService.createAlertsForViolation(rejectionViolation);

        assertTrue(alerts.isEmpty());
        verify(alertDAO, never()).insert(any(QCAlert.class));
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
        assertEquals("A1", alerts.get(0).getId());
        assertEquals("A2", alerts.get(1).getId());
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
        assertEquals("A1", alerts.get(0).getId());
        assertFalse("Alert should be unread", alerts.get(0).getReadStatus());
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